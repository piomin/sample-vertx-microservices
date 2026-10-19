package pl.piomin.services.vertx.customer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.consul.ConsulContainer;
import pl.piomin.services.vertx.customer.client.AccountClient;
import pl.piomin.services.vertx.customer.data.Account;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link AccountClient} inter-service communication.
 *
 * <p>Sets up a Testcontainers Consul instance and an embedded Vert.x HTTP server
 * that stubs the account-service endpoints. The {@link AccountClient} is wired with a
 * {@link ServiceDiscovery} that imports the stub service from Consul, allowing end-to-end
 * testing of service discovery and HTTP communication without starting the real account service.
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountClientTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountClientTests.class);

    /** Port on which the stub account HTTP server listens. */
    private static final int STUB_PORT = 8888;

    /** Customer ID used across ordered tests. */
    private static final String CUSTOMER_ID = "customer-123";

    static final ConsulContainer consulContainer =
            new ConsulContainer("consul:1.14")
                    .withConsulCommand("kv put config/account-service test=abc");

    static AccountClient accountClient;

    /** Stub account data returned by the embedded HTTP server. */
    static final JsonArray customerAccounts = new JsonArray()
            .add(new JsonObject()
                    .put("id", "acc-1")
                    .put("number", "ACC-001")
                    .put("balance", 1000)
                    .put("customerId", CUSTOMER_ID));

    @BeforeAll
    static void init(Vertx vertx, VertxTestContext testContext) {
        consulContainer.start();
        int consulPort = consulContainer.getMappedPort(8500);

        // 1. Start an embedded stub HTTP server that mimics the account-service REST API.
        startStubAccountServer(vertx)
            .compose(httpServer -> {
                LOGGER.info("Stub account server started on port {}", STUB_PORT);

                // 2. Register the stub as "account-service" in Consul.
                JsonObject serviceRegistration = new JsonObject()
                        .put("ID", "account-service-1")
                        .put("Name", "account-service")
                        .put("Address", "127.0.0.1")
                        .put("Port", STUB_PORT)
                        .put("Tags", new JsonArray().add("http-endpoint"));

                WebClient registrationClient = WebClient.create(vertx);
                return registrationClient
                        .put(consulPort, "localhost", "/v1/agent/service/register")
                        .sendJsonObject(serviceRegistration)
                        .map(res -> {
                            LOGGER.info("Consul registration HTTP status: {}", res.statusCode());
                            return res;
                        });
            })
            .compose(res -> {
                // 3. Create ServiceDiscovery with ConsulServiceImporter and wire AccountClient.
                ServiceDiscovery discovery = ServiceDiscovery.create(vertx);
                discovery.registerServiceImporter(
                        new ConsulServiceImporter(),
                        new JsonObject()
                                .put("host", "localhost")
                                .put("port", consulPort)
                                .put("scan-period", 2000));
                accountClient = new AccountClient(discovery);

                // 4. Wait for the first Consul poll to import the account-service record.
                return Future.future(promise -> vertx.setTimer(3000, t -> promise.complete()));
            })
            .onSuccess(v -> testContext.completeNow())
            .onFailure(testContext::failNow);
    }

    /**
     * Starts an embedded Vert.x HTTP server that stubs two account-service endpoints:
     * <ul>
     *   <li>{@code GET /account/customer/:customerId} — returns canned accounts for CUSTOMER_ID,
     *       or an empty array for any other customer ID.</li>
     * </ul>
     */
    private static Future<HttpServer> startStubAccountServer(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/account/customer/:customerId").handler(rc -> {
            String customerId = rc.pathParam("customerId");
            if (CUSTOMER_ID.equals(customerId)) {
                rc.response()
                        .putHeader("Content-Type", "application/json")
                        .end(customerAccounts.encodePrettily());
            } else {
                rc.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonArray().encodePrettily());
            }
        });

        return vertx.createHttpServer()
                .requestHandler(router)
                .listen(STUB_PORT);
    }

    @AfterAll
    static void destroy() {
        consulContainer.stop();
    }

    @Test
    @Order(1)
    void shouldFindCustomerAccounts(Vertx vertx, VertxTestContext testContext) {
        accountClient.findCustomerAccounts(CUSTOMER_ID)
                .onSuccess(accounts -> {
                    LOGGER.info("Accounts for customer {}: {}", CUSTOMER_ID, accounts);
                    assertNotNull(accounts);
                    assertFalse(accounts.isEmpty(), "Expected at least one account for customer");
                    Account account = accounts.getFirst();
                    assertEquals(CUSTOMER_ID, account.getCustomerId());
                    assertEquals("ACC-001", account.getNumber());
                    assertEquals(1000, account.getBalance());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(2)
    void shouldReturnEmptyListForNonexistentCustomer(Vertx vertx, VertxTestContext testContext) {
        accountClient.findCustomerAccounts("nonexistent-customer")
                .onSuccess(accounts -> {
                    LOGGER.info("Accounts for nonexistent customer: {}", accounts);
                    assertNotNull(accounts);
                    assertTrue(accounts.isEmpty(), "Expected empty list for nonexistent customer");
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }
}
