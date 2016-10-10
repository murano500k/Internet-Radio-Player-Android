package com.stc.radio.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.Metadata;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import timber.log.Timber;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.stc.radio.player.R.layout.notification;
import static com.stc.radio.player.db.NowPlaying.STATUS_IDLE;
import static com.stc.radio.player.db.NowPlaying.STATUS_PAUSING;
import static com.stc.radio.player.db.NowPlaying.STATUS_PLAYING;
import static com.stc.radio.player.db.NowPlaying.STATUS_STARTING;
import static com.stc.radio.player.db.NowPlaying.STATUS_SWITCHING;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_CONNECTIVITY;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_FOCUS;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_UNMUTE;

public class NotificationProviderRx {
	public static final String TAG = "NotificationProvider";
	private static final int NOTIFICATION_ID = 1431;
    private int artImage = R.drawable.default_art;
	private Context context;
    private NotificationManager mNotificationManager;
    private ServiceRadioRx serviceRadioRx;
	private EventBus bus=EventBus.getDefault();
	String song;
	String artist;
	Bitmap art;
	private Metadata metadata;
	private int status;
	private Station station;

	public NotificationProviderRx(Context context, ServiceRadioRx serviceRadioRx) {
        this.context = context;
        mNotificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.serviceRadioRx=serviceRadioRx;
	   //if(Evemnt) EventBus.getDefault().register(this);
    }
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNowPlayingUpdate(NowPlaying nowPlaying) {
		if(nowPlaying.getStation()==null) cancelNotification();
		else if(!nowPlaying.getStation().equals(station)
					|| nowPlaying.getStatus()!=status
					|| !nowPlaying.getMetadata().equals(metadata)){
				station=nowPlaying.getStation();
				status=nowPlaying.getStatus();
				metadata=nowPlaying.getMetadata();
				mNotificationManager.notify(NOTIFICATION_ID, getNotification());
			}
		}


	private Notification getNotification(){
		RemoteViews mNotificationTemplate = new RemoteViews(serviceRadioRx.getApplicationContext().getPackageName(), notification);
		RemoteViews mExpandedView = new RemoteViews(serviceRadioRx.getApplicationContext().getPackageName(), R.layout.notification_expanded);
		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, getServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_PLAY_PAUSE, 0));
		mExpandedView.setOnClickPendingIntent(R.id.notification_play, getServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_PLAY_PAUSE, 1));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev, getServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_PREVIOUS, 2));
		mExpandedView.setOnClickPendingIntent(R.id.notification_prev, getServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_PREVIOUS, 3));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next, getServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_NEXT, 4));
		mExpandedView.setOnClickPendingIntent(R.id.notification_next, getServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_NEXT, 5));

		mNotificationTemplate.setOnClickPendingIntent(R.id.album_art, getActivityPendingIntent(MainActivity.INTENT_OPEN_APP, 6));
		mExpandedView.setOnClickPendingIntent(R.id.album_art, getActivityPendingIntent(MainActivity.INTENT_OPEN_APP, 7));



		String stationName=station.name;
		artist="";
		if(metadata.getArtist()!=null) artist=metadata.getArtist();
		song="";
		if(metadata.getSong()!=null) song=metadata.getSong();
		mNotificationTemplate.setTextViewText(R.id.extra_info, stationName);
		mNotificationTemplate.setTextViewText(R.id.artist, artist);
		mNotificationTemplate.setTextViewText(R.id.song, song);
		mExpandedView.setTextViewText(R.id.extra_info, stationName);
		mExpandedView.setTextViewText(R.id.artist, artist);
		mExpandedView.setTextViewText(R.id.song, song);

		art=NowPlaying.getInstance().getStationArtBitmap(station);
		if(art!=null) {
			mNotificationTemplate.setImageViewBitmap(R.id.album_art, art);
			mExpandedView.setImageViewBitmap(R.id.album_art, art);
		}else {
			mNotificationTemplate.setImageViewResource(R.id.album_art, R.drawable.default_art);
			mExpandedView.setImageViewResource(R.id.album_art, R.drawable.default_art);
		}
		Notification notification = new Notification.Builder(context).setSmallIcon(R.mipmap.ic_launcher)
				.setPriority(Notification.PRIORITY_HIGH)
				.setDeleteIntent(getActivityPendingIntent(MainActivity.INTENT_CLOSE_APP, 10))
				.setContent(mNotificationTemplate)
				.build();
		notification.bigContentView = mExpandedView;

		Timber.d("status %d", status);
			switch (status){
				case STATUS_IDLE:
					notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_play_new);
					notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_play_new);
					notification.flags = 0;
					notification.ledOnMS = 0;
					break;
				case STATUS_PLAYING:
					notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_pause_new);
					notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_pause_new);
					notification.flags = 0;
					notification.ledOnMS = 0;
					break;
				case STATUS_PAUSING:
					mNotificationTemplate.setTextViewText(R.id.artist, "PAUSING");
					mExpandedView.setTextViewText(R.id.artist, "PAUSING");
				case STATUS_STARTING:
					mNotificationTemplate.setTextViewText(R.id.artist, "STARTING");
					mExpandedView.setTextViewText(R.id.artist, "STARTING");
				case STATUS_SWITCHING:
					mNotificationTemplate.setTextViewText(R.id.artist, "SWITCHING");
					mExpandedView.setTextViewText(R.id.artist, "SWITCHING");
				case STATUS_WAITING_CONNECTIVITY:
					mNotificationTemplate.setTextViewText(R.id.artist, "WAITING_CONNECTIVITY");
					mExpandedView.setTextViewText(R.id.artist, "WAITING_CONNECTIVITY");
				case STATUS_WAITING_FOCUS:
					mNotificationTemplate.setTextViewText(R.id.artist, "WAITING_FOCUS");
					mExpandedView.setTextViewText(R.id.artist, "WAITING_FOCUS");
				case STATUS_WAITING_UNMUTE:
					mNotificationTemplate.setTextViewText(R.id.artist, "WAITING_UNMUTE");
					mExpandedView.setTextViewText(R.id.artist, "WAITING_UNMUTE");
					notification.ledARGB = 0xFF00FF7F;
					notification.flags = Notification.FLAG_SHOW_LIGHTS/* | Notification.FLAG_FOREGROUND_SERVICE*/;
					notification.ledOnMS = 100;
					notification.ledOffMS = 100;
					notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
					notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
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

	private PendingIntent getServicePendingIntent(int which, int index){
		//Log.i(TAG, "getServicePendingIntent: "+ extra);
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(serviceRadioRx.getApplicationContext(), ServiceRadioRx.class);
		intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
		intent.putExtra(ServiceRadioRx.EXTRA_WHICH, which);
		return PendingIntent.getService(serviceRadioRx.getApplicationContext(), index, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}



}
