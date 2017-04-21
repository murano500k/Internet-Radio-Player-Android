
package com.stc.radio.player.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Images {

    @SerializedName("default")
    @Expose
    private String _default;

    /**
     * 
     * @return
     *     The _default
     */
    public String getDefault() {
        return _default;
    }

    /**
     * 
     * @param _default
     *     The default
     */
    public void setDefault(String _default) {
        this._default = _default;
    }

}
