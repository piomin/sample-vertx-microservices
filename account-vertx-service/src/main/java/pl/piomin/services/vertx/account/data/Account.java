package pl.piomin.services.vertx.account.data;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

@DataObject
public class Account {

    public static final String DB_TABLE = "account";

    private String id;
    private String number;
    private int balance;
    private String customerId;

    public Account() {

    }

    public Account(String id, String number, int balance, String customerId) {
        this.id = id;
        this.number = number;
        this.balance = balance;
        this.customerId = customerId;
    }

    public Account(JsonObject json) {
        this.id = json.getString("id");
        this.number = json.getString("number");
        this.balance = json.getInteger("balance");
        this.customerId = json.getString("customerId");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
        return Json.encodePrettily(this);
    }

}
