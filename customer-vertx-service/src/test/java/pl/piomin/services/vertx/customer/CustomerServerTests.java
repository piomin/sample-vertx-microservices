package pl.piomin.services.vertx.customer;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import pl.piomin.services.vertx.customer.data.Customer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerServerTests {

    final static Logger LOGGER = LoggerFactory.getLogger(CustomerServerTests.class);
    final static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:5.0"));
    final static ConsulContainer consulContainer = new ConsulContainer("consul:1.14")
            .withConsulCommand("kv put config/customer-service test=abc");

    static String id;

    @BeforeAll
    static void init(Vertx vertx) {
        mongoDBContainer.start();
        consulContainer.start();

        vertx.deployVerticle(new MongoVerticle(mongoDBContainer.getMappedPort(27017)));
        vertx.deployVerticle(new CustomerServer(consulContainer.getMappedPort(8500)));
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
        client.get(3333, "localhost", "/customer")
                .send()
                .onSuccess(res -> {
                    LOGGER.info(res.bodyAsString());
                    assertNotNull(res.body());
                    assertNotNull(res.bodyAsJsonArray().getJsonObject(0).getString("id"));
                    testContext.completeNow();
                });
    }

    @Test
    @Order(1)
    void shouldAddNew(Vertx vertx, VertxTestContext testContext) {
        Customer c = new Customer();
        c.setAge(20);
        c.setName("Test");
        WebClient client = WebClient.create(vertx);
        client.post(3333, "localhost", "/customer")
                .sendJson(c)
                .onSuccess(res -> {
                    LOGGER.info(res.bodyAsString());
                    assertNotNull(res.body());
                    assertNotNull(res.bodyAsJson(Customer.class).getId());
                    id = res.bodyAsJson(Customer.class).getId();
                    testContext.completeNow();
                });
    }

    @Test
    @Order(2)
    void shouldFindByName(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.get(3333, "localhost", "/customer/name/Test")
                .send()
                .onSuccess(res -> {
                    LOGGER.info(res.bodyAsString());
                    assertNotNull(res.body());
                    assertNotNull(res.bodyAsJsonArray().getJsonObject(0).getString("id"));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(2)
    void shouldFindById(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.get(3333, "localhost", "/customer/" + id)
                .send()
                .onSuccess(res -> {
                    LOGGER.info(res.bodyAsString());
                    assertNotNull(res.body());
                    assertNotNull(res.bodyAsJson(Customer.class).getId());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

}
