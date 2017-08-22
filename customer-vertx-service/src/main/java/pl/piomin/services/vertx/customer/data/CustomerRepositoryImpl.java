package pl.piomin.services.vertx.customer.data;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class CustomerRepositoryImpl implements CustomerRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerRepositoryImpl.class);
	
	MongoClient client;
	
	public CustomerRepositoryImpl(final MongoClient client) {
		this.client = client;
	}
	
	@Override
	public CustomerRepository save(Customer customer, Handler<AsyncResult<Customer>> resultHandler) {
		JsonObject json = JsonObject.mapFrom(customer);
		client.save(Customer.DB_TABLE, json, res -> {
			if (res.succeeded()) {
				LOGGER.info("Customer created: {}", res.result());
				customer.setId(res.result());
				resultHandler.handle(Future.succeededFuture(customer));
			} else {
				LOGGER.error("Customer not created", res.cause());
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public CustomerRepository findAll(Handler<AsyncResult<List<Customer>>> resultHandler) {
		client.find(Customer.DB_TABLE, new JsonObject(), res -> {
			if (res.succeeded()) {
				List<Customer> customers = res.result().stream().map(it -> new Customer(it.getString("_id"), it.getString("name"), it.getInteger("age"))).collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(customers));
			} else {
				LOGGER.error("Customer not found", res.cause());
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public CustomerRepository findById(String id, Handler<AsyncResult<Customer>> resultHandler) {
		client.find(Customer.DB_TABLE, new JsonObject().put("_id", id), res -> {
			if (res.succeeded()) {
				List<Customer> customers = res.result().stream().map(it -> new Customer(it.getString("_id"), it.getString("name"), it.getInteger("age"))).collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(customers.get(0)));
			} else {
				LOGGER.error("Account not found", res.cause());
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public CustomerRepository findByName(String name, Handler<AsyncResult<List<Customer>>> resultHandler) {
		client.find(Customer.DB_TABLE, new JsonObject().put("name", name), res -> {
			if (res.succeeded()) {
				List<Customer> customers = res.result().stream().map(it -> new Customer(it.getString("_id"), it.getString("name"), it.getInteger("age"))).collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(customers));
			} else {
				LOGGER.error("Account not found", res.cause());
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public CustomerRepository remove(String id, Handler<AsyncResult<Void>> resultHandler) {
		client.removeDocument(Customer.DB_TABLE, new JsonObject().put("_id", id), res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.future());
			} else {
				LOGGER.error("Customer not found", res.cause());
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

}
