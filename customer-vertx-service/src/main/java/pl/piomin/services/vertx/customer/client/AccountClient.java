package pl.piomin.services.vertx.customer.client;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClient;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import pl.piomin.services.vertx.customer.data.Account;

public class AccountClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountClient.class);

    private final ServiceDiscovery discovery;

    public AccountClient(ServiceDiscovery discovery) {
        this.discovery = discovery;
    }

    public Future<List<Account>> findCustomerAccounts(String customerId) {
        return discovery.getRecord(r -> r.getName().equals("account-service"))
            .compose(record -> {
                if (record == null) {
                    return Future.failedFuture("No account-service found");
                }
                ServiceReference ref = discovery.getReference(record);
                WebClient client = ref.getAs(WebClient.class);
                return client.get("/account/customer/" + customerId)
                    .send()
                    .onComplete(ar -> ref.release())
                    .compose(response -> {
                        if (response.statusCode() != 200) {
                            return Future.failedFuture("Failed to fetch accounts: " + response.statusMessage());
                        }
                        try {
                            List<Account> accounts = response.bodyAsJsonArray().stream()
                                .map(it -> Json.decodeValue(it.toString(), Account.class))
                                .collect(Collectors.toList());
                            return Future.succeededFuture(accounts);
                        } catch (Exception e) {
                            return Future.failedFuture("Failed to parse accounts: " + e.getMessage());
                        }
                    });
            });
    }
}
