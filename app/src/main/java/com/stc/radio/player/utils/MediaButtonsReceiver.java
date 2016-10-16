package com.stc.radio.player.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import com.stc.radio.player.ServiceRadioRx;

import timber.log.Timber;

import static com.stc.radio.player.ServiceRadioRx.EXTRA_WHICH;

public class MediaButtonsReceiver extends BroadcastReceiver {

	public static final String TAG = "MediaButtonsReceiver";

	public MediaButtonsReceiver() {
		super();
	}

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
		if(action == KeyEvent.ACTION_DOWN){
		if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
				|| keycode == KeyEvent.KEYCODE_MEDIA_PLAY
				|| keycode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
			Timber.w("KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE");
			btnControlClick(context, ServiceRadioRx.EXTRA_WHICH_PLAY_PAUSE);
		} else if (keycode == KeyEvent.KEYCODE_MEDIA_NEXT) {
			Timber.w("KeyEvent.KEYCODE_MEDIA_PLAY_NEXT");
			btnControlClick(context, ServiceRadioRx.EXTRA_WHICH_NEXT);
		} else if (keycode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
			Timber.w("KeyEvent.KEYCODE_MEDIA_PLAY_PREV");
			btnControlClick(context, ServiceRadioRx.EXTRA_WHICH_PREVIOUS);
		} else if(keycode == KeyEvent.KEYCODE_HEADSETHOOK){
			Timber.w("KEYCODE_HEADSETHOOK");
		}

		}

	}


	private void btnControlClick(Context context, int extra){
		Log.i(TAG, "KEYCODE_MEDIA: "+extra);
			Intent intent = new Intent(context, ServiceRadioRx.class);
			intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
			intent.putExtra(EXTRA_WHICH, extra);
			context.startService(intent);
	}
}