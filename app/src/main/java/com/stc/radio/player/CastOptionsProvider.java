package com.stc.radio.player;

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import java.util.ArrayList;
import java.util.List;

public class CastOptionsProvider implements OptionsProvider {
    public static final String CUSTOM_NAMESPACE = "urn:x-cast:custom_radio_namespace";

    @Override
    public CastOptions getCastOptions(Context context) {


        List<String> supportedNamespaces = new ArrayList<>();
        supportedNamespaces.add(CUSTOM_NAMESPACE);
        CastOptions castOptions = new CastOptions.Builder()
                .setReceiverApplicationId(context.getString(R.string.app_id))
                .setSupportedNamespaces(supportedNamespaces)
                .build();
        return castOptions;
    }
    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}