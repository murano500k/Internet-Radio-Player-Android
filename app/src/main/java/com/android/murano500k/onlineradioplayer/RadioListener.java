package com.android.murano500k.onlineradioplayer;

/**
 * Created by mertsimsek on 01/07/15.
 */
public interface RadioListener {

  void onRadioLoading();

  void onRadioConnected();

  void onRadioStarted();

  void onRadioStopped(boolean updateNotification);

  void onMetaDataReceived(String s, String s2);

  void onError();

  void onNextSongShouldPlay();
  void onCurrentSongShouldPlay();

  void onPrevSongShouldPlay();

  void onSleepTimerStatusUpdate(String action, int seconds);

}
