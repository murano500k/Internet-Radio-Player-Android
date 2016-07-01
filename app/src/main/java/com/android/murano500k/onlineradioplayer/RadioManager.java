package com.android.murano500k.onlineradioplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.spoledge.aacdecoder.MultiPlayer;

import java.util.ArrayList;
import java.util.List;


public class RadioManager implements IRadioManager {

    private static final String TAG = "RadioManager";

    private static boolean isLogging = false;

    private static RadioManager instance = null;

    private static RadioPlayerService mService;

    private Context mContext;

    private List<RadioListener> mRadioListenerQueue;

    private boolean isServiceConnected;
    private StationContent.Station currentStation;
    public static boolean needResume;


    public  StationContent.Station getCurrentStation() {
        return currentStation;
    }

    public  void setCurrentStation(StationContent.Station currentStation) {
        this.currentStation = currentStation;
    }




    private RadioManager(Context mContext) {
        this.mContext = mContext;
        mRadioListenerQueue = new ArrayList<>();
        isServiceConnected = false;

    }


    public static RadioManager with(Context mContext) {
        if (instance == null){
            needResume=false;
            instance = new RadioManager(mContext);
        }

        return instance;
    }


    public static RadioPlayerService getService(){
        return mService;
    }

    public MultiPlayer getPlayer(){
        return mService.getPlayer();
    }

    @Override
    public void startRadio(String streamURL) {
        mService.play(streamURL);
        needResume=true;
    }

    @Override
    public void startRadio() {
        if(currentStation!=null && isServiceConnected){
            mService.play(currentStation.url);
            needResume=true;
        }else if(!isServiceConnected) Toast.makeText(mContext, "Service not connected", Toast.LENGTH_SHORT).show();
        else Toast.makeText(mContext, "Station not selected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void stopRadio() {
        if(isServiceConnected){
            mService.stop();
            needResume=false;
        }else  Toast.makeText(mContext, "Service not connected", Toast.LENGTH_SHORT).show();

    }


    @Override
    public boolean isPlaying() {
        if(mService!=null && isServiceConnected){
            log("IsPlaying : " + mService.isPlaying());
            return mService.isPlaying();
        }else {
            return false;
        }
    }
    @Override
    public void registerListener(RadioListener mRadioListener) {
        if (isServiceConnected)
            mService.registerListener(mRadioListener);
        else
            mRadioListenerQueue.add(mRadioListener);
    }

    @Override
    public void setClosedFromNotification(boolean isClosedFromNotification){
        if (isServiceConnected)
            mService.setClosedFromNotification(isClosedFromNotification);

    }

    @Override
    public boolean isClosedFromNotification(){
        if (isServiceConnected)
            return mService.isClosedFromNotification();
        else return false;
    }


    @Override
    public void unregisterListener(RadioListener mRadioListener) {
        log("Register unregistered.");
        mService.unregisterListener(mRadioListener);
    }

    @Override
    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    @Override
    public void connect() {
        log("Requested to connect service.");
        Intent intent = new Intent(mContext, RadioPlayerService.class);
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }



    @Override
    public void disconnect() {
        log("Service Disconnected.");
        if(mService != null && isServiceConnected)

            mContext.unbindService(mServiceConnection);
    }


    @Override
    public void updateNotification(String stationName, String singerName, String songName, int smallArt, int bigArt) {
        Log.d(TAG,"updateNotification" + (mService!=null));

        if(mService != null && isServiceConnected)
            mService.updateNotification(stationName, singerName, songName, smallArt, bigArt);
    }

    @Override
    public void updateNotification(String stationName, String singerName, String songName, int smallArt, Bitmap bigArt) {
        Log.d(TAG,"updateNotification" + (mService!=null));
        if(mService != null && isServiceConnected)
            mService.updateNotification(stationName, singerName, songName, smallArt, bigArt);
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {

            log("Service Connected.");

            mService = ((RadioPlayerService.LocalBinder) binder).getService();
            mService.setLogging(isLogging);
            isServiceConnected = true;
            mService.setActivity((MainActivity)mContext);

            if (!mRadioListenerQueue.isEmpty()) {
                for (RadioListener mRadioListener : mRadioListenerQueue) {
                    registerListener(mRadioListener);
                    mRadioListener.onRadioConnected();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceConnected=false;
        }
    };


    private void log(String log) {
        if (isLogging)
            Log.v("RadioManager", "RadioManagerLog : " + log);
    }


    public void stationSelected(StationContent.Station station) {
        if(currentStation==station) {
            /*if(isServiceConnected  && isPlaying())
            stopRadio();*/
        }else {
            currentStation=station;
            if(isServiceConnected) startRadio();
        }
    }
}
