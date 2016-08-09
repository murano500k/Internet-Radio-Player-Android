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

/**
 * This is the interface to the MicrophoneInput class for wrapping
 * the audio input.
 * 
 * Implement this interface to get real-time audio frames of PCM samples.
 * 
 * @author Trausti Kristjansson
 *
 */
public interface MicrophoneInputListener {
  /**
   * processAudioFrame gets called periodically, e.g. every 20ms with PCM
   * audio samples.
   * 
   * @param audioFrame this is an array of samples, e.g. if the sampling rate
   * is 8000 samples per second, then the array should contain 160 16 bit
   * samples.
   */
  public void processAudioFrame(short[] audioFrame);
}
