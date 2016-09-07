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
		Log.d(TAG, "intentAction=" + intentAction);
		if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			return;
		}
		KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		int keycode = event.getKeyCode();
		int action = event.getAction();
		Log.d(TAG, "KeyEvent action=" + intentAction);
		Log.d(TAG, "KeyEvent keycode=" + keycode);
		if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
				|| keycode == KeyEvent.KEYCODE_MEDIA_PLAY
				|| keycode == KeyEvent.KEYCODE_MEDIA_PAUSE
				|| keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
			if (action == KeyEvent.ACTION_DOWN) {
				btnControlClick(context, ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED);
			}
		} else if (keycode == KeyEvent.KEYCODE_MEDIA_NEXT) {
			if (action == KeyEvent.ACTION_DOWN) {
				btnControlClick(context, ServiceRadioRx.EXTRA_NEXT_PRESSED);
			}
		} else if (keycode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
			if (action == KeyEvent.ACTION_DOWN) {
				btnControlClick(context, ServiceRadioRx.EXTRA_PREV_PRESSED);
			}
		}
	}


	private void btnControlClick(Context context, String extra){
		Log.i(TAG, "KEYCODE_MEDIA: "+extra);
			Intent intent = new Intent(context, ServiceRadioRx.class);
			intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
			intent.putExtra(extra, extra);
			context.startService(intent);
	}
}