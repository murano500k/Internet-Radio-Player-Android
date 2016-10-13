
package com.stc.radio.player.contentmodel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class PlaylistContent {

    @SerializedName("ad_channels")
    @Expose
    private String adChannels;
    @SerializedName("channel_director")
    @Expose
    private String channelDirector;
    @SerializedName("created_at")
    @Expose
    private String createdAt;
    @SerializedName("description_long")
    @Expose
    private String descriptionLong;
    @SerializedName("description_short")
    @Expose
    private String descriptionShort;
    @SerializedName("forum_id")
    @Expose
    private Object forumId;
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("key")
    @Expose
    private String key;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("network_id")
    @Expose
    private Integer networkId;
    @SerializedName("old_id")
    @Expose
    private Integer oldId;
    @SerializedName("premium_id")
    @Expose
    private Object premiumId;
    @SerializedName("tracklist_server_id")
    @Expose
    private Object tracklistServerId;
    @SerializedName("updated_at")
    @Expose
    private String updatedAt;
    @SerializedName("asset_id")
    @Expose
    private Integer assetId;
    @SerializedName("asset_url")
    @Expose
    private String assetUrl;
    @SerializedName("banner_url")
    @Expose
    private Object bannerUrl;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("similar_channels")
    @Expose
    private List<SimilarChannel> similarChannels = new ArrayList<SimilarChannel>();
    @SerializedName("images")
    @Expose
    private Images images;

    /**
     * 
     * @return
     *     The adChannels
     */
    public String getAdChannels() {
        return adChannels;
    }

    /**
     * 
     * @param adChannels
     *     The ad_channels
     */
    public void setAdChannels(String adChannels) {
        this.adChannels = adChannels;
    }

    /**
     * 
     * @return
     *     The channelDirector
     */
    public String getChannelDirector() {
        return channelDirector;
    }

    /**
     * 
     * @param channelDirector
     *     The channel_director
     */
    public void setChannelDirector(String channelDirector) {
        this.channelDirector = channelDirector;
    }

    /**
     * 
     * @return
     *     The createdAt
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 
     * @param createdAt
     *     The created_at
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 
     * @return
     *     The descriptionLong
     */
    public String getDescriptionLong() {
        return descriptionLong;
    }

    /**
     * 
     * @param descriptionLong
     *     The description_long
     */
    public void setDescriptionLong(String descriptionLong) {
        this.descriptionLong = descriptionLong;
    }

    /**
     * 
     * @return
     *     The descriptionShort
     */
    public String getDescriptionShort() {
        return descriptionShort;
    }

    /**
     * 
     * @param descriptionShort
     *     The description_short
     */
    public void setDescriptionShort(String descriptionShort) {
        this.descriptionShort = descriptionShort;
    }

    /**
     * 
     * @return
     *     The forumId
     */
    public Object getForumId() {
        return forumId;
    }

    /**
     * 
     * @param forumId
     *     The forum_id
     */
    public void setForumId(Object forumId) {
        this.forumId = forumId;
    }

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
     *     The key
     */
    public String getKey() {
        return key;
    }

    /**
     * 
     * @param key
     *     The key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 
     * @return
     *     The name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name
     *     The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @return
     *     The networkId
     */
    public Integer getNetworkId() {
        return networkId;
    }

    /**
     * 
     * @param networkId
     *     The network_id
     */
    public void setNetworkId(Integer networkId) {
        this.networkId = networkId;
    }

    /**
     * 
     * @return
     *     The oldId
     */
    public Integer getOldId() {
        return oldId;
    }

    /**
     * 
     * @param oldId
     *     The old_id
     */
    public void setOldId(Integer oldId) {
        this.oldId = oldId;
    }

    /**
     * 
     * @return
     *     The premiumId
     */
    public Object getPremiumId() {
        return premiumId;
    }

    /**
     * 
     * @param premiumId
     *     The premium_id
     */
    public void setPremiumId(Object premiumId) {
        this.premiumId = premiumId;
    }

    /**
     * 
     * @return
     *     The tracklistServerId
     */
    public Object getTracklistServerId() {
        return tracklistServerId;
    }

    /**
     * 
     * @param tracklistServerId
     *     The tracklist_server_id
     */
    public void setTracklistServerId(Object tracklistServerId) {
        this.tracklistServerId = tracklistServerId;
    }

    /**
     * 
     * @return
     *     The updatedAt
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 
     * @param updatedAt
     *     The updated_at
     */
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 
     * @return
     *     The assetId
     */
    public Integer getAssetId() {
        return assetId;
    }

    /**
     * 
     * @param assetId
     *     The asset_id
     */
    public void setAssetId(Integer assetId) {
        this.assetId = assetId;
    }

    /**
     * 
     * @return
     *     The assetUrl
     */
    public String getAssetUrl() {
        return assetUrl;
    }

    /**
     * 
     * @param assetUrl
     *     The asset_url
     */
    public void setAssetUrl(String assetUrl) {
        this.assetUrl = assetUrl;
    }

    /**
     * 
     * @return
     *     The bannerUrl
     */
    public Object getBannerUrl() {
        return bannerUrl;
    }

    /**
     * 
     * @param bannerUrl
     *     The banner_url
     */
    public void setBannerUrl(Object bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    /**
     * 
     * @return
     *     The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * 
     * @param description
     *     The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @return
     *     The similarChannels
     */
    public List<SimilarChannel> getSimilarChannels() {
        return similarChannels;
    }

    /**
     * 
     * @param similarChannels
     *     The similar_channels
     */
    public void setSimilarChannels(List<SimilarChannel> similarChannels) {
        this.similarChannels = similarChannels;
    }

    /**
     * 
     * @return
     *     The images
     */
    public Images getImages() {
        return images;
    }

    /**
     * 
     * @param images
     *     The images
     */
    public void setImages(Images images) {
        this.images = images;
    }

}
