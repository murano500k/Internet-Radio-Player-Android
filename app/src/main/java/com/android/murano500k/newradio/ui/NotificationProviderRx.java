package com.android.murano500k.newradio.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.android.murano500k.newradio.PlayerStatus;
import com.android.murano500k.newradio.PlaylistManager;
import com.android.murano500k.newradio.R;
import com.android.murano500k.newradio.ServiceRadioRx;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.android.murano500k.newradio.R.layout.notification;


/**
 * Created by artem on 8/8/16.
 */
public class NotificationProviderRx {
	public static final String TAG = "NotificationProvider";
	public static final int NOTIFICATION_ID = 1431;
	private String stationName = "";
    private int artImage = R.drawable.default_art;

    private Context context;
    private NotificationManager mNotificationManager;
    private ServiceRadioRx serviceRadioRx;
    private PlaylistManager playlistManager;


    public NotificationProviderRx(Context context, ServiceRadioRx serviceRadioRx) {
        this.context = context;
        mNotificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        playlistManager=new PlaylistManager(context);
        this.serviceRadioRx=serviceRadioRx;
	    EventBus.getDefault().register(this);

    }
	public void updateNotification(UiEvent.UI_ACTION action, PlayerStatus playerStatus){


		RemoteViews mNotificationTemplate = new RemoteViews(serviceRadioRx.getApplicationContext().getPackageName(), notification);
		RemoteViews mExpandedView = new RemoteViews(serviceRadioRx.getApplicationContext().getPackageName(), R.layout.notification_expanded);
		Notification.Builder notificationBuilder = new Notification.Builder(context);
		if(playlistManager.getSelectedUrl()!=null) this.stationName = playlistManager.getNameFromUrl(playlistManager.getSelectedUrl());
		else stationName="";
		artImage=playlistManager.getArt(stationName, context);

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, getServicePendingIntent(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED, 0));
		mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, getServicePendingIntent(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED, 1));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev, getServicePendingIntent(ServiceRadioRx.EXTRA_PREV_PRESSED, 2));
		mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_prev, getServicePendingIntent(ServiceRadioRx.EXTRA_PREV_PRESSED, 3));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next, getServicePendingIntent(ServiceRadioRx.EXTRA_NEXT_PRESSED, 4));
		mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_next, getServicePendingIntent(ServiceRadioRx.EXTRA_NEXT_PRESSED, 5));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_artimage, getActivityPendingIntent(ActivityRxTest.INTENT_OPEN_APP, 6));

		mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_artimage, getActivityPendingIntent(ActivityRxTest.INTENT_OPEN_APP, 6));

		mNotificationTemplate.setImageViewResource(R.id.notification_prev, R.drawable.ic_prev);
		mNotificationTemplate.setImageViewResource(R.id.notification_next, R.drawable.ic_next);
		mNotificationTemplate.setImageViewResource(R.id.notification_artimage, artImage);

		mExpandedView.setTextViewText(R.id.notification_expanded_station_name, stationName);
		mExpandedView.setImageViewResource(R.id.notification_expanded_prev, R.drawable.ic_prev);
		mExpandedView.setImageViewResource(R.id.notification_expanded_next, R.drawable.ic_next);
		mExpandedView.setImageViewResource(R.id.notification_expanded_artimage, artImage);

		if(action==UiEvent.UI_ACTION.LOADING_STARTED) {
			mNotificationTemplate.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
			mExpandedView.setImageViewResource(R.id.notification_expanded_play, R.drawable.ic_loading);
		}else if(action==UiEvent.UI_ACTION.PLAYBACK_STARTED) {
			mNotificationTemplate.setImageViewResource(R.id.notification_play, R.drawable.ic_pause);
			mExpandedView.setImageViewResource(R.id.notification_expanded_play, R.drawable.ic_pause);
		}else if(action==UiEvent.UI_ACTION.PLAYBACK_STOPPED) {
			mNotificationTemplate.setImageViewResource(R.id.notification_play, R.drawable.ic_play);
			mExpandedView.setImageViewResource(R.id.notification_expanded_play, R.drawable.ic_play);
		}
		Notification notification = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			notificationBuilder = notificationBuilder
					.setColor(serviceRadioRx.getColor(R.color.colorPrimary));
		}
			notification = notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
					.setPriority(Notification.PRIORITY_HIGH)
					.setDeleteIntent(getActivityPendingIntent(ActivityRxTest.INTENT_CLOSE_APP, 10))
					.setContent(mNotificationTemplate)
					.build();

		notification.bigContentView = mExpandedView;

		if(playerStatus==ServiceRadioRx.STATUS_WAITING_CONNECTIVITY){
			//notification.ledARGB = 0xFFff0000;
			notification.ledARGB = 0xFF00FF7F;
			notification.flags = Notification.FLAG_SHOW_LIGHTS/* | Notification.FLAG_FOREGROUND_SERVICE*/;
			notification.ledOnMS = 100;
			notification.ledOffMS = 100;
		}else if(playerStatus!=ServiceRadioRx.STATUS_PLAYING &&
				playerStatus!=ServiceRadioRx.STATUS_STOPPED){
			notification.ledARGB = 0xFF00FF7F;
			notification.flags = Notification.FLAG_SHOW_LIGHTS;
			notification.ledOnMS = 2000;
			notification.ledOffMS = 50;
		}
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
	public void cancelNotification(){
		if(mNotificationManager!=null)mNotificationManager.cancel(NOTIFICATION_ID);
	}

	private PendingIntent getActivityPendingIntent(String action, int i){
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(serviceRadioRx.getApplicationContext(), ActivityRxTest.class);
		intent.addFlags(FLAG_UPDATE_CURRENT);
		intent.setAction(action);
		return PendingIntent.getActivity(serviceRadioRx.getApplicationContext(), i, intent, PendingIntent.FLAG_UPDATE_CURRENT);

	}
	private PendingIntent getServicePendingIntent(String extra, int index){
		//Log.i(TAG, "getServicePendingIntent: "+ extra);
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(serviceRadioRx.getApplicationContext(), ServiceRadioRx.class);
		intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
		intent.putExtra(extra, extra);
		return PendingIntent.getService(serviceRadioRx.getApplicationContext(), index, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private Intent getServiceIntent(String extra){
		//Log.i(TAG, "getServicePendingIntent: "+ extra);
		Intent intent = new Intent(serviceRadioRx.getApplicationContext(), ServiceRadioRx.class);
		intent.setClass(serviceRadioRx.getApplicationContext(), ServiceRadioRx.class);
		intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
		intent.putExtra(extra, extra);
		return intent;
	}
	private PendingIntent getServicePendingIntentTemplate(){
		Intent intent = new Intent(serviceRadioRx.getApplicationContext(), ServiceRadioRx.class);
		intent.setClass(serviceRadioRx.getApplicationContext(), ServiceRadioRx.class);
		intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
		return PendingIntent.getService(serviceRadioRx.getApplicationContext(), 2, intent, 0);
	}





	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onUiEvent(UiEvent eventUi) {
		UiEvent.UI_ACTION action=eventUi.getUiAction();
		if(action.equals(UiEvent.UI_ACTION.LOADING_STARTED) || action.equals(UiEvent.UI_ACTION.PLAYBACK_STARTED)
				||action.equals(UiEvent.UI_ACTION.PLAYBACK_STOPPED)){
			updateNotification(action, eventUi.getExtras().playerStatus);
		}
	}


}
