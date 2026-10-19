package pl.piomin.services.vertx.account;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.piomin.services.vertx.account.data.Account;
import pl.piomin.services.vertx.account.data.AccountRepository;


public class AccountServer extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountServer.class);

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MongoVerticle());
        vertx.deployVerticle(new AccountServer());
    }

    @Override
    public void start() throws Exception {
        AccountRepository repository = AccountRepository.createProxy(vertx, "account-service");

        SchemaRepository schemaRepository = SchemaRepository.create(
                new JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri("https://vertx.io"));

        ValidationHandler accountValidationHandler = ValidationHandlerBuilder
                .create(schemaRepository)
                .body(Bodies.json(Schemas.objectSchema()
                        .requiredProperty("number", Schemas.stringSchema().with(Keywords.minLength(1)))
                        .requiredProperty("customerId", Schemas.stringSchema().with(Keywords.minLength(1)))
                        .requiredProperty("balance", Schemas.intSchema().with(Keywords.minimum(0)))))
                .build();

        Router router = Router.router(vertx);
        router.route("/account/*").handler(ResponseContentTypeHandler.create());
        router.route(HttpMethod.POST, "/account").handler(BodyHandler.create());
        router.get("/account/:id").produces("application/json").handler(rc -> {
            repository.findById(rc.request().getParam("id")).onComplete(res -> {
                if (res.succeeded()) {
                    Account a = res.result();
                    if (a == null) {
                        rc.response().setStatusCode(404).end();
                    } else {
                        rc.response().end(Json.encodePrettily(a));
                    }
                } else {
                    rc.response().setStatusCode(500).end();
                }
            });
        });
        router.get("/account/customer/:customer").produces("application/json").handler(rc -> {
            repository.findByCustomer(rc.request().getParam("customer")).onComplete(res -> {
                if (res.succeeded()) {
                    rc.response().end(Json.encodePrettily(res.result()));
                } else {
                    rc.response().setStatusCode(500).end();
                }
            });
        });
        router.get("/account").produces("application/json").handler(rc -> {
            repository.findAll().onComplete(res -> {
                if (res.succeeded()) {
                    rc.response().end(Json.encodePrettily(res.result()));
                } else {
                    rc.response().setStatusCode(500).end();
                }
            });
        });
        router.post("/account")
                .handler(accountValidationHandler)
                .produces("application/json")
                .handler(rc -> {
                    Account a = rc.body().asPojo(Account.class);
                    repository.save(a).onComplete(res -> {
                        if (res.succeeded()) {
                            rc.response().end(Json.encodePrettily(res.result()));
                        } else {
                            rc.response().setStatusCode(500).end();
                        }
                    });
                })
                .failureHandler(rc -> {
                    if (rc.failure() instanceof BadRequestException) {
                        rc.response()
                                .setStatusCode(400)
                                .putHeader("Content-Type", "application/json")
                                .end(new JsonObject().put("error", rc.failure().getMessage()).encode());
                    } else {
                        rc.response().setStatusCode(500).end();
                    }
                });
        router.delete("/account/:id").handler(rc -> {
            repository.remove(rc.request().getParam("id")).onComplete(res -> {
                if (res.succeeded()) {
                    rc.response().setStatusCode(res.result() ? 200 : 404).end();
                } else {
                    rc.response().setStatusCode(500).end();
                }
            });
        });

        WebClient client = WebClient.create(vertx);
        ConfigStoreOptions file = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "application.json"));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(file));
        retriever.getConfig().onComplete(conf -> {
            JsonObject discoveryConfig = conf.result().getJsonObject("discovery");
            vertx.createHttpServer().requestHandler(router).listen(conf.result().getInteger("port"));
            JsonObject json = new JsonObject()
                    .put("ID", "account-service-1")
                    .put("Name", "account-service")
                    .put("Address", "127.0.0.1")
                    .put("Port", 2222)
                    .put("Tags", new JsonArray().add("http-endpoint"));
            client.put(discoveryConfig.getInteger("port"), discoveryConfig.getString("host"), "/v1/agent/service/register")
                    .sendJsonObject(json).onComplete(   res -> {
//                        LOGGER.info("Consul registration status: {}", res.result().statusCode());
                    });
        });

    }

}
