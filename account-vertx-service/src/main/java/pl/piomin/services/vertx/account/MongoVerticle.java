package pl.piomin.services.vertx.account;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ProxyHelper;
import pl.piomin.services.vertx.account.data.AccountRepository;
import pl.piomin.services.vertx.account.data.AccountRepositoryImpl;

public class MongoVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        ConfigStoreOptions file = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "application.json"));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(file));
        retriever.getConfig(conf -> {
            JsonObject datasourceConfig = conf.result().getJsonObject("datasource");
            JsonObject o = new JsonObject();
            o.put("host", datasourceConfig.getString("host"));
            o.put("port", datasourceConfig.getInteger("port"));
            o.put("db_name", datasourceConfig.getString("db_name"));
            final MongoClient client = MongoClient.createShared(vertx, o);
            final AccountRepository service = new AccountRepositoryImpl(client);
            ProxyHelper.registerService(AccountRepository.class, vertx, service, "account-service");
        });
    }

}
