package pl.piomin.services.vertx.customer.data;

import java.util.List;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

public interface CustomerRepository {

	@Fluent
	CustomerRepository save(Account account, Handler<AsyncResult<Account>> resultHandler);

	@Fluent
	CustomerRepository findAll(Handler<AsyncResult<List<Account>>> resultHandler);

	@Fluent
	CustomerRepository findById(String id, Handler<AsyncResult<Account>> resultHandler);
	
	@Fluent
	CustomerRepository findByCustomer(String customerId, Handler<AsyncResult<List<Account>>> resultHandler);
	
	@Fluent
	CustomerRepository remove(String id, Handler<AsyncResult<Void>> resultHandler);

	static CustomerRepository createProxy(Vertx vertx, String address) {
		return new CustomerRepositoryVertxEBProxy(vertx, address);
	}
	
	static CustomerRepository create(MongoClient client) {
		return new CustomerRepositoryImpl(client);
	}
	
}
