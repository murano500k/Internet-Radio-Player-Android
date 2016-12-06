 /*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stc.radio.player;

 import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
 import android.content.pm.PackageInfo;
 import android.content.pm.PackageManager;
 import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouter;

 import com.activeandroid.ActiveAndroid;
 import com.activeandroid.query.Delete;
 import com.activeandroid.query.From;
 import com.stc.radio.player.db.DBMediaItem;
 import com.stc.radio.player.db.DBUserPrefsItem;
 import com.stc.radio.player.model.MusicProvider;
import com.stc.radio.player.playback.MyLocalPlayback;
import com.stc.radio.player.playback.PlaybackManager;
import com.stc.radio.player.playback.QueueManager;
import com.stc.radio.player.ui.MusicPlayerActivity;
import com.stc.radio.player.utils.CarHelper;
import com.stc.radio.player.utils.LogHelper;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_ROOT;



 /**
  * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
  * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
  * exposes it through its MediaSession.Token, which allows the client to create a MediaController
  * that connects to and send control commands to the MediaSession remotely. This is useful for
  * user interfaces that need to interact with your media session, like Android Auto. You can
  * (should) also use the same service from your app's UI, which gives a seamless playback
  * experience to the user.
  *
  * To implement a MediaBrowserService, you need to:
  *
  * <ul>
  *
  * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
  *      related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
  *      {@link android.service.media.MediaBrowserService#onLoadChildren};
  * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
  *      with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
  *
  * <li> Set a callback on the
  *      {@link android.media.session.MediaSession#setCallback(android.media.session.MediaSession.Callback)}.
  *      The callback will receive all the user's actions, like play, pause, etc;
  *
  * <li> Handle all the actual music playing using any method your app prefers (for example,
  *      {@link android.media.MediaPlayer})
  *
  * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
  *      {@link android.media.session.MediaSession#setPlaybackState(android.media.session.PlaybackState)}
  *      {@link android.media.session.MediaSession#setMetadata(android.media.MediaMetadata)} and
  *      {@link android.media.session.MediaSession#setQueue(List)})
  *
  * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
  *      android.media.browse.MediaBrowserService
  *
  * </ul>
  *
  * To make your app compatible with Android Auto, you also need to:
  *
  * <ul>
  *
  * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
  *      with a &lt;automotiveApp&gt; root element. For a media app, this must include
  *      an &lt;uses name="media"/&gt; element as a child.
  *      For example, in AndroidManifest.xml:
  *          &lt;meta-data android:name="com.google.android.gms.car.application"
  *              android:resource="@xml/automotive_app_desc"/&gt;
  *      And in res/values/automotive_app_desc.xml:
  *          &lt;automotiveApp&gt;
  *              &lt;uses name="media"/&gt;
  *          &lt;/automotiveApp&gt;
  *
  * </ul>

  * @see <a href="README.md">README.md</a> for more details.
  *
  */
 public class MusicService extends MediaBrowserServiceCompat implements
         PlaybackManager.PlaybackServiceCallback {

     private static final String TAG = LogHelper.makeLogTag(MusicService.class);

     // Extra on MediaSession that contains the Cast device name currently connected to
     public static final String EXTRA_CONNECTED_CAST = "com.example.android.uamp.CAST_NAME";
     // The action of the incoming Intent indicating that it contains a command
     // to be executed (see {@link #onStartCommand})
     public static final String ACTION_CMD = "com.example.android.uamp.ACTION_CMD";
     // The key in the extras of the incoming Intent indicating the command that
     // should be executed (see {@link #onStartCommand})
     public static final String CMD_NAME = "CMD_NAME";
     // A value of a CMD_NAME key in the extras of the incoming Intent that
     // indicates that the music playback should be paused (see {@link #onStartCommand})
     public static final String CMD_PAUSE = "CMD_PAUSE";
     // A value of a CMD_NAME key that indicates that the music playback should switch
     // to local playback from cast playback.
     public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";
     // Delay stopSelf by using a handler.
     private static final int STOP_DELAY = 30000;

     private MusicProvider mMusicProvider;
     private PlaybackManager mPlaybackManager;

     private MediaSessionCompat mSession;
     private MediaNotificationManager mMediaNotificationManager;
     private Bundle mSessionExtras;
     private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
     private MediaRouter mMediaRouter;
     private PackageValidator mPackageValidator;
     private boolean mIsConnectedToCar;
     private BroadcastReceiver mCarConnectionReceiver;

	 public void checkDBVersion(){
		 if(getVersionCode()<6) {
			 ActiveAndroid.beginTransaction();
			 try {
				 From from=new Delete().from(DBMediaItem.class);
				 if(from.exists())from.execute();
				 from=new Delete().from(DBUserPrefsItem.class);
				 if(from.exists())from.execute();
			 }catch (Exception e){

			 }
			 finally {
				 ActiveAndroid.endTransaction();
			 }
		 }

	 }

	 private String getAppVersion(){
		 try {
			 PackageInfo _info = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
			 return _info.versionName;
		 } catch (PackageManager.NameNotFoundException e) {
			 e.printStackTrace();
			 return "";
		 }
	 }

	 private int getVersionCode(){
		 try {
			 PackageInfo _info = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
			 return _info.versionCode;
		 } catch (PackageManager.NameNotFoundException e) {
			 e.printStackTrace();
			 return -1;
		 }
	 }
     /*
      * (non-Javadoc)
      * @see android.app.Service#onCreate()
      */
     @Override
     public void onCreate() {
         super.onCreate();
	     checkDBVersion();
         LogHelper.d(TAG, "onCreate");

         mMusicProvider = new MusicProvider();

         // To make the app more responsive, fetch and cache catalog information now.
         // This can help improve the response time in the method
         // {@link #onLoadChildren(String, Result<List<MediaItem>>) onLoadChildren()}.
         mMusicProvider.retrieveMediaAsync(null /* Callback */);

         mPackageValidator = new PackageValidator(this);

         QueueManager queueManager = new QueueManager(mMusicProvider, getResources(),
                 new QueueManager.MetadataUpdateListener() {
                     @Override
                     public void onMetadataChanged(MediaMetadataCompat metadata) {
                         mSession.setMetadata(metadata);
                     }

                     @Override
                     public void onMetadataRetrieveError() {
                         mPlaybackManager.updatePlaybackState(
                                 getString(R.string.error_no_metadata));
                     }

                     @Override
                     public void onCurrentQueueIndexUpdated(int queueIndex) {
                         mPlaybackManager.handlePlayRequest();
                     }

                     @Override
                     public void onQueueUpdated(String title,
                                                List<MediaSessionCompat.QueueItem> newQueue) {
                         mSession.setQueue(newQueue);
                         mSession.setQueueTitle(title);
                     }
                 });



         MyLocalPlayback playback = new MyLocalPlayback(this, mMusicProvider);
         //LocalPlayback playback = new LocalPlayback(this, mMusicProvider);
         mPlaybackManager = new PlaybackManager(this, getResources(), mMusicProvider, queueManager,
                 playback);

         // Start a new MediaSession
         mSession = new MediaSessionCompat(this, "MusicService");
         setSessionToken(mSession.getSessionToken());
         mSession.setCallback(mPlaybackManager.getMediaSessionCallback());
         mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                 MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

         Context context = getApplicationContext();
         Intent intent = new Intent(context, MusicPlayerActivity.class);
         PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                 intent, PendingIntent.FLAG_UPDATE_CURRENT);
         mSession.setSessionActivity(pi);

         mSessionExtras = new Bundle();
         CarHelper.setSlotReservationFlags(mSessionExtras, true, true, true);
         //WearHelper.setSlotReservationFlags(mSessionExtras, true, true);
         //WearHelper.setUseBackgroundFromTheme(mSessionExtras, true);
         mSession.setExtras(mSessionExtras);

         mPlaybackManager.updatePlaybackState(null);

         try {
             mMediaNotificationManager = new MediaNotificationManager(this);
         } catch (RemoteException e) {
             throw new IllegalStateException("Could not create a MediaNotificationManager", e);
         }



         mMediaRouter = MediaRouter.getInstance(getApplicationContext());

         registerCarConnectionReceiver();
     }

     /**
      * (non-Javadoc)
      * @see android.app.Service#onStartCommand(Intent, int, int)
      */
     @Override
     public int onStartCommand(Intent startIntent, int flags, int startId) {
         if (startIntent != null) {
             String action = startIntent.getAction();
             String command = startIntent.getStringExtra(CMD_NAME);
             if (ACTION_CMD.equals(action)) {
                 if (CMD_PAUSE.equals(command)) {
                     mPlaybackManager.handlePauseRequest();
                 }
             } else {
                 // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                 MediaButtonReceiver.handleIntent(mSession, startIntent);
             }
         }
         // Reset the delay handler to enqueue a message to stop the service if
         // nothing is playing.
         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
         return START_STICKY;
     }

     /**
      * (non-Javadoc)
      * @see android.app.Service#onDestroy()
      */
     @Override
     public void onDestroy() {
         LogHelper.d(TAG, "onDestroy");
         unregisterCarConnectionReceiver();
         // Service is being killed, so make sure we release our resources
         mPlaybackManager.handleStopRequest(null);
         mMediaNotificationManager.stopNotification();



         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mSession.release();
     }


     @Override
     public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                  Bundle rootHints) {
         LogHelper.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName,
                 "; clientUid=" + clientUid + " ; rootHints=", rootHints);
         if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
             // If the request comes from an untrusted package, return null. No further calls will
             // be made to other media browsing methods.
             LogHelper.w(TAG, "OnGetRoot: IGNORING request from untrusted package "
                     + clientPackageName);
             return null;
         }

         return new BrowserRoot(MEDIA_ID_ROOT, null);
     }

     @Override
     public void onLoadChildren(@NonNull final String parentMediaId,
                                @NonNull final Result<List<MediaItem>> result) {
         LogHelper.d(TAG, "OnLoadChildren: parentMediaId=", parentMediaId);
         if (mMusicProvider.isInitialized()) {
             // if music library is ready, return immediately
             result.sendResult(mMusicProvider.getChildren(parentMediaId));
         } else {
             result.detach();

             mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                 @Override
                 public void onMusicCatalogReady(boolean success) {
                     result.sendResult(mMusicProvider.getChildren(parentMediaId));
                 }
             });
         }
     }

	 @Override
	 public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result, @NonNull Bundle options) {

		 super.onLoadChildren(parentId, result, options);
	 }

	 @Override
	 public void onLoadItem(String itemId, Result<MediaItem> result) {


	 }

	 @Override
     public void onPlaybackStart() {
         if (!mSession.isActive()) {
             mSession.setActive(true);
         }

         mDelayedStopHandler.removeCallbacksAndMessages(null);

         // The service needs to continue running even after the bound client (usually a
         // MediaController) disconnects, otherwise the music playback will stop.
         // Calling startService(Intent) will keep the service running until it is explicitly killed.
         startService(new Intent(getApplicationContext(), MusicService.class));
     }


     /**
      * Callback method called from PlaybackManager whenever the music stops playing.
      */
     @Override
     public void onPlaybackStop() {
         // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
         // potentially stopping the service.
         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
         stopForeground(true);
     }

     @Override
     public void onNotificationRequired() {
         mMediaNotificationManager.startNotification();
     }

     @Override
     public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
         mSession.setPlaybackState(newState);
     }

     private void registerCarConnectionReceiver() {
         IntentFilter filter = new IntentFilter(CarHelper.ACTION_MEDIA_STATUS);
         mCarConnectionReceiver = new BroadcastReceiver() {
             @Override
             public void onReceive(Context context, Intent intent) {
                 String connectionEvent = intent.getStringExtra(CarHelper.MEDIA_CONNECTION_STATUS);
                 mIsConnectedToCar = CarHelper.MEDIA_CONNECTED.equals(connectionEvent);
                 LogHelper.i(TAG, "Connection event to Android Auto: ", connectionEvent,
                         " isConnectedToCar=", mIsConnectedToCar);
             }
         };
         registerReceiver(mCarConnectionReceiver, filter);
     }

     private void unregisterCarConnectionReceiver() {
         unregisterReceiver(mCarConnectionReceiver);
     }

     /**
      * A simple handler that stops the service if playback is not active (playing)
      */
     private static class DelayedStopHandler extends Handler {
         private final WeakReference<MusicService> mWeakReference;

         private DelayedStopHandler(MusicService service) {
             mWeakReference = new WeakReference<>(service);
         }

         @Override
         public void handleMessage(Message msg) {
             MusicService service = mWeakReference.get();
             if (service != null && service.mPlaybackManager.getPlayback() != null) {
                 if (service.mPlaybackManager.getPlayback().isPlaying()) {
                     LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                     return;
                 }
                 LogHelper.d(TAG, "Stopping service with delay handler.");
                 service.stopSelf();
             }
         }
     }

 }
