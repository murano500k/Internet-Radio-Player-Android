package com.android.murano500k.onlineradioplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by artem on 6/29/16.
 */
public class SleepTimerEventReceiver extends BroadcastReceiver {
    public static final String SLEEP_TIMER_EVENT = "com.android.murano500k.onlineradioplayer.SLEEP_TIMER_EVENT";

    public static final String EVENT_ACTION = "com.android.murano500k.onlineradioplayer.EVENT_ACTION";

    public static final String LEFT_SECONDS = "com.android.murano500k.onlineradioplayer.LEFT_SECONDS";
    public static final String TIMER_UPDATE = "com.android.murano500k.onlineradioplayer.TIMER_UPDATE";
    public static final String TIMER_FINISH = "com.android.murano500k.onlineradioplayer.TIMER_FINISH";
    public static final String TIMER_CANCELLED = "com.android.murano500k.onlineradioplayer.TIMER_CANCELLED";

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
