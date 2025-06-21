package pl.piomin.services.vertx.account.data;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

@ProxyGen
public interface AccountRepository {

//    @Fluent
    Future<Account> save(Account account);

//    @Fluent
    Future<List<Account>> findAll();

//    @Fluent
    Future<Account> findById(String id);

//    @Fluent
    Future<List<Account>> findByCustomer(String customerId);

//    @Fluent
    Future<Boolean> remove(String id);

    static AccountRepository createProxy(Vertx vertx, String address) {
        return new AccountRepositoryVertxEBProxy(vertx, address);
    }

    static AccountRepository create(MongoClient client) {
        return new AccountRepositoryImpl(client);
    }

}
