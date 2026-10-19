package pl.piomin.services.vertx.account;

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
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import pl.piomin.services.vertx.account.data.Account;
import pl.piomin.services.vertx.account.data.AccountRepository;
import pl.piomin.services.vertx.account.data.User;


public class AccountServer extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountServer.class);

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MongoVerticle());
        vertx.deployVerticle(new AccountServer());
    }

    @Override
    public void start() throws Exception {
        AccountRepository repository = AccountRepository.createProxy(vertx, "security-account-service");

        JsonObject keycloakJson = new JsonObject()
            .put("realm", "master")
            .put("realm-public-key", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1xVBifXfS1uVM8S14JlyLpXck+0+hBQX258IiL5Fm2rZpkQ5lN9N1tadQdXBKk8V/0SxdTyoX7cpYQkcOs0Rj0XXmX7Lnk56euZwel+3MKAZWA20ld8BCfmDtX4/+VP311USUqR/W8Fd2p/gugKWF6VDMkri92qob1DdrcUiRlD8XYC0pwHwSvyW/3JvE5HeTy3U4vxC+19wHcwzLGNlVOlYPk9mzJHXN+LhZr/Tc7HeAsvVxYDXwOOh+/UWweMkvKy+OSNKG3aWLb92Ni3HejFn9kd4TRHfaapwWg1m5Duf3uqz8WDHbS/LeS4g3gQS0SvcCYI0huSoG3NA/z4K7wIDAQAB")
            .put("auth-server-url", "http://localhost:8080/auth")
            .put("ssl-required", "external")
            .put("resource", "vertx-account")
            .put("credentials", new JsonObject().put("secret", "73b55e04-e562-41ea-b39c-263b7b36945d"));

        OAuth2Auth oauth2 = KeycloakAuth.create(vertx, OAuth2FlowType.PASSWORD, keycloakJson);
        OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler.create(vertx, oauth2, "http://localhost:2222/callback");

        Router router = Router.router(vertx);
        router.route("/account/*").handler(ResponseContentTypeHandler.create());
        router.route("/account/*").handler(oauth2Handler);
        router.route(HttpMethod.POST, "/account").handler(BodyHandler.create());
        router.route(HttpMethod.POST, "/login").handler(BodyHandler.create());
        oauth2Handler.setupCallback(router.get("/callback"));

        router.post("/login").produces("application/json").handler(rc -> {
            User u = rc.body().asPojo(User.class);
            Oauth2Credentials credentials = new Oauth2Credentials()
                .setFlow(OAuth2FlowType.PASSWORD)
                .setUsername(u.getUsername())
                .setPassword(u.getPassword());
            oauth2.authenticate(credentials).onComplete(res -> {
                if (res.failed()) {
                    LOGGER.error("Access token error: {}", res.cause().getMessage());
                    rc.response().setStatusCode(500).end();
                } else {
                    io.vertx.ext.auth.User authUser = res.result();
                    String rawToken = authUser.principal().getString("access_token");
                    LOGGER.info("Access Token: {}", rawToken);
                    User user = new User(rawToken);
                    rc.response().end(user.toString());
                }
            });
        });

        router.get("/account/:id").produces("application/json").handler(rc -> {
            repository.findById(rc.request().getParam("id")).onComplete(res -> {
                if (res.succeeded()) {
                    Account account = res.result();
                    if (account == null) {
                        rc.response().setStatusCode(404).end();
                    } else {
                        LOGGER.info("Found: {}", account);
                        rc.response().end(account.toString());
                    }
                } else {
                    rc.response().setStatusCode(500).end();
                }
            });
        });

        router.get("/account/customer/:customer").produces("application/json").handler(rc -> {
            repository.findByCustomer(rc.request().getParam("customer")).onComplete(res -> {
                if (res.succeeded()) {
                    LOGGER.info("Found: {}", res.result());
                    rc.response().end(Json.encodePrettily(res.result()));
                } else {
                    rc.response().setStatusCode(500).end();
                }
            });
        });

        router.get("/account").produces("application/json").handler(rc -> {
            repository.findAll().onComplete(res -> {
                if (res.succeeded()) {
                    LOGGER.info("Found all: {}", res.result());
                    rc.response().end(Json.encodePrettily(res.result()));
                } else {
                    rc.response().setStatusCode(500).end();
                }
            });
        });

        router.post("/account").produces("application/json").handler(rc -> {
            Account a = rc.body().asPojo(Account.class);
            repository.save(a).onComplete(res -> {
                if (res.succeeded()) {
                    Account account = res.result();
                    LOGGER.info("Created: {}", account);
                    rc.response().end(account.toString());
                } else {
                    rc.response().setStatusCode(500).end();
                }
            });
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
                .put("ID", "security-account-service-1")
                .put("Name", "security-account-service")
                .put("Address", "127.0.0.1")
                .put("Port", 2222)
                .put("Tags", new JsonArray().add("http-endpoint"));
            client.put(discoveryConfig.getInteger("port"), discoveryConfig.getString("host"), "/v1/agent/service/register")
                .sendJsonObject(json).onComplete(res -> {
                    LOGGER.info("Consul registration status: {}", res.result() != null ? res.result().statusCode() : "failed");
                });
        });
    }

}
