package com.android.murano500k.newradio;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.odesanmi.m3ufileparser.M3U_Parser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Created by artem on 9/6/16.
 */
@RunWith(AndroidJUnit4.class)
public class TestSelectPls extends Assert {
	private static final String TAG = "TEST";
	String testStr= "#EXTM3U#EXTINF:0,Digitally Imported - Basslinehttp://pub2.diforfree.org:8000/di_bassline_hi#EXTINF:0,Digitally Imported - Big Beathttp://pub2.diforfree.org:8000/di_bigbeat_hi#EXTINF:0,Digitally Imported - Breakshttp://pub2.diforfree.org:8000/di_breaks_hi#EXTINF:0,Digitally Imported - ChillHophttp://pub2.diforfree.org:8000/di_chillhop_hi#EXTINF:0,Digitally Imported - Chillstephttp://pub2.diforfree.org:8000/di_chillstep_hi#EXTINF:0,Digitally Imported - Dark DnBhttp://pub2.diforfree.org:8000/di_darkdnb_hi#EXTINF:0,Digitally Imported - Deep Househttp://pub2.diforfree.org:8000/di_deephouse_hi#EXTINF:0,Digitally Imported - Deep Techhttp://pub2.diforfree.org:8000/di_deeptech_hi#EXTINF:0,Digitally Imported - Drum 'n Basshttp://pub2.diforfree.org:8000/di_drumandbass_hi#EXTINF:0,Digitally Imported - Dubhttp://pub2.diforfree.org:8000/di_dub_hi#EXTINF:0,Digitally Imported - Dub Technohttp://pub2.diforfree.org:8000/di_dubtechno_hi#EXTINF:0,Digitally Imported - Dubstephttp://pub2.diforfree.org:8000/di_dubstep_hi#EXTINF:0,Digitally Imported - Future Synthpophttp://pub2.diforfree.org:8000/di_futuresynthpop_hi#EXTINF:0,Digitally Imported - Junglehttp://pub2.diforfree.org:8000/di_jungle_hi#EXTINF:0,Digitally Imported - Latin Househttp://pub2.diforfree.org:8000/di_latinhouse_hi#EXTINF:0,Digitally Imported - Liquid DnBhttp://pub2.diforfree.org:8000/di_liquiddnb_hi#EXTINF:0,Digitally Imported - Minimalhttp://pub2.diforfree.org:8000/di_minimal_hi#EXTINF:0,Digitally Imported - Oldschool Acidhttp://pub2.diforfree.org:8000/di_oldschoolacid_hi#EXTINF:0,Digitally Imported - Oldschool Ravehttp://pub2.diforfree.org:8000/di_oldschoolrave_hi#EXTINF:0,Digitally Imported - Oldschool Techno & Trancehttp://pub2.diforfree.org:8000/di_classicelectronica_hi#EXTINF:0,Digitally Imported - Progressive Psyhttp://pub2.diforfree.org:8000/di_progressivepsy_hi#EXTINF:0,Digitally Imported - Underground Technohttp://pub2.diforfree.org:8000/di_undergroundtechno_hi#EXTINF:0,Digitally Imported - Vocal Trancehttp://pub2.diforfree.org:8000/di_vocaltrance_hi";
	@Test
	public void testParse(){
		ArrayList<String> allURls = new ArrayList<String>();
		String everything=null;

		try {
			M3U_Parser parser=new M3U_Parser();
			M3U_Parser.M3UHolder holder = parser.parseString(testStr);
			Log.w(TAG, "result size: "+holder.getSize());
			Log.w(TAG, "result Names: "+ holder.getNames());
			Log.w(TAG, "result Urls: "+holder.getUrls());
			assertNotNull(holder);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
