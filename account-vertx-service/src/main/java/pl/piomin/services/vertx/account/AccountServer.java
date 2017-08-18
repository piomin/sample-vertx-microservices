package pl.piomin.services.vertx.account;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
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
		router.get("/account/:id").produces("application/json").handler(rc -> {
			  rc.response().end(Json.encodePrettily(new Account(Integer.valueOf(rc.request().getParam("id")), "1234567", 1000, 2)));
		});
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	}

}
