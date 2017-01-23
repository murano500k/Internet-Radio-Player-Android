package com.stc.radio.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.stc.radio.player.utils.SettingsProvider;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.stc.radio.player.MusicService.ACTION_CMD;
import static com.stc.radio.player.MusicService.CMD_NAME;
import static com.stc.radio.player.MusicService.CMD_PAUSE;

/**
 * Created by artem on 1/23/17.
 */
public class AlarmReceiver extends BroadcastReceiver
{
	private static final String TAG = "AlarmReceiver";
	private static final int NOTIFICATION_SLEEP_ID = 125;
	public static final String SLEEP_TIMER_CANCEL = "com.stc.radio.player.SLEEP_TIMER_CANCEL";
	public static final String SLEEP_TIMER_START = "com.stc.radio.player.SLEEP_TIMER_START";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d(TAG, "onReceive: ");
		NotificationManager nm=(NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		if(SLEEP_TIMER_CANCEL.equals(intent.getAction()) ||
				!SettingsProvider.decrementTimerValueMinutes()){
			Toast.makeText(context, "Sleep timer finished", Toast.LENGTH_SHORT).show();
			nm.cancel(NOTIFICATION_SLEEP_ID);
			Intent i=new Intent(context, MusicService.class);
			i.setAction(ACTION_CMD);
			i.putExtra(CMD_NAME, CMD_PAUSE);
			context.startService(i);
		}else {
			Intent intentAlarm = new Intent(context, AlarmReceiver.class);
			intentAlarm.setAction(SLEEP_TIMER_CANCEL);
			Notification notification=new Notification.Builder(context)
					.setContentTitle("Sleep timer is running")
					.setContentText(SettingsProvider.getTimerValueMinutes()+" minutes left.\n " +
							"Click to cancel")
					.setSmallIcon(R.drawable.ic_launcher_white)
					.setContentIntent(
							PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT)
					).build();
			nm.notify(NOTIFICATION_SLEEP_ID, notification);
		}
	}

}
