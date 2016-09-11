
package com.android.murano500k.newradio.rest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponceRegister {

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
     * No args constructor for use in serialization
     * 
     */
    public ResponceRegister() {
    }

    /**
     * 
     * @param time
     * @param email
     * @param token
     * @param channels
     * @param lastupd
     * @param timeExp
     * @param password
     */
    public ResponceRegister(String token, String time, String timeExp, String email, String password, String lastupd, Channels channels) {
        this.token = token;
        this.time = time;
        this.timeExp = timeExp;
        this.email = email;
        this.password = password;
        this.lastupd = lastupd;
        this.channels = channels;
    }

    /**
     * 
     * @return
     *     The token
     */
    public String getToken() {
        return token;
    }

    /**
     * 
     * @param token
     *     The token
     */
    public void setToken(String token) {
        this.token = token;
    }

    public ResponceRegister withToken(String token) {
        this.token = token;
        return this;
    }

    /**
     * 
     * @return
     *     The time
     */
    public String getTime() {
        return time;
    }

    /**
     * 
     * @param time
     *     The time
     */
    public void setTime(String time) {
        this.time = time;
    }

    public ResponceRegister withTime(String time) {
        this.time = time;
        return this;
    }

    /**
     * 
     * @return
     *     The timeExp
     */
    public String getTimeExp() {
        return timeExp;
    }

    /**
     * 
     * @param timeExp
     *     The timeExp
     */
    public void setTimeExp(String timeExp) {
        this.timeExp = timeExp;
    }

    public ResponceRegister withTimeExp(String timeExp) {
        this.timeExp = timeExp;
        return this;
    }

    /**
     * 
     * @return
     *     The email
     */
    public String getEmail() {
        return email;
    }

    /**
     * 
     * @param email
     *     The email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    public ResponceRegister withEmail(String email) {
        this.email = email;
        return this;
    }

    /**
     * 
     * @return
     *     The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * 
     * @param password
     *     The password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public ResponceRegister withPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * 
     * @return
     *     The lastupd
     */
    public String getLastupd() {
        return lastupd;
    }

    /**
     * 
     * @param lastupd
     *     The lastupd
     */
    public void setLastupd(String lastupd) {
        this.lastupd = lastupd;
    }

    public ResponceRegister withLastupd(String lastupd) {
        this.lastupd = lastupd;
        return this;
    }

    /**
     * 
     * @return
     *     The channels
     */
    public Channels getChannels() {
        return channels;
    }

    /**
     * 
     * @param channels
     *     The channels
     */
    public void setChannels(Channels channels) {
        this.channels = channels;
    }

    public ResponceRegister withChannels(Channels channels) {
        this.channels = channels;
        return this;
    }


}
