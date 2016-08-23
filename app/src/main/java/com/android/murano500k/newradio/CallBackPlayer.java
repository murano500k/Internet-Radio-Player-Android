package com.android.murano500k.newradio;

import android.media.AudioTrack;

import com.android.murano500k.newradio.events.EventBufferUpdate;
import com.android.murano500k.newradio.events.EventError;
import com.android.murano500k.newradio.events.EventMetadata;
import com.android.murano500k.newradio.events.EventStarted;
import com.android.murano500k.newradio.events.EventStopped;
import com.spoledge.aacdecoder.PlayerCallback;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

/**
 * Created by artem on 8/18/16.
 */
public class CallBackPlayer implements PlayerCallback{

	private static final String TAG = "CallBackPlayer";

	public CallBackPlayer() {
	}

	@Override
	public void playerStarted() {
		EventBus.getDefault().post(new EventStarted());
	}

	@Override
	public void playerPCMFeedBuffer(boolean intermediate, int audioBufferSizeMs, int audioBufferCapacityMs) {
		EventBus.getDefault().post(new EventBufferUpdate(
				(audioBufferSizeMs * 100 / audioBufferCapacityMs),intermediate));
	}

	@Override
	public void playerStopped(int i) {
		EventBus.getDefault().post(new EventMetadata(null, null));
		EventBus.getDefault().post(new EventStopped());
	}

	@Override
	public void playerException(Throwable throwable) {
		EventBus.getDefault().post(new EventError(
				throwable.getMessage()+ "\n\t"+ Arrays.toString(throwable.getStackTrace())
		));
	}

	@Override
	public void playerMetadata(String s1, String s2) {
		if(s1!=null && s1.equals("StreamTitle")) {
			EventBus.getDefault().post(new EventMetadata(PlaylistManager.getArtistFromString(s2),PlaylistManager
					.getTrackFromString(s2)));
		}
	}

	@Override
	public void playerAudioTrackCreated(AudioTrack audioTrack) {

	}

}
