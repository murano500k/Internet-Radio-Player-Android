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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.android.murano500k.newradio.R;

/**
 * This is an example of a simple settings pane with persistence of settings.
 * 
 * This class handles the UI interaction for the settings pane.
 * It provides a simple drop-down menu to set the sampling rate.
 * It uses Bundles to persist (i.e. store) the setting between sessions.
 *  
 * @author trausti@google.com (Trausti Kristjansson)
 *
 */
public class Settings extends Activity {

  private int mSampleRate;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    readPreferences();

    /**
     * Drop down selection of sample rate.
     */
    Spinner spinner = (Spinner) findViewById(R.id.sampleRateSpinner);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this, R.array.sample_rate_array, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

    spinner.setPrompt(Integer.toString(mSampleRate));
    int spinnerPosition = adapter.getPosition(Integer.toString(mSampleRate));

    spinner.setSelection(spinnerPosition);

    /** 
     * Ok button dismiss settings.
     */
    Button okButton=(Button)findViewById(R.id.settingsOkButton);
    Button.OnClickListener okBtnListener = 
        new Button.OnClickListener() {

      @Override
      public void onClick(View v) {
        // Dismiss this dialog.
        Settings.this.setPreferences();
        finish();

      }
    };
    okButton.setOnClickListener(okBtnListener);
  }

  public class MyOnItemSelectedListener implements OnItemSelectedListener {

    @Override
    public void onItemSelected(AdapterView<?> parent,
        View view, int pos, long id) {
      Settings.this.mSampleRate =
          Integer.parseInt(parent.getItemAtPosition(pos).toString());
    }

    @Override
    public void onNothingSelected(AdapterView parent) {
      // Do nothing.
    }
  }

  private void readPreferences() {
    SharedPreferences preferences = getSharedPreferences("LevelMeter",
        MODE_PRIVATE);
    mSampleRate = preferences.getInt("SampleRate", 8000);
  }

  private void setPreferences() {
    SharedPreferences preferences = getSharedPreferences("LevelMeter",
        MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();

    editor.putInt("SampleRate", mSampleRate);
    editor.commit();
  }
}