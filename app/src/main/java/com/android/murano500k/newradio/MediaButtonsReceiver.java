package com.android.murano500k.newradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonsReceiver extends BroadcastReceiver {

	public static final String TAG = "MediaButtonsReceiver";

	public MediaButtonsReceiver() {
		super();
	}
/*mMediaButtonReceiverComponent = new ComponentName(this.getPackageName(), HeadsetButtonsReceiver.class.getName());
	    mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);*/

	@Override
	public void onReceive(Context context, Intent intent) {

			String intentAction = intent.getAction();
		Log.d(TAG, "intentAction="+intentAction);
		if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			return;
		}

			KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			int keycode = event.getKeyCode();
			int action = event.getAction();
		Log.d(TAG, "KeyEvent action="+intentAction);
		Log.d(TAG, "KeyEvent keycode="+keycode);

			if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
				if (action == KeyEvent.ACTION_DOWN) {
					PlaylistManager.addToLog("KEYCODE_MEDIA_PLAY_PAUSE");
					Intent playPauseIntent = new Intent(context, ServiceRadio.class);
					playPauseIntent.setAction(Constants.INTENT.PLAYBACK.PLAY_PAUSE);
					context.startService(playPauseIntent);
				}
			}

			if (keycode == KeyEvent.KEYCODE_MEDIA_NEXT) {
				if (action == KeyEvent.ACTION_DOWN) {
					PlaylistManager.addToLog("KEYCODE_MEDIA_NEXT");
					Intent playPauseIntent = new Intent(context, ServiceRadio.class);
					playPauseIntent.setAction(Constants.INTENT.PLAYBACK.PLAY_NEXT);
					context.startService(playPauseIntent);
				}
			}

			if (keycode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
				if (action == KeyEvent.ACTION_DOWN) {
					PlaylistManager.addToLog("KEYCODE_MEDIA_PREVIOUS");
					Intent playPauseIntent = new Intent(context, ServiceRadio.class);
					playPauseIntent.setAction(Constants.INTENT.PLAYBACK.PLAY_PREV);
					context.startService(playPauseIntent);
				}
			}

		}
}