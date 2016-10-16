package com.stc.radio.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stc.radio.player.db.Metadata;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.utils.PabloPicasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.stc.radio.player.db.NowPlaying.STATUS_IDLE;
import static com.stc.radio.player.db.NowPlaying.STATUS_PLAYING;

public class NotificationProviderRx implements Target{
	public static final String TAG = "NotificationProvider";
	private static final int NOTIFICATION_ID = 1431;
	private static final int REQUEST_CODE = 100;
	private PendingIntent mOpenIntent;

	private int artImage = R.drawable.default_art;
	private Context context;
	private EventBus bus = EventBus.getDefault();
	String song;
	String artist;
	Bitmap bitmap;
	String artUrl;
	private Metadata metadata;

	private String station;

	private final ServiceRadioRx mService;
	private MediaSessionCompat.Token mSessionToken;

	private int mPlaybackState;
	private Metadata mMetadata;

	private NotificationManager mNotificationManager;

	private PendingIntent mPlayPauseIntent;
	private PendingIntent mPlayIntent;
	private PendingIntent mPreviousIntent;
	private PendingIntent mNextIntent;

	private int mNotificationColor;

	private boolean mStarted = false;


	public NotificationProviderRx(Context context, ServiceRadioRx service) {
		mService = service;
		this.context = context;
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		mPlayPauseIntent = createServicePendingIntent(0);
		mPreviousIntent = createServicePendingIntent(-1);
		mNextIntent = createServicePendingIntent(1);
		mOpenIntent = createContentIntent();
		//if(Evemnt) EventBus.getDefault().register(this);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNowPlayingUpdate(NowPlaying nowPlaying) {
		if (nowPlaying.getStation() == null) cancelNotification();
		else if (!nowPlaying.getStation().equals(station)
				|| nowPlaying.getStatus() != mPlaybackState) {
			station = nowPlaying.getStation().getName();
			mPlaybackState = nowPlaying.getStatus();
			metadata = nowPlaying.getMetadata();
			if (this.artUrl==null || !this.artUrl.equals(nowPlaying.getStation().getArtUrl())) {
				this.artUrl = nowPlaying.getStation().getArtUrl();
				if (this.artUrl != null && this.artUrl.contains("http")) {
					PabloPicasso.with(mService).load(Uri.parse(artUrl)).into(this);
					return;
				}
			}
			Notification notification = createNotificationMedia();
			if (notification != null) {
				mNotificationManager.notify(NOTIFICATION_ID, notification);
			}

		}
	}
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onMetadataUpdate(Metadata metadata) {
		this.metadata = metadata;
		Notification notification = createNotificationMedia();
		if (notification != null) {
			mNotificationManager.notify(NOTIFICATION_ID, notification);
		}
	}

	private Notification getNotification() {/*
		RemoteViews mNotificationTemplate = new RemoteViews(mService.getApplicationContext().getPackageName(), notification);
		RemoteViews mExpandedView = new RemoteViews(mService.getApplicationContext().getPackageName(), R.layout.notification_expanded);
		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play,
				createServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_PLAY_PAUSE, 0));
		mExpandedView.setOnClickPendingIntent(R.id.notification_play,
				createServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_PLAY_PAUSE, 1));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev,
				createServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_PREVIOUS, 2));
		mExpandedView.setOnClickPendingIntent(R.id.notification_prev,
				createServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_PREVIOUS, 3));

		mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next,
				createServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_NEXT, 4));
		mExpandedView.setOnClickPendingIntent(R.id.notification_next,
				createServicePendingIntent(ServiceRadioRx.EXTRA_WHICH_NEXT, 5));

		mNotificationTemplate.setOnClickPendingIntent(R.id.album_art,
				getActivityPendingIntent(MainActivity.INTENT_OPEN_APP, 6));
		mExpandedView.setOnClickPendingIntent(R.id.album_art,
				getActivityPendingIntent(MainActivity.INTENT_OPEN_APP, 7));


		String stationName = station.getName();
		artist = "";
		song = "";
		if (metadata != null) {
			if (metadata.getArtist() != null) artist = metadata.getArtist();
			if (metadata.getSong() != null) song = metadata.getSong();
		}
		mNotificationTemplate.setTextViewText(R.id.extra_info, stationName);
		mNotificationTemplate.setTextViewText(R.id.artist, artist);
		mNotificationTemplate.setTextViewText(R.id.song, song);
		mExpandedView.setTextViewText(R.id.extra_info, stationName);
		mExpandedView.setTextViewText(R.id.artist, artist);
		mExpandedView.setTextViewText(R.id.song, song);
		Notification notification = new Notification.Builder(context).setSmallIcon(R.mipmap.ic_launcher)
				.setPriority(Notification.PRIORITY_HIGH)
				.setDeleteIntent(createClosePendingIntent())
				.setContent(mNotificationTemplate)
				.build();
		notification.bigContentView = mExpandedView;

		Timber.d("status %d", status);
		switch (status) {
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
				notification.flags = Notification.FLAG_SHOW_LIGHTS*//* | Notification.FLAG_FOREGROUND_SERVICE*//*;
				notification.ledOnMS = 100;
				notification.ledOffMS = 100;
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
				break;
		}
		PabloPicasso.with(context).load(station.getArtUrl()).error(R.drawable.default_art).into(mNotificationTemplate, R.id.album_art, NOTIFICATION_ID, notification);
		PabloPicasso.with(context).load(station.getArtUrl()).error(R.drawable.default_art).into(mExpandedView, R.id.album_art, NOTIFICATION_ID, notification);
		return notification;
	}*/return null;
	}
	public void cancelNotification() {
		if (mNotificationManager != null) mNotificationManager.cancel(NOTIFICATION_ID);
	}


	private PendingIntent createContentIntent() {
		Intent openUI = new Intent(mService.getApplicationContext(), MainActivity.class);
		openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		openUI.setAction(MainActivity.INTENT_CLOSE_APP);
		return PendingIntent.getActivity(mService.getApplicationContext(), REQUEST_CODE, openUI,
				FLAG_UPDATE_CURRENT);
	}

	private PendingIntent createServicePendingIntent(int which) {
		//Log.i(TAG, "createServicePendingIntent: "+ extra);
		String pkg = mService.getPackageName();
		return PendingIntent.getBroadcast(mService.getApplicationContext(), REQUEST_CODE,
				new Intent(ServiceRadioRx.INTENT_USER_ACTION)
						.setClass(mService.getApplicationContext(),ServiceRadioRx.class)
						.setPackage(pkg)
						.putExtra(ServiceRadioRx.EXTRA_WHICH, which),
				FLAG_UPDATE_CURRENT);
	}

	private PendingIntent createClosePendingIntent() {
		//Log.i(TAG, "createServicePendingIntent: "+ extra);
		Intent closeUI = new Intent(mService, MainActivity.class);
		closeUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		closeUI.setAction(MainActivity.INTENT_CLOSE_APP);
		return PendingIntent.getActivity(mService, REQUEST_CODE, closeUI,
				FLAG_UPDATE_CURRENT);
	}
	private PendingIntent getActivityPendingIntent(String action, int i){
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(context, MainActivity.class);
		intent.addFlags(FLAG_UPDATE_CURRENT);
		intent.setAction(action);
		return PendingIntent.getActivity(context,i, intent, FLAG_UPDATE_CURRENT);
	}

	private PendingIntent getServicePendingIntent(int which, int index){
		//Log.i(TAG, "getServicePendingIntent: "+ extra);
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(mService.getApplicationContext(), ServiceRadioRx.class);
		intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
		intent.putExtra(ServiceRadioRx.EXTRA_WHICH, which);
		return PendingIntent.getService(mService.getApplicationContext(), index, intent, FLAG_UPDATE_CURRENT);
	}

	private Notification createNotificationMedia() {

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mService);
		int playPauseButtonPosition = 0;

		// If skip to previous action is enabled
		notificationBuilder.addAction(R.drawable.ic_skip_previous_white_24dp,
				mService.getString(R.string.label_previous), getServicePendingIntent(-1,0));

		playPauseButtonPosition = 1;

		addPlayPauseAction(notificationBuilder);
		notificationBuilder.addAction(R.drawable.ic_skip_next_white_24dp,
				mService.getString(R.string.label_next), getServicePendingIntent(1,2));

		String contentText = "";
		if(metadata!=null)contentText+=metadata.getArtist() + "" + metadata.getSong();
		String contentTitle = "" + station;

		notificationBuilder
//                .setStyle(new Notification.MediaStyle()
//                        .setShowActionsInCompactView(
//                                new int[]{playPauseButtonPosition})  // show only play/pause in compact view
//                        .setMediaSession(mSessionToken))
				.setColor(mNotificationColor)
				.setSmallIcon(R.drawable.ic_notification)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.setContentIntent(getActivityPendingIntent(MainActivity.INTENT_OPEN_APP,3))
				.setContentTitle(contentTitle)
				.setContentText(contentText)
				.setPriority(Notification.PRIORITY_HIGH)
				.setDeleteIntent(getActivityPendingIntent(MainActivity.INTENT_CLOSE_APP, 4));
		if(bitmap==null){
			notificationBuilder.setLargeIcon(drawableToBitmap(mService.getDrawable(artImage)));
		}else {
			notificationBuilder.setLargeIcon(bitmap);
		}
		return notificationBuilder.build();
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		Bitmap bitmap = null;

		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if(bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}

		if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	private void addPlayPauseAction(NotificationCompat.Builder builder) {
		String label;
		int icon;
		PendingIntent intent;
		if (mPlaybackState == STATUS_PLAYING) {
			label = mService.getString(R.string.label_pause);
			icon = R.drawable.uamp_ic_pause_white_24dp;
		} else if (mPlaybackState == STATUS_IDLE) {
			label = mService.getString(R.string.label_play);
			icon = R.drawable.uamp_ic_play_arrow_white_24dp;
		} else {
			label = mService.getString(R.string.label_loading);
			icon = R.drawable.ic_loading_24dp;
		}
		intent = getServicePendingIntent(0, 1);
		builder.addAction(new NotificationCompat.Action(icon, label, intent));
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
		//if(this.bitmap!=null)bitmap.recycle();
		this.bitmap=bitmap;
		Notification notification=createNotificationMedia();
		if(notification!=null) mNotificationManager.notify(NOTIFICATION_ID,notification);
	}

	@Override
	public void onBitmapFailed(Drawable errorDrawable) {

	}

	@Override
	public void onPrepareLoad(Drawable placeHolderDrawable) {

	}
}
