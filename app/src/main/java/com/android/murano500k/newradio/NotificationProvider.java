package com.android.murano500k.newradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import static com.android.murano500k.newradio.Constants.UI_STATE.LOADING;
import static com.android.murano500k.newradio.Constants.UI_STATE.PLAYING;
import static com.android.murano500k.newradio.R.layout.notification;


/**
 * Created by artem on 8/8/16.
 */
public class NotificationProvider implements ListenerRadio {
	public static final String TAG = "NotificationProvider";

	private String stationName = "";
    private String singerName = "";
    private String songName = "";
    private int artImage = R.drawable.default_art;
    private int status;

    private Context context;
    private NotificationManager mNotificationManager;
    private ServiceRadio serviceRadio;
    private String text;
    private PlaylistManager playlistManager;


    public NotificationProvider(Context context, ServiceRadio serviceRadio) {
        this.context = context;
        mNotificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        playlistManager=new PlaylistManager(context);
        this.serviceRadio=serviceRadio;
    }

    private void buildNotification(boolean updateNotif) {
	    Intent intentPause = new Intent(context, ServiceRadio.class);
	    intentPause.setAction(Constants.INTENT.PLAYBACK.PAUSE);

	    Intent intentPlay = new Intent(context, ServiceRadio.class);
	    intentPlay.setAction(Constants.INTENT.PLAYBACK.RESUME);

	    Intent intentPrev = new Intent(context, ServiceRadio.class);
	    intentPrev.setAction(Constants.INTENT.PLAYBACK.PLAY_PREV);

	    Intent intentNext = new Intent(context, ServiceRadio.class);
	    intentNext.setAction(Constants.INTENT.PLAYBACK.PLAY_NEXT);

	    Intent intentCancel = new Intent(context, ServiceRadio.class);
	    intentCancel.setAction(Constants.INTENT.CLOSE_NOTIFICATION);

	    Intent intentOpenPlayer = new Intent(context, ActivityRadio.class);
	    intentOpenPlayer.setAction(Constants.INTENT.OPEN_APP);

	    PendingIntent prevPending = PendingIntent.getService(context, 0, intentPrev, 0);
	    PendingIntent nextPending = PendingIntent.getService(context, 0, intentNext, 0);
	    PendingIntent playPending = PendingIntent.getService(context, 0, intentPlay, 0);
	    PendingIntent pausePending = PendingIntent.getService(context, 0, intentPause, 0);
	    PendingIntent openPending = PendingIntent.getActivity(context, 0, intentOpenPlayer, 0);
	    PendingIntent cancelPending = PendingIntent.getService(context, 0, intentCancel, 0);

	    RemoteViews mNotificationTemplate = new RemoteViews(context.getPackageName(), notification);
	    RemoteViews mExpandedView = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
	    Notification.Builder notificationBuilder = new Notification.Builder(context);
	    this.stationName = PlaylistManager.getNameFromUrl(playlistManager.getSelectedUrl());
	    artImage=PlaylistManager.getArt(stationName, context);

	    if(playlistManager!=null) {
		    switch (status){
			    case LOADING:
				    mNotificationTemplate.setViewVisibility(R.id.notification_spinner, View.VISIBLE);
				    mExpandedView.setViewVisibility(R.id.notification_expanded_spinner, View.VISIBLE);

				    mNotificationTemplate.setViewVisibility(R.id.notification_play, View.GONE);
				    mExpandedView.setViewVisibility(R.id.notification_expanded_play, View.GONE);

				    mNotificationTemplate.setImageViewResource(R.id.notification_play, R.drawable.ic_pause);
				    mExpandedView.setImageViewResource(R.id.notification_expanded_play, R.drawable.ic_pause);

				    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, pausePending);
				    mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, pausePending);
				    break;

			    case PLAYING:

				    mNotificationTemplate.setViewVisibility(R.id.notification_spinner, View.GONE);
				    mExpandedView.setViewVisibility(R.id.notification_expanded_spinner, View.GONE);

				    mNotificationTemplate.setViewVisibility(R.id.notification_play, View.VISIBLE);
				    mExpandedView.setViewVisibility(R.id.notification_expanded_play, View.VISIBLE);

				    mNotificationTemplate.setImageViewResource(R.id.notification_play, R.drawable.ic_pause);
				    mExpandedView.setImageViewResource(R.id.notification_expanded_play, R.drawable.ic_pause);

				    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, pausePending);
				    mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, pausePending);
				    break;

			    case Constants.UI_STATE.IDLE:
				    mNotificationTemplate.setViewVisibility(R.id.notification_spinner, View.GONE);
				    mExpandedView.setViewVisibility(R.id.notification_expanded_spinner, View.GONE);

				    mNotificationTemplate.setViewVisibility(R.id.notification_play, View.VISIBLE);
				    mExpandedView.setViewVisibility(R.id.notification_expanded_play, View.VISIBLE);

				    mNotificationTemplate.setImageViewResource(R.id.notification_play, R.drawable.ic_play);
				    mExpandedView.setImageViewResource(R.id.notification_expanded_play, R.drawable.ic_play);

				    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, playPending);
				    mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, playPending);
				    break;

			    default:
				    mNotificationTemplate.setViewVisibility(R.id.notification_spinner, View.GONE);
				    mExpandedView.setViewVisibility(R.id.notification_expanded_spinner, View.GONE);

				    mNotificationTemplate.setViewVisibility(R.id.notification_play, View.VISIBLE);
				    mExpandedView.setViewVisibility(R.id.notification_expanded_play, View.VISIBLE);

				    mNotificationTemplate.setImageViewResource(R.id.notification_play, R.drawable.ic_play);
				    mExpandedView.setImageViewResource(R.id.notification_expanded_play, R.drawable.ic_play);

				    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, playPending);
				    mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, playPending);
				    break;
		    }


            mNotificationTemplate.setImageViewResource(R.id.notification_prev, R.drawable.ic_prev);
            mNotificationTemplate.setImageViewResource(R.id.notification_next, R.drawable.ic_next);
            mNotificationTemplate.setImageViewResource(R.id.notification_artimage, artImage);

		    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_artimage, openPending);
            mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev, prevPending);
            mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next, nextPending);




                mExpandedView.setTextViewText(R.id.notification_expanded_station_name, stationName);
                mExpandedView.setImageViewResource(R.id.notification_expanded_prev, R.drawable.ic_prev);
                mExpandedView.setImageViewResource(R.id.notification_expanded_next, R.drawable.ic_next);
                mExpandedView.setImageViewResource(R.id.notification_expanded_artimage, artImage);

	            mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_artimage, openPending);
                mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_prev, prevPending);
                mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_next, nextPending);
		    Notification notification = notificationBuilder
				    .setSmallIcon(artImage)
				    .setPriority(Notification.PRIORITY_DEFAULT)
				    .setDeleteIntent(cancelPending)
				    .setContent(mNotificationTemplate)
				    .build();
                notification.bigContentView = mExpandedView;

	            if (updateNotif) mNotificationManager.notify(Constants.NOTIFICATION_ID, notification);
	            else mNotificationManager.cancel(Constants.NOTIFICATION_ID);

	    }

    }


	@Override
	public void onFinish() {
		if(mNotificationManager!=null) mNotificationManager.cancel(Constants.NOTIFICATION_ID);

	}

	@Override
    public void onLoadingStarted() {
        status=Constants.UI_STATE.LOADING;
        text=null;
        singerName="";
        songName="";
        buildNotification(true);
    }

    @Override
    public void onProgressUpdate(int p, int pMax, String s) {
    }

    @Override
    public void onRadioConnected() {
    }

    @Override
    public void onPlaybackStarted(String url) {
        status=Constants.UI_STATE.PLAYING;
        text=null;
        singerName="";
        songName="";
        buildNotification(true);
    }

    @Override
    public void onPlaybackStopped(boolean updateNotification) {
        status=Constants.UI_STATE.IDLE;
        text=null;
        singerName="";
        songName="";
        buildNotification(updateNotification);
    }

    @Override
    public void onMetaDataReceived(String s, String s2) {
        if(s!=null && s.equals("StreamTitle")) {
            text= PlaylistManager.getArtistFromString(s2) + " - "
                    +PlaylistManager.getTrackFromString(s2);
            singerName= PlaylistManager.getArtistFromString(s2);
            songName=PlaylistManager.getTrackFromString(s2);
        }
        status=Constants.UI_STATE.PLAYING;
        buildNotification(true);
    }

    @Override
    public void onPlaybackError(boolean updateNotification) {

        status=Constants.UI_STATE.IDLE;
        text=null;
        singerName="";
        songName="";
	    buildNotification(updateNotification);
    }



	@Override
    public void onStationSelected(String url) {
        text=PlaylistManager.getNameFromUrl(playlistManager.getSelectedUrl());
        buildNotification(true);
    }

    @Override
    public void onSleepTimerStatusUpdate(String action, int seconds) {

    }


}
