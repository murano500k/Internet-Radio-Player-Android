/*
package com.android.murano500k.newradio;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.murano500k.onlineradioplayer.R;
import com.android.murano500k.onlineradioplayer.RadioPlayerService;

*/
/**
 * Created by artem on 7/22/16.
 *//*

public class WidgetProviderRadio {//TODO UPDATE WIDGET CODE
    private static final String TAG = "WidgetProviderRadio";
    private Bitmap artImageId=null;
    public static final String SELECTED_STATION_TITLE= "SELECTED_STATION_TITLE";
    public static final String IS_PLAYING= "IS_PLAYING";
    String title;
    boolean isPlaying;
    private AppWidgetManager widgetManager;
    int[] widgetIds;
    private RemoteViews remoteViews;

    public WidgetProviderRadio() {


    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)){
            title = intent.getStringExtra(SELECTED_STATION_TITLE);
            isPlaying = intent.getBooleanExtra(IS_PLAYING, false);
        }
        super.onReceive(context, intent);

    }



    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        widgetManager=appWidgetManager;
        widgetIds=appWidgetIds;
        final int count = appWidgetIds.length;
        for (int i = 0; i < count; i++) {

            int widgetId = appWidgetIds[i];

            Intent intentPlayPause = new Intent(RadioPlayerService.NOTIFICATION_INTENT_PLAY_PAUSE);
            //Intent intentPlayPause = new Intent(RadioPlayerService.WIDGET_INTENT_PLAY_PAUSE);
            Intent intentPrev = new Intent(RadioPlayerService.NOTIFICATION_INTENT_PREV);
            Intent intentNext = new Intent(RadioPlayerService.NOTIFICATION_INTENT_NEXT);
            //Intent intentPrev = new Intent(RadioPlayerService.WIDGET_INTENT_PREV);
            //Intent intentNext = new Intent(RadioPlayerService.WIDGET_INTENT_NEXT);
            Intent intentOpenPlayer = new Intent(RadioPlayerService.NOTIFICATION_INTENT_OPEN_PLAYER);
            */
/*intentNext.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intentPrev.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intentPlayPause.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);*//*

            intentOpenPlayer.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);

            intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
            intentPrev.addCategory(Intent.CATEGORY_DEFAULT);
            intentNext.addCategory(Intent.CATEGORY_DEFAULT);
            intentOpenPlayer.addCategory(Intent.CATEGORY_DEFAULT);
            //PendingIntent prevPending = PendingIntent.getActivity(context, 0, intentPrev, 0);
            //PendingIntent nextPending = PendingIntent.getActivity(context,  0, intentNext, 0);
            //PendingIntent playPausePending = PendingIntent.getActivity(context,  0, intentNext, 0);
            PendingIntent playPausePending = PendingIntent.getService(context, 0, intentPlayPause, 0);
            PendingIntent nextPending = PendingIntent.getService(context, 0, intentNext, 0);
            PendingIntent prevPending = PendingIntent.getService(context, 0, intentPrev, 0);
            PendingIntent openPending = PendingIntent.getActivity(context, 0, intentOpenPlayer, 0);
            remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.simple_widget);

            if (artImageId == null){
                if(title!=null){
                    int art = RadioPlayerService.getArt(title, context);
                    if(art!=0)artImageId = BitmapFactory.decodeResource(context.getResources(), art);
                }
                else  artImageId=BitmapFactory.decodeResource(context.getResources(),R.drawable.default_art);

            }
            Log.d(TAG, "title: "+title+" isPlaying: "+ isPlaying+" ");
            if(title==null) title="";

            remoteViews.setTextViewText(R.id.title_playing, title);
            remoteViews.setImageViewResource(R.id.btn_widget_prev, R.drawable.ic_prev);
            remoteViews.setImageViewResource(R.id.btn_widget_next, R.drawable.ic_next);

            remoteViews.setImageViewResource(R.id.btn_widget_play, isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
            remoteViews.setImageViewBitmap(R.id.widget_image, artImageId);

            remoteViews.setOnClickPendingIntent(R.id.btn_widget_prev, prevPending);
            remoteViews.setOnClickPendingIntent(R.id.btn_widget_next, nextPending);
            remoteViews.setOnClickPendingIntent(R.id.btn_widget_play, playPausePending);
            remoteViews.setOnClickPendingIntent(R.id.widget_image, openPending);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);

        }
    }
}
*/
