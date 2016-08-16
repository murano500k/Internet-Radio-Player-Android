package com.android.murano500k.newradio;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.vk.sdk.VKAccessToken;


public class ActivityStart extends Activity {

	private VKAccessToken accessToken;
	TextView textView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);


	}

}
