package com.stc.radio.player.db;

import com.activeandroid.Configuration;
import com.activeandroid.content.ContentProvider;

public class DatabaseContentProvider extends ContentProvider {

    @Override
    protected Configuration getConfiguration() {
        Configuration.Builder builder = new Configuration.Builder(getContext());
        builder.addModelClass(DBMediaItem.class);
        builder.addModelClass(DBUserPrefsItem.class);
        return builder.create();
    }}