package com.android.murano500k.newradio;

import android.app.Application;

/**
 * Created by artem on 8/13/16.
 */

public class RadioApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		//FD:CC:9C:3D:A8:C7:3F:8B:92:4E:4A:2F:4C:20:61:A7:58:C6:7B:62
		/*String[] fingerprints = VKUtil.getCertificateFingerprint(this, this.getPackageName());
		Log.d("VK", fingerprints.toString());
		vkAccessTokenTracker.startTracking();

		VKSdk.initialize(this.getApplicationContext());*/

	}/*
	VKAccessTokenTracker vkAccessTokenTracker = new VKAccessTokenTracker() {
		@Override
		public void onVKAccessTokenChanged(VKAccessToken oldToken, VKAccessToken newToken) {
			if (newToken == null) {
				Intent intent=new Intent(getApplicationContext(), ActivityStart.class);
				intent.putExtra(Constants.VK.TOKEN_STATUS, false);
				startActivity(intent);
			}
			else {

				Toast.makeText(getApplicationContext(), oldToken+ " Token changed to "+newToken, Toast.LENGTH_SHORT).show();

			}
		}
	};
*/
}
