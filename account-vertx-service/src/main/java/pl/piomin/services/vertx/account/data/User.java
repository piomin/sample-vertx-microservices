package pl.piomin.services.vertx.account.data;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class User {

	private String accessToken;
	private String username;
	private String password;
	private String scope;

	public User() {

	}

	public User(String accessToken) {
		this.accessToken = accessToken;
	}

	public User(String username, String password, String scope) {
		this.username = username;
		this.password = password;
		this.scope = scope;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public JsonObject toJson() {
		return JsonObject.mapFrom(this);
	}
	
	@Override
	public String toString() {
		return Json.encodePrettily(this);
	}

}
