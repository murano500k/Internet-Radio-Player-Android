package com.stc.radio.player.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.widget.RemoteViews;

import com.stc.radio.player.R;
import com.stc.radio.player.ServiceRadioRx;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.Station;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import timber.log.Timber;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.stc.radio.player.ui.MainActivity.INTENT_CLOSE_APP;
import static com.stc.radio.player.ui.MainActivity.INTENT_OPEN_APP;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

public class NotificationProviderRx {
	public static final String TAG = "NotificationProvider";
	private static final int NOTIFICATION_ID = 1431;
    private int artImage = R.drawable.default_art;
	private Bitmap art;

	private Context context;
    private NotificationManager mNotificationManager;
    private ServiceRadioRx serviceRadioRx;
	private String artist,song, stationName;
	Notification notification;

	public NotificationProviderRx(Context context, ServiceRadioRx serviceRadioRx) {
        this.context = context;
        mNotificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.serviceRadioRx=serviceRadioRx;
	   //if(Evemnt) EventBus.getDefault().register(this);
    }
	private Notification getNotification(){
		stationName="";
		artist=DbHelper.getNowPlaying().artist;
		song=DbHelper.getNowPlaying().song;
		if(DbHelper.getNowPlaying().getUiState()== MainActivity.UI_STATE.LOADING) {
			artist="loading...";
			song="";
		}
		else if (DbHelper.getNowPlaying().getUiState()== MainActivity.UI_STATE.IDLE) {
			artist="";
			song="";
		} else {
			if(artist==null) artist="";
			if(song==null) song="";
		}

		if(DbHelper.getNowPlaying().getUrl()!=null) {
			Station s = DbHelper.getActiveStation();
			assertNotNull(s);
			Timber.w("url=%s",s.url);
			stationName = s.name;
			File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath()
					+ "/" + s.artPath);
			if (file.exists()) {
				BitmapFactory.Options bmOptions = new BitmapFactory.Options();
				art = BitmapFactory.decodeFile(file.getPath(), bmOptions);
			} else {
				fail();
			}
		}

		RemoteViews mNotificationTemplate = new RemoteViews(serviceRadioRx.getApplicationContext().getPackageName(), R.layout.notification);
		RemoteViews mExpandedView = new RemoteViews(serviceRadioRx.getApplicationContext().getPackageName(), R.layout.notification_expanded);
		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, getServicePendingIntent(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED, 0));
		mExpandedView.setOnClickPendingIntent(R.id.notification_play, getServicePendingIntent(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED, 1));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev, getServicePendingIntent(ServiceRadioRx.EXTRA_PREV_PRESSED, 2));
		mExpandedView.setOnClickPendingIntent(R.id.notification_prev, getServicePendingIntent(ServiceRadioRx.EXTRA_PREV_PRESSED, 3));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next, getServicePendingIntent(ServiceRadioRx.EXTRA_NEXT_PRESSED, 4));
		mExpandedView.setOnClickPendingIntent(R.id.notification_next, getServicePendingIntent(ServiceRadioRx.EXTRA_NEXT_PRESSED, 5));

		mNotificationTemplate.setOnClickPendingIntent(R.id.album_art, getActivityPendingIntent(INTENT_OPEN_APP, 6));
		mExpandedView.setOnClickPendingIntent(R.id.album_art, getActivityPendingIntent(INTENT_OPEN_APP, 7));

		mNotificationTemplate.setTextViewText(R.id.extra_info, stationName);
		mNotificationTemplate.setTextViewText(R.id.artist, artist);
		mNotificationTemplate.setTextViewText(R.id.song, song);
		mNotificationTemplate.setImageViewBitmap(R.id.album_art, art);

		mExpandedView.setTextViewText(R.id.extra_info, stationName);
		mExpandedView.setTextViewText(R.id.artist, artist);
		mExpandedView.setTextViewText(R.id.song, song);
		mExpandedView.setImageViewBitmap(R.id.album_art, art);

		Notification notification = new Notification.Builder(context).setSmallIcon(R.mipmap.ic_launcher)
				.setPriority(Notification.PRIORITY_HIGH)
				.setDeleteIntent(getActivityPendingIntent(INTENT_CLOSE_APP, 10))
				.setContent(mNotificationTemplate)
				.build();
		notification.bigContentView = mExpandedView;
		switch(DbHelper.getNowPlaying().getUiState()){
			case MainActivity.UI_STATE.PLAYING:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_pause_new);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_pause_new);
				notification.flags = 0;
				notification.ledOnMS = 0;
				break;
			case MainActivity.UI_STATE.IDLE:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_play_new);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_play_new);
				notification.flags = 0;
				notification.ledOnMS = 0;
				break;
			case  MainActivity.UI_STATE.LOADING:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
				notification.ledARGB = 0xFF00FF7F;
				notification.flags = Notification.FLAG_SHOW_LIGHTS/* | Notification.FLAG_FOREGROUND_SERVICE*/;
				notification.ledOnMS = 100;
				notification.ledOffMS = 100;
				break;
		}
		return notification;
	}

	public void cancelNotification(){
		if(mNotificationManager!=null)mNotificationManager.cancel(NOTIFICATION_ID);
	}

	private PendingIntent getActivityPendingIntent(String action, int i){
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(context, MainActivity.class);
		intent.addFlags(FLAG_UPDATE_CURRENT);
		intent.setAction(action);
		return PendingIntent.getActivity(context,i, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	private PendingIntent getActivityBackgroundIntent(String action, int i){
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_FROM_BACKGROUND );
		intent.setAction(action);
		return PendingIntent.getActivity(context,i, intent, PendingIntent.FLAG_NO_CREATE);
	}
	private PendingIntent getServicePendingIntent(String extra, int index){
		//Log.i(TAG, "getServicePendingIntent: "+ extra);
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(serviceRadioRx.getApplicationContext(), ServiceRadioRx.class);
		intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
		intent.putExtra(extra, extra);
		return PendingIntent.getService(serviceRadioRx.getApplicationContext(), index, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}



	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onMetadataChanged(Metadata metadata) {
		if(notification==null) {
			notification=getNotification();
		}else {
			artist = "";
			song = "";
			if (DbHelper.getNowPlaying().artist != null) artist = DbHelper.getNowPlaying().artist;
			if (DbHelper.getNowPlaying().song != null) song = DbHelper.getNowPlaying().song;
			notification.contentView.setTextViewText(R.id.artist, artist);
			notification.bigContentView.setTextViewText(R.id.song, song);
		}
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}



	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onUiStateChanged(UiEvent event) {
		notification=getNotification();
		mNotificationManager.notify(NOTIFICATION_ID, notification);
/*
		assertNotNull(event);
		//DbHelper.updateNowPlaying(event);

		UiEvent.UI_ACTION action =event.getUiAction();
		Timber.d("onUiStateChanged %s", action);
		switch(action){
			case PLAYBACK_STARTED:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_pause_new);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_pause_new);
				notification.flags = 0;
				notification.ledOnMS = 0;
				break;
			case PLAYBACK_STOPPED:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_play_new);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_play_new);
				notification.flags = 0;
				notification.ledOnMS = 0;
				break;
			case LOADING_STARTED:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
				notification.ledARGB = 0xFF00FF7F;
				notification.flags = Notification.FLAG_SHOW_LIGHTS*//* | Notification.FLAG_FOREGROUND_SERVICE*//*;
				notification.ledOnMS = 100;
				notification.ledOffMS = 100;
				break;
		}
		stationName="";
		if(event.getExtras().url!=null) stationName=DbHelper.getNameFromUrl(event.getExtras().url);
		Timber.d("stationName %s", stationName);
		notification.contentView.setTextViewText(R.id.extra_info, stationName);
		notification.bigContentView.setTextViewText(R.id.extra_info, stationName);
		mNotificationManager.notify(NOTIFICATION_ID, notification);*/
	}/*
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNewArt(Bitmap bitmap){
		assertNotNull(bitmap);
		Timber.d("onNewArt %s", bitmap.toString());
		if(art==null || !art.equals(bitmap)) {
			art=bitmap;
			if (notification == null) notification = getNotification();
			notification.contentView.setImageViewBitmap(R.id.album_art, bitmap);
			notification.bigContentView.setImageViewBitmap(R.id.album_art, bitmap);
			mNotificationManager.notify(NOTIFICATION_ID, notification);
		}
	}*/





}
