package com.stc.radio.player.utils;

import android.content.Context;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

/**
 * Created by artem on 10/5/16.
 */

public final class PabloPicasso {
	private static Picasso instance;
	private static OkHttpDownloader downloader;

	public static Picasso with(Context context) {
		if (instance == null) {
			//if (downloader == null) downloader =  new OkHttpDownloader(context.getApplicationContext(), 2400000);
			instance = new Picasso.Builder(context.getApplicationContext())
					.memoryCache(new LruCache(240000))
					/*.downloader(downloader)*/
					.build();
		}
		return instance;
	}

	private PabloPicasso() {
		throw new AssertionError("No instances.");
	}
}