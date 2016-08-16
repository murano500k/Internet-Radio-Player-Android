package com.android.murano500k.newradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;


/**
 * Created by artem on 8/8/16.
 */
public class NotificationProvider implements ListenerRadio {
    private String stationName = "";
    private String singerName = "";
    private String songName = "";
    private int smallImage = R.drawable.default_art;
    private int artImage = R.drawable.default_art;
    private int playStatusImage = R.drawable.ic_play;
    private int status;

    private Context context;
    private NotificationManager mNotificationManager;
    private ServiceRadio serviceRadio;
    private static final int LOADING=0;
    private static final int PLAYING=1;
    private static final int WAITING=-1;
    private String text;
    private PlaylistManager playlistManager;


    public NotificationProvider(Context context, ServiceRadio serviceRadio) {
        this.context = context;
        mNotificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        playlistManager=new PlaylistManager(context);
        this.serviceRadio=serviceRadio;
    }

    private void buildNotification(boolean updateNotif) {
	        Intent intentPause = new Intent(Constants.INTENT.PLAYBACK.PAUSE);
	        Intent intentPlay = new Intent(Constants.INTENT.PLAYBACK.RESUME);
	        Intent intentPrev = new Intent(Constants.INTENT.PLAYBACK.PLAY_PREV);
	        Intent intentNext = new Intent(Constants.INTENT.PLAYBACK.PLAY_NEXT);
	        Intent intentOpenPlayer = new Intent(context, ActivityRadio.class);
	        intentOpenPlayer.setAction(Constants.INTENT.OPEN_APP);
	        Intent intentCancel = new Intent(Constants.INTENT.CLOSE_NOTIFICATION);

	        PendingIntent prevPending = PendingIntent.getService(context, 0, intentPrev, 0);
	        PendingIntent nextPending = PendingIntent.getService(context, 0, intentNext, 0);
	        PendingIntent playPending = PendingIntent.getService(context, 0, intentPlay, 0);
	        PendingIntent pausePending = PendingIntent.getService(context, 0, intentPause, 0);
	        PendingIntent openPending = PendingIntent.getActivity(context, 0, intentOpenPlayer, 0);
	        PendingIntent cancelPending = PendingIntent.getService(context, 0, intentCancel, 0);

	        RemoteViews mNotificationTemplate = new RemoteViews(context.getPackageName(), R.layout.notification);
	        Notification.Builder notificationBuilder = new Notification.Builder(context);
	    PendingIntent playbackPending;

	    int playbackResId;
	    if(playlistManager!=null) {
		    switch (status){
			    case LOADING:
				    playbackPending= pausePending;
				    playbackResId= R.drawable.ic_loading;
				    break;
			    case PLAYING:
				    playbackPending= pausePending;
				    playbackResId= R.drawable.ic_pause;
				    break;
			    case WAITING:
				    playbackPending= playPending;
				    playbackResId= R.drawable.ic_play;
				    break;
			    default:
				    playbackPending= playPending;
				    playbackResId= R.drawable.ic_play;
				    break;
		    }
		    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, playbackPending);
		    mNotificationTemplate.setImageViewResource(R.id.notification_play, playbackResId);

	        this.stationName = PlaylistManager.getNameFromUrl(playlistManager.getSelectedUrl());
	        artImage=PlaylistManager.getArt(stationName, context);
            mNotificationTemplate.setTextViewText(R.id.station_name, stationName);
            mNotificationTemplate.setImageViewResource(R.id.notification_prev, R.drawable.ic_prev);
            mNotificationTemplate.setImageViewResource(R.id.notification_next, R.drawable.ic_next);
            mNotificationTemplate.setImageViewResource(R.id.widget_image, artImage);
		    mNotificationTemplate.setOnClickPendingIntent(R.id.widget_image, openPending);
            mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev, prevPending);
            mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next, nextPending);
            Notification notification = notificationBuilder
                    .setSmallIcon(artImage)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setDeleteIntent(cancelPending)
                    .setContent(mNotificationTemplate)
                    .setUsesChronometer(true)
                    .build();
            //notification.flags = Notification.FLAG_ONGOING_EVENT;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                RemoteViews mExpandedView = new RemoteViews(context.getPackageName(), R.layout
                        .notification_expanded);
                mExpandedView.setTextViewText(R.id.notification_station_name, stationName);
                mExpandedView.setImageViewResource(R.id.notification_expanded_prev, R.drawable.ic_prev);
                mExpandedView.setImageViewResource(R.id.notification_expanded_next, R.drawable.ic_next);
                mExpandedView.setImageViewResource(R.id.notification_expanded_play, playbackResId);
                mExpandedView.setImageViewResource(R.id.widget_image, artImage);

	            mExpandedView.setOnClickPendingIntent(R.id.widget_image, openPending);
                mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_prev, prevPending);
                mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_next, nextPending);
                mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, playbackPending);
                notification.bigContentView = mExpandedView;
	            if (updateNotif) mNotificationManager.notify(Constants.NOTIFICATION_ID, notification);
	            else mNotificationManager.cancel(Constants.NOTIFICATION_ID);
            }
	    }

    }

	@Override
	public void onFinish() {

	}

	@Override
    public void onLoadingStarted(String url) {
        status=LOADING;
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
        status=PLAYING;
        text=null;
        singerName="";
        songName="";
        buildNotification(true);
    }

    @Override
    public void onPlaybackStopped(boolean updateNotification) {
        status=WAITING;
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
        status=PLAYING;
        buildNotification(true);
    }

    @Override
    public void onPlaybackError(boolean updateNotification) {

        status=WAITING;
        text=null;
        singerName="";
        songName="";
	    buildNotification(updateNotification);
    }



	@Override
    public void onStationSelected(String url) {
        //status=PLAYING;
        text=PlaylistManager.getNameFromUrl(playlistManager.getSelectedUrl());
        buildNotification(true);
    }

    @Override
    public void onSleepTimerStatusUpdate(String action, int seconds) {

    }


}
