package pl.piomin.services.vertx.customer.data;

import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class CustomerRepositoryImpl implements CustomerRepository {

	@Override
	public CustomerRepository save(Account account, Handler<AsyncResult<Account>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CustomerRepository findAll(Handler<AsyncResult<List<Account>>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CustomerRepository findById(String id, Handler<AsyncResult<Account>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CustomerRepository findByCustomer(String customerId, Handler<AsyncResult<List<Account>>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CustomerRepository remove(String id, Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

}
