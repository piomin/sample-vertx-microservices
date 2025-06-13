package pl.piomin.services.vertx.customer;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.piomin.services.vertx.customer.data.Customer;
import pl.piomin.services.vertx.customer.data.CustomerRepository;


public class CustomerServer extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServer.class);

    private Integer port;

    public CustomerServer() {

    }

    public CustomerServer(Integer port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MongoVerticle());
        vertx.deployVerticle(new CustomerServer());
    }

    @Override
    public void start() throws Exception {
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);
        CustomerRepository repository = CustomerRepository.createProxy(vertx, "customer-service");

        Router router = Router.router(vertx);
        router.route("/customer/*").handler(ResponseContentTypeHandler.create());
        router.route(HttpMethod.POST, "/customer").handler(BodyHandler.create());
        router.get("/customer/:id").produces("application/json").handler(rc -> {
            rc.response().end(Json.encodePrettily(repository.findById(rc.request().getParam("id"))));
        });
        router.get("/customer/name/:name").produces("application/json").handler(rc -> {
            rc.response().end(Json.encodePrettily(repository.findByName(rc.request().getParam("name"))));
        });
        router.get("/customer").produces("application/json").handler(rc -> {
            rc.response().end(Json.encodePrettily(repository.findAll()));
        });
        router.post("/customer").produces("application/json").handler(rc -> {
            Customer c = rc.body().asPojo(Customer.class);
            rc.response().end(Json.encodePrettily(repository.save(c)));
        });
        router.delete("/customer/:id").handler(rc -> {
            repository.remove(rc.request().getParam("id"));
            rc.response().setStatusCode(200);
        });

        ConfigStoreOptions file = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "application.json"));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(file));
        retriever.getConfig().onComplete(conf -> {
            vertx.createHttpServer().requestHandler(router).listen(conf.result().getInteger("port"));
            JsonObject discoveryConfig = conf.result().getJsonObject("discovery");
            discovery.registerServiceImporter(new ConsulServiceImporter(),
                    new JsonObject()
                            .put("host", discoveryConfig.getString("host"))
                            .put("port", port == null ? discoveryConfig.getInteger("port") : port)
                            .put("scan-period", 2000));
        });

    }

}
