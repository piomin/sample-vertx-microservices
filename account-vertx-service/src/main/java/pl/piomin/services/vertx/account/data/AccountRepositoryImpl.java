package pl.piomin.services.vertx.account.data;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class AccountRepositoryImpl implements AccountRepository {

	MongoClient client;
	
	public AccountRepositoryImpl(final MongoClient client) {
		this.client = client;
	}
	
	@Override
	public AccountRepository save(Account account, Handler<AsyncResult<Account>> resultHandler) {
		JsonObject json = JsonObject.mapFrom(account);
		client.save("account", json, res -> {
			if (res.succeeded()) {
				account.setId(res.result());
				resultHandler.handle(Future.succeededFuture(account));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public AccountRepository findAll(Handler<AsyncResult<List<Account>>> resultHandler) {
		client.find("account", new JsonObject(), res -> {
			if (res.succeeded()) {
				List<Account> accounts = res.result().stream().map(it -> new Account(it.getString("_id"), it.getString("number"), it.getInteger("balance"), it.getInteger("customerId"))).collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(accounts));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public AccountRepository findById(String id, Handler<AsyncResult<Account>> resultHandler) {
		client.find("account", new JsonObject().put("_id", id), res -> {
			if (res.succeeded()) {
				List<Account> accounts = res.result().stream().map(it -> new Account(it.getString("_id"), it.getString("number"), it.getInteger("balance"), it.getInteger("customerId"))).collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(accounts.get(0)));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

}
