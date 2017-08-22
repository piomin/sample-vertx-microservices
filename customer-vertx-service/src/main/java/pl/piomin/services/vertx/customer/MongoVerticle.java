package pl.piomin.services.vertx.customer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ProxyHelper;
import pl.piomin.services.vertx.customer.data.CustomerRepository;
import pl.piomin.services.vertx.customer.data.CustomerRepositoryImpl;

public class MongoVerticle extends AbstractVerticle {

	@Override
	public void start() throws Exception {
		JsonObject o = new JsonObject();
		o.put("host", "192.168.99.100");
		o.put("port", 27017);
		o.put("db_name", "test");
		final MongoClient client = MongoClient.createShared(vertx, o);
		final CustomerRepository service = new CustomerRepositoryImpl(client);
		ProxyHelper.registerService(CustomerRepository.class, vertx, service, "customer-service");
	}
	
}
