package pl.piomin.services.vertx.account.data;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class Account {

	private String id;
	private String number;
	private int balance;
	private Integer customerId;

	public Account() {
	
	}

	public Account(String id, String number, int balance, Integer customerId) {
		this.id = id;
		this.number = number;
		this.balance = balance;
		this.customerId = customerId;
	}
	
	public Account(JsonObject json) {
		this.id = json.getString("_id");
		this.number = json.getString("number");
		this.balance = json.getInteger("balance");
		this.customerId = json.getInteger("customerId");
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

	public Integer getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Integer customerId) {
		this.customerId = customerId;
	}

	public JsonObject toJson() {
		return JsonObject.mapFrom(this);
	}
	
}
