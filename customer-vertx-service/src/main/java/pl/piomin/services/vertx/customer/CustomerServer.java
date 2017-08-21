package pl.piomin.services.vertx.customer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import pl.piomin.services.vertx.customer.data.Account;
import pl.piomin.services.vertx.customer.data.AccountRepository;


public class CustomerServer extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServer.class);
	
	public static void main(String[] args) throws Exception {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new MongoVerticle());
		vertx.deployVerticle(new CustomerServer());
	}
	
	@Override
	public void start() throws Exception {

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
		
	}

}
