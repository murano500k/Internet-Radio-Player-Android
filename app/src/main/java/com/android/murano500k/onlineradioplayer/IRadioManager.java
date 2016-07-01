package com.android.murano500k.onlineradioplayer;


import android.graphics.Bitmap;

/**
 * Created by mertsimsek on 03/07/15.
 */
public interface IRadioManager {

    void startRadio(String streamURL);
    void startRadio();

    void stopRadio();

    boolean isPlaying();

    void registerListener(RadioListener mRadioListener);

    void setClosedFromNotification(boolean isClosedFromNotification);

    boolean isClosedFromNotification();


    void unregisterListener(RadioListener mRadioListener);

    void setLogging(boolean logging);

    void connect();

    void disconnect();



    void updateNotification(String stationName, String singerName, String songName, int smallArt, int bigArt);

    void updateNotification(String stationName, String singerName, String songName, int smallArt, Bitmap bigArt);
}
