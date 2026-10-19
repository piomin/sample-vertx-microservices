package pl.piomin.services.vertx.account;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import pl.piomin.services.vertx.account.data.Account;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountServerTests {

    final static Logger LOGGER = LoggerFactory.getLogger(AccountServerTests.class);
    final static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:5.0"));
    final static ConsulContainer consulContainer = new ConsulContainer("consul:1.14")
            .withConsulCommand("kv put config/account-service test=abc");

    static String id;
    static Vertx vertx;

    @BeforeAll
    static void init() {
        mongoDBContainer.start();
        consulContainer.start();

        MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
                .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
                .setEnabled(true);
        vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(metricsOptions));

        vertx.deployVerticle(new MongoVerticle(mongoDBContainer.getMappedPort(27017)));
        vertx.deployVerticle(new AccountServer());
    }

    @AfterAll
    static void destroy() {
        if (vertx != null) {
            vertx.close();
        }
        mongoDBContainer.stop();
        consulContainer.stop();
    }

    @Test
    @Order(3)
    void shouldFindAll(VertxTestContext testContext) {
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
    void shouldAddNew(VertxTestContext testContext) {
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
    void shouldFindById(VertxTestContext testContext) {
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

    @Test
    @Order(4)
    void shouldDeleteById(VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.delete(2222, "localhost", "/account/" + id)
                .send()
                .onSuccess(res -> {
                    LOGGER.info("Delete status: {}", res.statusCode());
                    assertEquals(200, res.statusCode());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(5)
    void shouldExposeMetricsEndpoint(VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.get(2222, "localhost", "/metrics")
                .send()
                .onSuccess(res -> {
                    LOGGER.info("Metrics status: {}", res.statusCode());
                    assertEquals(200, res.statusCode(), "Expected 200 from /metrics endpoint");
                    String contentType = res.getHeader("Content-Type");
                    assertNotNull(contentType, "Expected Content-Type header on metrics response");
                    assertTrue(contentType.startsWith("text/plain"), "Expected text/plain Content-Type for Prometheus metrics");
                    String body = res.bodyAsString();
                    assertNotNull(body, "Expected non-empty metrics body");
                    assertTrue(body.contains("jvm_") || body.contains("vertx_") || body.contains("process_"),
                            "Expected Prometheus metrics in body (jvm_, vertx_, or process_ prefix)");
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }
}
