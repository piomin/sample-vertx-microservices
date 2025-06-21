package pl.piomin.services.vertx.account.data;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

@ProxyGen
public interface AccountRepository {

    Future<Account> save(Account account);
    Future<List<Account>> findAll();
    Future<Account> findById(String id);
    Future<List<Account>> findByCustomer(String customerId);
    Future<Boolean> remove(String id);

    static AccountRepository createProxy(Vertx vertx, String address) {
        return new AccountRepositoryVertxEBProxy(vertx, address);
    }

    static AccountRepository create(MongoClient client) {
        return new AccountRepositoryImpl(client);
    }

}
