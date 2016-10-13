
package com.stc.radio.player.contentmodel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SimilarChannel {

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("similar_channel_id")
    @Expose
    private Integer similarChannelId;

    /**
     * 
     * @return
     *     The id
     */
    public Integer getId() {
        return id;
    }

    /**
     * 
     * @param id
     *     The id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 
     * @return
     *     The similarChannelId
     */
    public Integer getSimilarChannelId() {
        return similarChannelId;
    }

    /**
     * 
     * @param similarChannelId
     *     The similar_channel_id
     */
    public void setSimilarChannelId(Integer similarChannelId) {
        this.similarChannelId = similarChannelId;
    }

}
