package pl.piomin.services.vertx.customer.data;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

@ProxyGen
public interface CustomerRepository {

    Future<Customer> save(Customer customer);
    Future<List<Customer>> findAll();
    Future<Customer> findById(String id);
    Future<List<Customer>> findByName(String name);
    Future<Boolean> remove(String id);

    static CustomerRepository createProxy(Vertx vertx, String address) {
        return new CustomerRepositoryVertxEBProxy(vertx, address);
    }

    static CustomerRepository create(MongoClient client) {
        return new CustomerRepositoryImpl(client);
    }
}
