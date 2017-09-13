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
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.KeycloakHelper;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.impl.OAuth2API;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
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
		
		JsonObject keycloakJson = new JsonObject()
			    .put("realm", "master")
			    .put("realm-public-key", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1xVBifXfS1uVM8S14JlyLpXck+0+hBQX258IiL5Fm2rZpkQ5lN9N1tadQdXBKk8V/0SxdTyoX7cpYQkcOs0Rj0XXmX7Lnk56euZwel+3MKAZWA20ld8BCfmDtX4/+VP311USUqR/W8Fd2p/gugKWF6VDMkri92qob1DdrcUiRlD8XYC0pwHwSvyW/3JvE5HeTy3U4vxC+19wHcwzLGNlVOlYPk9mzJHXN+LhZr/Tc7HeAsvVxYDXwOOh+/UWweMkvKy+OSNKG3aWLb92Ni3HejFn9kd4TRHfaapwWg1m5Duf3uqz8WDHbS/LeS4g3gQS0SvcCYI0huSoG3NA/z4K7wIDAQAB")
			    .put("auth-server-url", "http://192.168.99.100:38080/auth")
			    .put("ssl-required", "external")
			    .put("resource", "vertx")
			    .put("credentials", new JsonObject().put("secret", "73b55e04-e562-41ea-b39c-263b7b36945d"));
		
		OAuth2Auth oauth2 = KeycloakAuth.create(vertx, OAuth2FlowType.PASSWORD, keycloakJson);
		OAuth2AuthHandler oauth2Handler = (OAuth2AuthHandler) OAuth2AuthHandler.create(oauth2, "http://localhost:2222");
//		oauth2Handler.addAuthority("manage-account");
		JsonObject tokenConfig = new JsonObject().put("username", "piotr.minkowski").put("password", "Piot_123").put("scope", "modify");
		oauth2.getToken(tokenConfig, res -> {
			if (res.failed()) {
				LOGGER.error("Access token error: {}", res.cause().getMessage());
			} else {
				AccessToken token = res.result();				
				LOGGER.info("Access Token: {}", KeycloakHelper.rawAccessToken(token.principal()));
				token.isAuthorised("realm:modify", rc -> {
					LOGGER.info("Access Token: ok={}, result={}", rc.succeeded(), rc.result());
					if (rc.result()) {
						LOGGER.info("Access Token: {}", rc.result());
					}
				});
			}
		});
		
		Router router = Router.router(vertx);
		router.route("/account/*").handler(ResponseContentTypeHandler.create());
		router.route("/account/*").handler(oauth2Handler);
		router.route(HttpMethod.POST, "/account").handler(BodyHandler.create());
		oauth2Handler.setupCallback(router.get("/callback"));
		router.get("/account/:id").produces("application/json").handler(rc -> {
			repository.findById(rc.request().getParam("id"), res -> {
				Account account = res.result();
				LOGGER.info("Found: {}", account);
				rc.response().end(account.toString());
			});
		});
		router.get("/account/customer/:customer").produces("application/json").handler(rc -> {
			repository.findByCustomer(rc.request().getParam("customer"), res -> {
				List<Account> accounts = res.result();
				LOGGER.info("Found: {}", accounts);
				rc.response().end(Json.encodePrettily(accounts));
			});
		});
		router.get("/account").produces("application/json").handler(rc -> {
			repository.findAll(res -> {
				List<Account> accounts = res.result();
				LOGGER.info("Found all: {}", accounts);
				rc.response().end(Json.encodePrettily(accounts));
			});
		});
		router.post("/account").produces("application/json").handler(rc -> {
			Account a = Json.decodeValue(rc.getBodyAsString(), Account.class);
			repository.save(a, res -> {
				Account account = res.result();
				LOGGER.info("Created: {}", account);
				rc.response().end(account.toString());
			});
		});
		router.delete("/account/:id").handler(rc -> {
			repository.remove(rc.request().getParam("id"), res -> {
				LOGGER.info("Removed: {}", rc.request().getParam("id"));
				rc.response().setStatusCode(200);
			});
		});
		
		

		
		WebClient client = WebClient.create(vertx);
		ConfigStoreOptions file = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "application.json"));
		ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(file));
		retriever.getConfig(conf -> {
			JsonObject discoveryConfig = conf.result().getJsonObject("discovery");
			vertx.createHttpServer().requestHandler(router::accept).listen(conf.result().getInteger("port"));	
			JsonObject json = new JsonObject().put("ID", "account-service-1").put("Name", "account-service").put("Address", "127.0.0.1").put("Port", 2222).put("Tags", new JsonArray().add("http-endpoint"));
			client.put(discoveryConfig.getInteger("port"), discoveryConfig.getString("host"), "/v1/agent/service/register").sendJsonObject(json, res -> {
				LOGGER.info("Consul registration status: {}", res.result().statusCode());
			});
		});
		
	}

}
