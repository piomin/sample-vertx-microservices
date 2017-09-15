package pl.piomin.services.vertx.account;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import pl.piomin.services.vertx.account.data.User;

@RunWith(VertxUnitRunner.class)
public class AccountServerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(AccountServerTest.class);

	Vertx vertx;

	@Before
	public void before(TestContext context) throws IOException {
	    vertx = Vertx.vertx();
	    vertx.deployVerticle(MongoVerticle.class.getName(), context.asyncAssertSuccess());
	    DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", 2222));
	    vertx.deployVerticle(AccountServer.class.getName(), options, context.asyncAssertSuccess());
	}

	@Test
	public void testAuth(TestContext context) {
		Async async = context.async();
		WebClient client = WebClient.create(vertx);
		User u = new User("piotr.minkowski", "Piot_123", "modify-account view-account");
		client.post(2222, "localhost", "/login").sendJson(u, ar -> {
			LOGGER.info("Response code: {}", ar.result().statusCode());
			LOGGER.info("Response: {}", ar.result().bodyAsString());
			if (ar.result().statusCode() == 200) {
				User user = ar.result().bodyAsJson(User.class);
				client.get(2222, "localhost", "/account").putHeader("Authorization", "Bearer " + user.getAccessToken()).send(r -> {
					LOGGER.info("GET result: {}", r.result().bodyAsString());
					async.complete();
				});
			} else {
				async.complete();
			}
		});
	}

}
