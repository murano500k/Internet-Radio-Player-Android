package com.android.murano500k.newradio;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.android.murano500k.newradio.rest.RadioApiService;
import com.android.murano500k.newradio.rest.ResponceRegister;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by artem on 9/9/16.
 */

@RunWith(AndroidJUnit4.class)
public class TestAPI extends TestBase {
	private static final String TAG = "TestAPI";
	String testStr= "#EXTM3U#EXTINF:0,Digitally Imported - Basslinehttp://pub2.diforfree.org:8000/di_bassline_hi#EXTINF:0,Digitally Imported - Big Beathttp://pub2.diforfree.org:8000/di_bigbeat_hi#EXTINF:0,Digitally Imported - Breakshttp://pub2.diforfree.org:8000/di_breaks_hi#EXTINF:0,Digitally Imported - ChillHophttp://pub2.diforfree.org:8000/di_chillhop_hi#EXTINF:0,Digitally Imported - Chillstephttp://pub2.diforfree.org:8000/di_chillstep_hi#EXTINF:0,Digitally Imported - Dark DnBhttp://pub2.diforfree.org:8000/di_darkdnb_hi#EXTINF:0,Digitally Imported - Deep Househttp://pub2.diforfree.org:8000/di_deephouse_hi#EXTINF:0,Digitally Imported - Deep Techhttp://pub2.diforfree.org:8000/di_deeptech_hi#EXTINF:0,Digitally Imported - Drum 'n Basshttp://pub2.diforfree.org:8000/di_drumandbass_hi#EXTINF:0,Digitally Imported - Dubhttp://pub2.diforfree.org:8000/di_dub_hi#EXTINF:0,Digitally Imported - Dub Technohttp://pub2.diforfree.org:8000/di_dubtechno_hi#EXTINF:0,Digitally Imported - Dubstephttp://pub2.diforfree.org:8000/di_dubstep_hi#EXTINF:0,Digitally Imported - Future Synthpophttp://pub2.diforfree.org:8000/di_futuresynthpop_hi#EXTINF:0,Digitally Imported - Junglehttp://pub2.diforfree.org:8000/di_jungle_hi#EXTINF:0,Digitally Imported - Latin Househttp://pub2.diforfree.org:8000/di_latinhouse_hi#EXTINF:0,Digitally Imported - Liquid DnBhttp://pub2.diforfree.org:8000/di_liquiddnb_hi#EXTINF:0,Digitally Imported - Minimalhttp://pub2.diforfree.org:8000/di_minimal_hi#EXTINF:0,Digitally Imported - Oldschool Acidhttp://pub2.diforfree.org:8000/di_oldschoolacid_hi#EXTINF:0,Digitally Imported - Oldschool Ravehttp://pub2.diforfree.org:8000/di_oldschoolrave_hi#EXTINF:0,Digitally Imported - Oldschool Techno & Trancehttp://pub2.diforfree.org:8000/di_classicelectronica_hi#EXTINF:0,Digitally Imported - Progressive Psyhttp://pub2.diforfree.org:8000/di_progressivepsy_hi#EXTINF:0,Digitally Imported - Underground Technohttp://pub2.diforfree.org:8000/di_undergroundtechno_hi#EXTINF:0,Digitally Imported - Vocal Trancehttp://pub2.diforfree.org:8000/di_vocaltrance_hi";

	public TestAPI() {
		super();
	}

	@Test
	public void testDownload(){
		String everything=null;
		ResponceRegister responceRegister=getResponceRegister();
		Log.w(TAG, "result = "+responceRegister);

		Log.w(TAG,responceRegister.getLastupd());
		Log.w(TAG,responceRegister.getEmail());
		Log.w(TAG,responceRegister.getPassword());
		Log.w(TAG,responceRegister.getTime());
		Log.w(TAG,responceRegister.getTimeExp());
		Log.w(TAG,responceRegister.getToken());
		Log.w(TAG,responceRegister.getChannels().toString());
		Log.w(TAG,responceRegister.getChannels().toString());


		assertNotNull(responceRegister);


	}
	public ResponceRegister getResponceRegister() {
		Gson gson = new GsonBuilder()
				.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
				.create();

		Retrofit retrofit = new Retrofit.Builder()
				.addConverterFactory(GsonConverterFactory.create(gson))
				.baseUrl("https://api.friezy.ru/")
				.build();


		RadioApiService apiService = retrofit.create(RadioApiService.class);
		try {
			retrofit2.Response<ResponceRegister> response=apiService.get_token().execute();
			if(response.isSuccessful()) Log.d(TAG, "SUCCESS");
			else {
				Log.d(TAG, "FAIL: "+ response.errorBody().toString());
				return null;
			}

			return response.body();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}