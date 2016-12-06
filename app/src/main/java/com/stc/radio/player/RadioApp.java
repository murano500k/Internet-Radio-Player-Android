package com.stc.radio.player;

import com.activeandroid.ActiveAndroid;
import com.stc.radio.player.utils.SettingsProvider;

import timber.log.Timber;


public class RadioApp extends com.activeandroid.app.Application {

	@Override
	public void onTerminate() {
		super.onTerminate();
	}
	@Override
	public void onCreate() {
		super.onCreate();

		ActiveAndroid.initialize(this);
		SettingsProvider.init(getApplicationContext());

		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree(){
				@Override
				protected String createStackElementTag(StackTraceElement element) {
					return super.createStackElementTag(element)
							+'.'+element.getMethodName()
							+':'+element.getLineNumber();
				}
			});
		} else {
			//Timber.plant(new CrashReportingTree());
		}
	}

}
