package pl.piomin.services.vertx.customer.data;

import java.util.List;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

@DataObject
public class Customer {

    public static final String DB_TABLE = "customer";

    private String id;
    private String name;
    private int age;
    private List<Account> accounts;

    public Customer() {

    }

    public Customer(String id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public Customer(JsonObject json) {
        this.id = json.getString("id");
        this.name = json.getString("name");
        this.age = json.getInteger("age");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
        return Json.encodePrettily(this);
    }

}
