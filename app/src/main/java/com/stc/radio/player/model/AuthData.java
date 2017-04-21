
package com.stc.radio.player.model;

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

}
