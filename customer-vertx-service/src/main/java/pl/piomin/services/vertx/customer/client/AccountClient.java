package pl.piomin.services.vertx.customer.client;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClient;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import pl.piomin.services.vertx.customer.data.Account;

public class AccountClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(AccountClient.class);
	
	private ServiceDiscovery discovery;

	public AccountClient(ServiceDiscovery discovery) {
		this.discovery = discovery;
	}
	
	public AccountClient findCustomerAccounts(String customerId, Handler<AsyncResult<List<Account>>> resultHandler) {
		discovery.getRecord(r -> r.getName().equals("account-service"), res -> {
			LOGGER.info("Result: {}", res.result().getType());
			ServiceReference ref = discovery.getReference(res.result());
			WebClient client = ref.getAs(WebClient.class);
			client.get("/account/customer/" + customerId).send(res2 -> {
				LOGGER.info("Response: {}", res2.result().bodyAsString());
				List<Account> accounts = res2.result().bodyAsJsonArray().stream().map(it -> Json.decodeValue(it.toString(), Account.class)).collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(accounts));
			});
		});
		return this;
	}
}
