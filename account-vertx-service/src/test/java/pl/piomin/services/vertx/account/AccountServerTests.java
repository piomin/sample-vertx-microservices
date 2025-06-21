package pl.piomin.services.vertx.account;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import pl.piomin.services.vertx.account.data.Account;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class AccountServerTests {

    final static Logger LOGGER = LoggerFactory.getLogger(AccountServerTests.class);
    final static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:5.0"));
    final static ConsulContainer consulContainer = new ConsulContainer("consul:1.14")
            .withConsulCommand("kv put config/account-service test=abc");

    static String id;

    @BeforeAll
    static void init(Vertx vertx) {
        mongoDBContainer.start();
        consulContainer.start();

        vertx.deployVerticle(new MongoVerticle(mongoDBContainer.getMappedPort(27017)));
        vertx.deployVerticle(new AccountServer());
    }

    @AfterAll
    static void destroy() {
        mongoDBContainer.stop();
        consulContainer.stop();
    }

    @Test
    @Order(2)
    void shouldFindAll(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.get(2222, "localhost", "/account")
                .send()
                .onSuccess(res -> {
                    LOGGER.info(res.bodyAsString());
                    assertNotNull(res.body());
                    assertEquals(!res.bodyAsJsonArray().isEmpty(), true, "Expected at least one account");
                    assertNotNull(res.bodyAsJsonArray().getJsonObject(0).getString("id"));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(1)
    void shouldAddNew(Vertx vertx, VertxTestContext testContext) {
        Account a = new Account();
        a.setBalance(20);
        a.setCustomerId("123");
        a.setNumber("1234567890");
        WebClient client = WebClient.create(vertx);
        client.post(2222, "localhost", "/account")
                .sendJson(a)
                .onSuccess(res -> {
                    LOGGER.info(res.bodyAsString());
                    assertNotNull(res.body());
                    assertNotNull(res.bodyAsJson(Account.class).getId());
                    id = res.bodyAsJson(Account.class).getId();
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(2)
    void shouldFindById(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.get(2222, "localhost", "/account/" + id)
                .send()
                .onSuccess(res -> {
                    LOGGER.info(res.bodyAsString());
                    assertNotNull(res.body());
                    assertNotNull(res.bodyAsJson(Account.class).getId());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }
}
