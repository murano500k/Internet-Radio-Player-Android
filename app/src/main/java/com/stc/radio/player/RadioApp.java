package com.stc.radio.player;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Configuration;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;
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
		Configuration dbConfiguration = new Configuration.Builder(this)
				.setDatabaseName("radio.db")
				.setModelClasses(Station.class, NowPlaying.class).create();

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


/*
	private static class CrashReportingTree extends Timber.Tree {
		@Override protected void log(int priority, String tag, String message, Throwable t) {
			if (priority == Log.VERBOSE || priority == Log.DEBUG) {
				return;
			}

			FakeCrashLibrary.log(priority, tag, message);

			if (t != null) {
				if (priority == Log.ERROR) {
					FakeCrashLibrary.logError(t);
				} else if (priority == Log.WARN) {
					FakeCrashLibrary.logWarning(t);
				}
			}
		}

	}
*/
}
