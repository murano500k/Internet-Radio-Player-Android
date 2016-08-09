// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.speech.levelmeter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * This is a simple class that encapsulates the audio input and provides a
 *  callback interface for real-time audio processing.
 * 
 * This method is set up to provide 50 frames per second (i.e. 20 ms frames)
 * independent of sampling rate.
 * 
 * To use it
 * 1) implement the MicrophoneInputListener interface e.g.
 *     class MyAwesomeClass implements MicrophoneInputListener {...}
 * 2) Create the object, e.g. 
 *     micInput = new MicrophoneInput(this);
 * 3) Implement processAudioFrame in your MyAwesomeClass
 *     public void processAudioFrame(short[] audioFrame) {...}.
 *     
 * An example is provided in LevelMeterActivity.
 * 
 * Audio capture runs in a separate thread which is set up when start() is
 * called and destroyed when stop() is called.
 * 
 * @author Trausti Kristjansson
 *
 */
public class MicrophoneInput implements Runnable{
  int mSampleRate = 8000;
  int mAudioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
  final int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
  final int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;

  private final MicrophoneInputListener mListener;
  private Thread mThread;
  private boolean mRunning;

  AudioRecord recorder;

  int mTotalSamples = 0;

  private static final String TAG = "MicrophoneInput"; 

  public MicrophoneInput(MicrophoneInputListener listener) {
    mListener = listener;
  }
  
  public void setSampleRate(int sampleRate) {
    mSampleRate = sampleRate;
  }
  
  public void setAudioSource(int audioSource) {
    mAudioSource = audioSource;
  }

  public void start() {
    if (false == mRunning) {
      mRunning = true;
      mThread = new Thread(this);
      mThread.start();
    }
  }

  public void stop() {
    try {
      if (mRunning) {
        mRunning = false;
        mThread.join();
      }
    } catch (InterruptedException e) {
      Log.v(TAG, "InterruptedException.", e);
    }  
  }

  @Override
  public void run() {
    // Buffer for 20 milliseconds of data, e.g. 160 samples at 8kHz.
    short[] buffer20ms = new short[mSampleRate / 50];
    // Buffer size of AudioRecord buffer, which will be at least 1 second.
    int buffer1000msSize = bufferSize(mSampleRate, mChannelConfig,
        mAudioFormat);

    try {
      recorder = new AudioRecord(
          mAudioSource,
          mSampleRate,
          mChannelConfig,
          mAudioFormat,
          buffer1000msSize);
      recorder.startRecording();

      while (mRunning) {      
        int numSamples = recorder.read(buffer20ms, 0, buffer20ms.length);        
        mTotalSamples += numSamples;
        mListener.processAudioFrame(buffer20ms);
      }
      recorder.stop();
    } catch(Throwable x) {
      Log.v(TAG, "Error reading audio", x);
    } finally {
    }   
  }

  public int totalSamples() {
    return mTotalSamples;
  }

  public void setTotalSamples(int totalSamples) {
    mTotalSamples = totalSamples;
  }
  
  /**
   * Helper method to find a buffer size for AudioRecord which will be at
   * least 1 second.
   * 
   * @param sampleRateInHz the sample rate expressed in Hertz.
   * @param channelConfig describes the configuration of the audio channels.
   * @param audioFormat the format in which the audio data is represented. 
   * @return buffSize the size of the audio record input buffer.
   */
  private int bufferSize(int sampleRateInHz, int channelConfig,
      int audioFormat) {
    int buffSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig,
        audioFormat);
    if (buffSize < sampleRateInHz) {
      buffSize = sampleRateInHz;
    }
    return buffSize;
  }
}
