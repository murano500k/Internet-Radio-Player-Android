
package com.android.murano500k.newradio.rest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;




public class Channels {

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

    /**
     * No args constructor for use in serialization
     * 
     */
    public Channels() {
    }

    /**
     * 
     * @param cr
     * @param di
     * @param jr
     * @param rr
     * @param rt
     */
    public Channels(String di, String rt, String jr, String rr, String cr) {
        this.di = di;
        this.rt = rt;
        this.jr = jr;
        this.rr = rr;
        this.cr = cr;
    }

    /**
     * 
     * @return
     *     The di
     */
    public String getDi() {
        return di;
    }

    /**
     * 
     * @param di
     *     The di
     */
    public void setDi(String di) {
        this.di = di;
    }

    public Channels withDi(String di) {
        this.di = di;
        return this;
    }

    /**
     * 
     * @return
     *     The rt
     */
    public String getRt() {
        return rt;
    }

    /**
     * 
     * @param rt
     *     The rt
     */
    public void setRt(String rt) {
        this.rt = rt;
    }

    public Channels withRt(String rt) {
        this.rt = rt;
        return this;
    }

    /**
     * 
     * @return
     *     The jr
     */
    public String getJr() {
        return jr;
    }

    /**
     * 
     * @param jr
     *     The jr
     */
    public void setJr(String jr) {
        this.jr = jr;
    }

    public Channels withJr(String jr) {
        this.jr = jr;
        return this;
    }

    /**
     * 
     * @return
     *     The rr
     */
    public String getRr() {
        return rr;
    }

    /**
     * 
     * @param rr
     *     The rr
     */
    public void setRr(String rr) {
        this.rr = rr;
    }

    public Channels withRr(String rr) {
        this.rr = rr;
        return this;
    }

    /**
     * 
     * @return
     *     The cr
     */
    public String getCr() {
        return cr;
    }

    /**
     * 
     * @param cr
     *     The cr
     */
    public void setCr(String cr) {
        this.cr = cr;
    }

    public Channels withCr(String cr) {
        this.cr = cr;
        return this;
    }


}
