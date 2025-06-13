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
    public Future<Customer> save(Customer customer) {
        JsonObject json = JsonObject.mapFrom(customer);
        return client.save(Customer.DB_TABLE, json).map(res -> {
            customer.setId(res);
            return customer;
        });
    }

    @Override
    public Future<List<Customer>> findAll() {
        return client.find(Customer.DB_TABLE, new JsonObject()).map(res -> {
            List<Customer> customers = res.stream().map(it ->
                            new Customer(
                                    it.getString("_id"),
                                    it.getString("name"),
                                    it.getInteger("age")))
                    .toList();
            return customers;
        });
    }

    @Override
    public Future<Customer> findById(String id) {
        return client.find(Customer.DB_TABLE, new JsonObject().put("_id", id)).map(res -> {
            List<Customer> accounts = res.stream().map(it ->
                            new Account(
                                    it.getString("_id"),
                                    it.getString("name"),
                                    it.getInteger("age")))
                    .toList();
            return accounts.getFirst();
        });
    }

    @Override
    public Future<List<Customer>> findByName(String name) {
        return client.find(Customer.DB_TABLE, new JsonObject().put("name", name)).map(res -> {
            List<Customer> customers = res.stream().map(it ->
                            new Customer(
                                    it.getString("_id"),
                                    it.getString("name"),
                                    it.getInteger("age")))
                    .toList();
            return customers;
        });
    }

    @Override
    public Future<Boolean> remove(String id) {
        return client.removeDocument(Customer.DB_TABLE, new JsonObject().put("_id", id)).map(res -> {
            return res.getRemovedCount() > 0;
        });
    }

}
