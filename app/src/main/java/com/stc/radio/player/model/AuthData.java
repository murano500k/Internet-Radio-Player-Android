package com.stc.radio.player.model;

/**
 * Created by artem on 10/12/16.
 */

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AuthData {

	@SerializedName("token")
	@Expose
	private String token;
	@SerializedName("time")
	@Expose
	private String time;
	@SerializedName("timeExp")
	@Expose
	private String timeExp;
	@SerializedName("email")
	@Expose
	private String email;
	@SerializedName("password")
	@Expose
	private String password;
	@SerializedName("lastupd")
	@Expose
	private String lastupd;
	@SerializedName("channels")
	@Expose
	private Channels channels;

	/**
	 *
	 * @return
	 * The token
	 */
	public String getToken() {
		return token;
	}

	/**
	 *
	 * @param token
	 * The token
	 */
	public void setToken(String token) {
		this.token = token;
	}

	public AuthData withToken(String token) {
		this.token = token;
		return this;
	}

	/**
	 *
	 * @return
	 * The time
	 */
	public String getTime() {
		return time;
	}

	/**
	 *
	 * @param time
	 * The time
	 */
	public void setTime(String time) {
		this.time = time;
	}

	public AuthData withTime(String time) {
		this.time = time;
		return this;
	}

	/**
	 *
	 * @return
	 * The timeExp
	 */
	public String getTimeExp() {
		return timeExp;
	}

	/**
	 *
	 * @param timeExp
	 * The timeExp
	 */
	public void setTimeExp(String timeExp) {
		this.timeExp = timeExp;
	}

	public AuthData withTimeExp(String timeExp) {
		this.timeExp = timeExp;
		return this;
	}

	/**
	 *
	 * @return
	 * The email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 *
	 * @param email
	 * The email
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	public AuthData withEmail(String email) {
		this.email = email;
		return this;
	}

	/**
	 *
	 * @return
	 * The password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 *
	 * @param password
	 * The password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	public AuthData withPassword(String password) {
		this.password = password;
		return this;
	}

	/**
	 *
	 * @return
	 * The lastupd
	 */
	public String getLastupd() {
		return lastupd;
	}

	/**
	 *
	 * @param lastupd
	 * The lastupd
	 */
	public void setLastupd(String lastupd) {
		this.lastupd = lastupd;
	}

	public AuthData withLastupd(String lastupd) {
		this.lastupd = lastupd;
		return this;
	}

	/**
	 *
	 * @return
	 * The channels
	 */
	public Channels getChannels() {
		return channels;
	}

	/**
	 *
	 * @param channels
	 * The channels
	 */
	public void setChannels(Channels channels) {
		this.channels = channels;
	}

	public AuthData withChannels(Channels channels) {
		this.channels = channels;
		return this;
	}

}

class Channels {

	@SerializedName("di")
	@Expose
	private String di;
	@SerializedName("rt")
	@Expose
	private String rt;
	@SerializedName("jr")
	@Expose
	private String jr;
	@SerializedName("rr")
	@Expose
	private String rr;
	@SerializedName("cr")
	@Expose
	private String cr;

}
