package pl.piomin.services.vertx.account.data;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class AccountRepositoryImpl implements AccountRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountRepositoryImpl.class);

    MongoClient client;

    public AccountRepositoryImpl(final MongoClient client) {
        this.client = client;
    }

    @Override
    public Future<Account> save(Account account) {
        JsonObject json = JsonObject.mapFrom(account);
        return client.save(Account.DB_TABLE, json).map(res -> {
            account.setId(res);
            return account;
        });
    }

    @Override
    public Future<List<Account>> findAll() {
        return client.find(Account.DB_TABLE, new JsonObject()).map(res -> {
            List<Account> accounts = res.stream().map(it ->
                    new Account(
                            it.getString("_id"),
                            it.getString("number"),
                            it.getInteger("balance"),
                            it.getString("customerId")))
                    .collect(Collectors.toList());
            return accounts;
        });
    }

    @Override
    public Future<Account> findById(String id) {
        return client.find(Account.DB_TABLE, new JsonObject().put("_id", id)).map(res -> {
            List<Account> accounts = res.stream().map(it ->
                            new Account(
                                    it.getString("_id"),
                                    it.getString("number"),
                                    it.getInteger("balance"),
                                    it.getString("customerId")))
                    .toList();
            return accounts.getFirst();
        });
    }

    @Override
    public Future<List<Account>> findByCustomer(String customerId) {
        return client.find(Account.DB_TABLE, new JsonObject().put("customerId", customerId)).map(res -> {
            List<Account> accounts = res.stream().map(it ->
                            new Account(
                                    it.getString("_id"),
                                    it.getString("number"),
                                    it.getInteger("balance"),
                                    it.getString("customerId")))
                    .toList();
            return accounts;
        });
    }

    @Override
    public Future<Boolean> remove(String id) {
        return client.removeDocument(Account.DB_TABLE, new JsonObject().put("_id", id)).map(res -> {
           return res.getRemovedCount() > 0;
        });
    }

}
