package com.stc.radio.player;

import com.activeandroid.ActiveAndroid;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.stc.radio.player.utils.SettingsProvider;

import java.util.UUID;

import timber.log.Timber;


public class RadioApp extends com.activeandroid.app.Application {
	protected String userAgent;
	private FirebaseAnalytics mFirebaseAnalytics;

	@Override
	public void onTerminate() {
		super.onTerminate();
	}
	@Override
	public void onCreate() {
		super.onCreate();
		userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		mFirebaseAnalytics.setUserId(UUID.randomUUID().toString());
		ActiveAndroid.initialize(this);
		SettingsProvider.init(getApplicationContext());

		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree() {
				@Override
				protected String createStackElementTag(StackTraceElement element) {
					return super.createStackElementTag(element)
							+ '.' + element.getMethodName()
							+ ':' + element.getLineNumber();
				}
			});
		}
	}

		public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
			return new DefaultDataSourceFactory(this, bandwidthMeter,
					buildHttpDataSourceFactory(bandwidthMeter));
		}

		public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
			return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
		}

	public boolean useExtensionRenderers() {
		return BuildConfig.FLAVOR.equals("withExtensions");
	}


}


