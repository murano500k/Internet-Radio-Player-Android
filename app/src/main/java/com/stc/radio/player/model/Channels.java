
package com.stc.radio.player.model;

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

}
