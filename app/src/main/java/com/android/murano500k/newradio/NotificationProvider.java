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

    private void buildNotification() {
        if(playlistManager!=null) {
            Intent intentPlayPause = new Intent(Constants.INTENT_PLAY_PAUSE);
            Intent intentPrev = new Intent(Constants.INTENT_PLAY_PREV);
            Intent intentNext = new Intent(Constants.INTENT_PLAY_NEXT);
            Intent intentOpenPlayer = new Intent(Constants.INTENT_OPEN_APP);
            intentOpenPlayer.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Intent intentCancel = new Intent(Constants.INTENT_CLOSE_NOTIFICATION);

            PendingIntent prevPending = PendingIntent.getService(context, 0, intentPrev, 0);
            PendingIntent nextPending = PendingIntent.getService(context, 0, intentNext, 0);
            PendingIntent playPausePending = PendingIntent.getService(context, 0, intentPlayPause, 0);
            PendingIntent openPending = PendingIntent.getActivity(context, 0, intentOpenPlayer, 0);
            PendingIntent cancelPending = PendingIntent.getService(context, 0, intentCancel, 0);

            RemoteViews mNotificationTemplate = new RemoteViews(context.getPackageName(), R.layout.notification);
            Notification.Builder notificationBuilder = new Notification.Builder(context);

            this.stationName = PlaylistManager.getNameFromUrl(playlistManager.getSelectedUrl());
            artImage=PlaylistManager.getArt(stationName, context);
            switch (status){
                case LOADING:
                    playStatusImage= R.drawable.ic_loading;
                    break;
                case PLAYING:
                    playStatusImage= R.drawable.ic_pause;
                    break;
                case WAITING:
                    playStatusImage= R.drawable.ic_play;
                    break;
                default:
                    playStatusImage= R.drawable.ic_play;
                    break;
            }
            if(text==null){
                if(stationName==null) text="";
                else text=stationName;
            }
            mNotificationTemplate.setTextViewText(R.id.station_name, text);
            mNotificationTemplate.setImageViewResource(R.id.notification_prev, R.drawable.ic_prev);
            mNotificationTemplate.setImageViewResource(R.id.notification_next, R.drawable.ic_next);
            mNotificationTemplate.setImageViewResource(R.id.widget_image, artImage);
            mNotificationTemplate.setImageViewResource(R.id.notification_play, playStatusImage);
            mNotificationTemplate.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
            mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev, prevPending);
            mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next, nextPending);
            mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, playPausePending);
            Notification notification = notificationBuilder
                    .setSmallIcon(artImage)
                    .setContentIntent(openPending)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setDeleteIntent(cancelPending)
                    .setContent(mNotificationTemplate)
                    .setUsesChronometer(true)
                    .build();
            notification.flags = Notification.FLAG_ONGOING_EVENT;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                RemoteViews mExpandedView = new RemoteViews(context.getPackageName(), R.layout
                        .notification_expanded);
                mExpandedView.setTextViewText(R.id.notification_station_name, stationName);
                mExpandedView.setTextViewText(R.id.notification_artist, singerName);
                mExpandedView.setTextViewText(R.id.notification_song, songName);
                mExpandedView.setImageViewResource(R.id.notification_expanded_prev, R.drawable.ic_prev);
                mExpandedView.setImageViewResource(R.id.notification_expanded_next, R.drawable.ic_next);
                mExpandedView.setImageViewResource(R.id.notification_expanded_play, playStatusImage);
                mExpandedView.setImageViewResource(R.id.widget_image, artImage);
                mExpandedView.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
                mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_prev, prevPending);
                mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_next, nextPending);
                mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, playPausePending);
                notification.bigContentView = mExpandedView;
            }

            if (!serviceRadio.isClosedFromNotification())
                mNotificationManager.notify(Constants.NOTIFICATION_ID, notification);
            else mNotificationManager.cancel(Constants.NOTIFICATION_ID);
        }
    }
    @Override
    public void onLoadingStarted(String url) {
        status=LOADING;
        text=null;
        singerName="";
        songName="";
        buildNotification();
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
        buildNotification();
    }

    @Override
    public void onPlaybackStopped(boolean updateNotification) {
        status=WAITING;
        text=null;
        singerName="";
        songName="";
        if(updateNotification) buildNotification();
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
        buildNotification();
    }

    @Override
    public void onPlaybackError() {
        status=WAITING;
        text=null;
        singerName="";
        songName="";
        buildNotification();
    }

    @Override
    public void onListChanged() {

    }

    @Override
    public void onStationSelected(String url) {
        //status=PLAYING;
        text=PlaylistManager.getNameFromUrl(playlistManager.getSelectedUrl());
        buildNotification();
    }

    @Override
    public void onSleepTimerStatusUpdate(String action, int seconds) {

    }
    /*private void updateNotification(String singerName, String songName, int smallImage,
                                    Bitmap artImage) {
       if(serviceRadio!=null) {
            Intent intentPlayPause = new Intent(Constants.INTENT_PLAY_PAUSE);
            Intent intentPrev = new Intent(Constants.INTENT_PLAY_PREV);
            Intent intentNext = new Intent(Constants.INTENT_PLAY_NEXT);
            Intent intentOpenPlayer = new Intent(Constants.INTENT_OPEN_APP);
            intentOpenPlayer.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Intent intentCancel = new Intent(Constants.INTENT_CLOSE_NOTIFICATION);


    PendingIntent prevPending = PendingIntent.getService(context, 0, intentPrev, 0);
    PendingIntent nextPending = PendingIntent.getService(context, 0, intentNext, 0);
    PendingIntent playPausePending = PendingIntent.getService(context, 0, intentPlayPause, 0);
    PendingIntent openPending = PendingIntent.getActivity(context, 0, intentOpenPlayer, 0);
    PendingIntent cancelPending = PendingIntent.getService(context, 0, intentCancel, 0);

    RemoteViews mNotificationTemplate = new RemoteViews(context.getPackageName(), R.layout.notification);
    Notification.Builder notificationBuilder = new Notification.Builder(context);

    if (artImage == null){
        artImage = BitmapFactory.decodeResource(context.getResources(),
                StationContent.getArt(serviceRadio.getCurrentStationName(),context));
    }
    mNotificationTemplate.setTextViewText(R.id.notification_station_name, stationName);
    mNotificationTemplate.setImageViewResource(R.id.notification_prev, R.drawable.ic_prev);
    mNotificationTemplate.setImageViewResource(R.id.notification_next, R.drawable.ic_next);
    mNotificationTemplate.setImageViewResource(R.id.notification_play,
            serviceRadio.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    mNotificationTemplate.setImageViewBitmap(R.id.widget_image, artImage);

    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev, prevPending);
    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next, nextPending);
    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, playPausePending);

    Notification notification = notificationBuilder
            .setSmallIcon(smallImage)
            .setContentIntent(openPending)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setContent(mNotificationTemplate)
            .setUsesChronometer(true)
            .build();
    notification.flags = Notification.FLAG_ONGOING_EVENT;


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        RemoteViews mExpandedView = new RemoteViews(context.getPackageName(), R.layout
                .notification_expanded);
        mExpandedView.setTextViewText(R.id.notification_station_name, stationName);
        mExpandedView.setTextViewText(R.id.notification_line_one, singerName);
        mExpandedView.setTextViewText(R.id.notification_line_two, songName);
        mExpandedView.setImageViewResource(R.id.notification_expanded_prev, R.drawable.ic_prev);
        mExpandedView.setImageViewResource(R.id.notification_expanded_next, R.drawable.ic_next);
        mExpandedView.setImageViewResource(R.id.notification_expanded_play,
                serviceRadio.isPlaying() ? R.drawable.ic_pause
                        : R.drawable.ic_play);
        mExpandedView.setImageViewBitmap(R.id.widget_image, artImage);
        mExpandedView.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_prev, prevPending);
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_next, nextPending);
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, playPausePending);
        notification.bigContentView = mExpandedView;
    }

    if (mNotificationManager != null && !isClosedFromNotification)
            mNotificationManager.notify(Constants.NOTIFICATION_ID, notification);
}
    }*/
}
