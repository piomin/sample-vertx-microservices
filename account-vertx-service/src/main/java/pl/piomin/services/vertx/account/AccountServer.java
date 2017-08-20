package pl.piomin.services.vertx.account;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import pl.piomin.services.vertx.account.model.Account;


public class AccountServer extends AbstractVerticle {

	public static void main(String[] args) throws Exception {
		AccountServer server = new AccountServer();
		Vertx.vertx().deployVerticle(server);
	}
	
	@Override
	public void start() throws Exception {
		Router router = Router.router(vertx);
		router.route("/account/*").handler(ResponseContentTypeHandler.create());
		router.route("/account/*").handler(LoggerHandler.create());
		router.route(HttpMethod.POST, "/account").handler(BodyHandler.create());
		router.get("/account/:id").produces("application/json").handler(rc -> {
			rc.response().end(Json.encodePrettily(new Account(Integer.valueOf(rc.request().getParam("id")), "1234567", 1000, 2)));
		});
		router.get("/account").produces("application/json").handler(rc -> {
			List<Account> accounts = new ArrayList<>();
			accounts.add(new Account(1, "1234567", 1000, 2));
			accounts.add(new Account(2, "1234568", 1300, 3));
			accounts.add(new Account(3, "1234569", 700, 4));
			rc.response().end(Json.encodePrettily(accounts));
		});
		router.post("/account").produces("application/json").handler(rc -> {
			Account a = Json.decodeValue(rc.getBodyAsString(), Account.class);
			a.setId(1);
			rc.response().end(Json.encodePrettily(a));
		});
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	}

}
