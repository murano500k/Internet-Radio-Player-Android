package com.android.murano500k.newradio;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ActivityTemp extends Activity {
TextView textView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_temp);
		textView=(TextView) findViewById(R.id.text_log);
		PlaylistManager playlistManager=new PlaylistManager(getApplicationContext());
		textView.setText(PlaylistManager.getLog());

	}

}
