package pl.piomin.services.vertx.account;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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
import pl.piomin.services.vertx.account.data.User;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the security-vertx-service.
 *
 * <p>The service exposes OAuth2/Keycloak-protected endpoints under {@code /account/*}.
 * Tests verify that unauthenticated requests are rejected (HTTP 401) and that the
 * {@code /login} endpoint behaves correctly when Keycloak is not available (HTTP 500).
 * Full end-to-end OAuth2 tests require a running Keycloak instance and are not included here.
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountServerTests {

    final static Logger LOGGER = LoggerFactory.getLogger(AccountServerTests.class);
    final static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:5.0"));
    final static ConsulContainer consulContainer = new ConsulContainer("consul:1.14")
            .withConsulCommand("kv put config/security-account-service test=abc");

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

    /**
     * Accessing a protected {@code /account/*} endpoint without a bearer token
     * should be rejected with HTTP 401 Unauthorized.
     */
    @Test
    @Order(1)
    void shouldRejectUnauthenticatedGetAll(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.get(2222, "localhost", "/account")
                .send()
                .onSuccess(res -> {
                    LOGGER.info("Response status (no auth): {}", res.statusCode());
                    assertEquals(401, res.statusCode(), "Expected 401 Unauthorized for unauthenticated request");
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    /**
     * Accessing a protected {@code /account/:id} endpoint without a bearer token
     * should be rejected with HTTP 401 Unauthorized.
     */
    @Test
    @Order(2)
    void shouldRejectUnauthenticatedGetById(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.get(2222, "localhost", "/account/some-id")
                .send()
                .onSuccess(res -> {
                    LOGGER.info("Response status (no auth, get by id): {}", res.statusCode());
                    assertEquals(401, res.statusCode(), "Expected 401 Unauthorized for unauthenticated request");
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    /**
     * Login with invalid credentials when Keycloak is not available should return HTTP 500.
     */
    @Test
    @Order(3)
    void shouldFailLoginWithoutKeycloak(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        User u = new User("testuser", "testpass", "view-account");
        client.post(2222, "localhost", "/login")
                .sendJson(u)
                .onSuccess(res -> {
                    LOGGER.info("Login response status (no Keycloak): {}", res.statusCode());
                    // Without a running Keycloak, the OAuth2 token request fails
                    assertEquals(500, res.statusCode(), "Expected 500 when Keycloak is unavailable");
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

}
