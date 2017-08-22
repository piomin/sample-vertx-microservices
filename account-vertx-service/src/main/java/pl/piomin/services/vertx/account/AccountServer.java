package pl.piomin.services.vertx.account;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
import io.vertx.servicediscovery.impl.ServiceTypes;
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

		WebClient client = WebClient.create(vertx);
		JsonObject json = new JsonObject().put("ID", "account-service-1").put("Name", "account-service").put("Address", "127.0.0.1").put("Port", 8080).put("Tags", new JsonArray().add("http-endpoint"));
		client.put(8500, "192.168.99.100", "/v1/agent/service/register").sendJsonObject(json, res -> {
			LOGGER.info("Consul registration status: {}", res.result());
		});
//		JsonObject c = new JsonObject().put("host", "192.168.99.100").put("port", 8500).put("scan-period", 2000);
//		ServiceDiscovery discovery = ServiceDiscovery.create(vertx).registerServiceImporter(new ConsulServiceImporter(), c, res -> {
//			LOGGER.info("Discovery ready: {}", res.succeeded());
//		});
		
	    
		AccountRepository repository = AccountRepository.createProxy(vertx, "account-service");
		  
		Router router = Router.router(vertx);
		router.route("/account/*").handler(ResponseContentTypeHandler.create());
//		router.route("/account/*").handler(LoggerHandler.create());
		router.route(HttpMethod.POST, "/account").handler(BodyHandler.create());
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
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
		
		ServiceDiscovery discovery = ServiceDiscovery.create(vertx);
		discovery.registerServiceImporter(new ConsulServiceImporter(), new JsonObject().put("host", "192.168.99.100").put("port", 8500).put("scan-period", 2000), res -> {
			discovery.getRecord(r -> r.getName().equals("account-service"), res1 -> {
				LOGGER.info("Result: {}", res1.result().getType());
				ServiceReference ref = discovery.getReference(res1.result());
				WebClient client2 = ref.getAs(WebClient.class);
				client2.get("/accounts").send(res2 -> {
					LOGGER.info("Response: {}", res2.result().bodyAsString());
				});
			});
		});

//		Record r = HttpEndpoint.createRecord("account-service", "localhost", 8080, "/");
//		discovery.publish(r, res -> {
//			LOGGER.info("Registered: {}, {}", res.result().getRegistration(), res.result().getStatus());
//			discovery.getRecords(new JsonObject().put("name", "account-service"), ar -> {
//				LOGGER.info("Found: {}", ar.result());
//			});
//		});
		
	}

}
