package pl.piomin.services.vertx.account;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ServiceBinder;
import pl.piomin.services.vertx.account.data.AccountRepository;
import pl.piomin.services.vertx.account.data.AccountRepositoryImpl;

public class MongoVerticle extends AbstractVerticle {

    Integer port;

    public MongoVerticle() {

    }

    public MongoVerticle(Integer port) {
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        ConfigStoreOptions file = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "application.json"));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(file));
        retriever.getConfig().onComplete(conf -> {
            JsonObject datasourceConfig = conf.result().getJsonObject("datasource");
            JsonObject o = new JsonObject();
            o.put("host", datasourceConfig.getString("host"));
            if (this.port == null) {
                o.put("port", datasourceConfig.getInteger("port"));
            } else {
                o.put("port", port);
            }
            o.put("db_name", datasourceConfig.getString("db_name"));
            final MongoClient client = MongoClient.createShared(vertx, o);
            final AccountRepository service = new AccountRepositoryImpl(client);
            
            // Register the service proxy
            new ServiceBinder(vertx)
                .setAddress("account-service")
                .register(AccountRepository.class, service);
        });
    }

}
