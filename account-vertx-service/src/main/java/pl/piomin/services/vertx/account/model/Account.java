package pl.piomin.services.vertx.account.model;

public class Account {

	private Integer id;
	private String number;
	private int balance;
	private Integer customerId;

	public Account() {
	
	}

	public Account(Integer id, String number, int balance, Integer customerId) {
		this.id = id;
		this.number = number;
		this.balance = balance;
		this.customerId = customerId;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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

}
