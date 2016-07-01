package com.android.murano500k.onlineradioplayer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by artem on 6/29/16.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";
    ConnectivityManager cm;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        boolean isOnline=isOnline(context);
        Log.d(TAG, "NetworkChangeReceiver onReceive. isOnline: "+ isOnline );
        Intent intent1 = new Intent();
        if(isOnline) {
            intent1.setAction(RadioPlayerService.CONNECT_INTENT_CONNECTED);
        } else {
            intent1.setAction(RadioPlayerService.CONNECT_INTENT_DISCONNECTED);
        }
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent1.addCategory(Intent.CATEGORY_DEFAULT);
        //context.startActivity(intent);
        PendingIntent pending = PendingIntent.getService(context, 0, intent1, 0);
        try {
            pending.send();
        } catch (PendingIntent.CanceledException e) {
            Log.d(TAG, "PendingIntent.CanceledException e: "+ e.getMessage() );
            e.printStackTrace();
        }

    }
    public boolean isOnline(Context context) {
        if (cm == null) cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }
}