package pl.piomin.services.vertx.account;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        Router router = Router.router(vertx);
        router.route("/account/*").handler(ResponseContentTypeHandler.create());
        router.route(HttpMethod.POST, "/account").handler(BodyHandler.create());
        router.get("/account/:id").produces("application/json").handler(rc -> {
            rc.response().end(Json.encodePrettily(repository.findById(rc.request().getParam("id"))));
        });
        router.get("/account/customer/:customer").produces("application/json").handler(rc -> {
            rc.response().end(Json.encodePrettily(repository.findByCustomer(rc.request().getParam("customer"))));
        });
        router.get("/account").produces("application/json").handler(rc -> {
            rc.response().end(Json.encodePrettily(repository.findAll()));
        });
        router.post("/account").produces("application/json").handler(rc -> {
            Account a = rc.body().asPojo(Account.class);
            rc.response().end(Json.encodePrettily(repository.save(a)));
        });
        router.delete("/account/:id").handler(rc -> {
            repository.remove(rc.request().getParam("id"));
            rc.response().setStatusCode(200);
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
                        LOGGER.info("Consul registration status: {}", res.result().statusCode());
                    });
        });

    }

}
