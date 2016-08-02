package com.android.murano500k.newradio;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class StationContent {

    public static final String[] contentArrShort = {

    };

    public static final String[] contentArr = {
            "http://pub1.diforfree.org:8000/di_ambient_hi",
		    "http://pub1.diforfree.org:8000/di_bassline_hi",
		    "http://pub1.diforfree.org:8000/di_bigbeat_hi",
		    "http://pub1.diforfree.org:8000/di_bigroomhouse_hi",
            "http://pub1.diforfree.org:8000/di_breaks_hi",
            "http://pub1.diforfree.org:8000/di_chillhop_hi",
            "http://pub1.diforfree.org:8000/di_chillout_hi",
            "http://pub1.diforfree.org:8000/di_chilloutdreams_hi",
            "http://pub1.diforfree.org:8000/di_chiptunes_hi",
            "http://pub1.diforfree.org:8000/di_classiceurodance_hi",
            "http://pub1.diforfree.org:8000/di_classiceurodisco_hi",
            "http://pub1.diforfree.org:8000/di_classictrance_hi",
            "http://pub1.diforfree.org:8000/di_classicvocaltrance_hi",
            "http://pub1.diforfree.org:8000/di_clubdubstep_hi",
            "http://pub1.diforfree.org:8000/di_clubsounds_hi",
            "http://pub1.diforfree.org:8000/di_cosmicdowntempo_hi",
            "http://pub1.diforfree.org:8000/di_darkdnb_hi",
            "http://pub1.diforfree.org:8000/di_deephouse_hi",
            "http://pub1.diforfree.org:8000/di_deepnudisco_hi",
            "http://pub1.diforfree.org:8000/di_deeptech_hi",
            "http://pub1.diforfree.org:8000/di_discohouse_hi",
            "http://pub1.diforfree.org:8000/di_djmixes_hi",
            "http://pub1.diforfree.org:8000/di_downtempolounge_hi",
            "http://pub1.diforfree.org:8000/di_drumandbass_hi",
            "http://pub1.diforfree.org:8000/di_dubstep_hi",
		    "http://pub1.diforfree.org:8000/di_dub_hi",
		    "http://pub1.diforfree.org:8000/di_dubtechno_hi",
		    "http://pub1.diforfree.org:8000/di_eclectronica_hi",
            "http://pub1.diforfree.org:8000/di_electrohouse_hi",
            "http://pub1.diforfree.org:8000/di_epictrance_hi",
            "http://pub1.diforfree.org:8000/di_eurodance_hi",
            "http://pub1.diforfree.org:8000/di_funkyhouse_hi",
            "http://pub1.diforfree.org:8000/di_futuresynthpop_hi",
            "http://pub1.diforfree.org:8000/di_glitchhop_hi",
            "http://pub1.diforfree.org:8000/di_goapsy_hi",
            "http://pub1.diforfree.org:8000/di_handsup_hi",
            "http://pub1.diforfree.org:8000/di_hardcore_hi",
            "http://pub1.diforfree.org:8000/di_harddance_hi",
            "http://pub1.diforfree.org:8000/di_hardstyle_hi",
            "http://pub1.diforfree.org:8000/di_house_hi",
		    "http://pub1.diforfree.org:8000/di_jungle_hi",
		    "http://pub1.diforfree.org:8000/di_latinhouse_hi",
            "http://pub1.diforfree.org:8000/di_liquiddnb_hi",
            "http://pub1.diforfree.org:8000/di_liquiddubstep_hi",
            "http://pub1.diforfree.org:8000/di_lounge_hi",
            "http://pub1.diforfree.org:8000/di_mainstage_hi",
            "http://pub1.diforfree.org:8000/di_minimal_hi",
            "http://pub1.diforfree.org:8000/di_oldschoolacid_hi",
            "http://pub1.diforfree.org:8000/di_oldschoolelectronica_hi",
            "http://pub1.diforfree.org:8000/di_progressive_hi",
            "http://pub1.diforfree.org:8000/di_progressivepsy_hi",
            "http://pub1.diforfree.org:8000/di_psychill_hi",
            "http://pub1.diforfree.org:8000/di_russianclubhits_hi",
            "http://pub1.diforfree.org:8000/di_sankeys_hi",
            "http://pub1.diforfree.org:8000/di_soulfulhouse_hi",
            "http://pub1.diforfree.org:8000/di_spacemusic_hi",
            "http://pub1.diforfree.org:8000/di_techhouse_hi",
            "http://pub1.diforfree.org:8000/di_techno_hi",
            "http://pub1.diforfree.org:8000/di_trance_hi",
            "http://pub1.diforfree.org:8000/di_trap_hi",
            "http://pub1.diforfree.org:8000/di_tribalhouse_hi",
            "http://pub1.diforfree.org:8000/di_ukgarage_hi",
            "http://pub1.diforfree.org:8000/di_umfradio_hi",
		    "http://pub1.diforfree.org:8000/di_undergroundtechno_hi",
		    "http://pub1.diforfree.org:8000/di_vocalchillout_hi",
            "http://pub1.diforfree.org:8000/di_vocaltrance_hi"
    };



    private static final int COUNT = 25;
    private static final String TAG = "StationContent" ;

    public static ArrayList<Station> initStations(){
        ArrayList<Station>list = new ArrayList<>();

        for (int i = 0; i < contentArr.length; i++) {
            list.add(createStationItem(i, contentArr[i]));
        }
        return list;
    }
    private static Station createStationItem(int position,String urlStr) {
        return new Station(position, makeNameFromUrl(urlStr),urlStr);
    }




    public static int getArt(String fileName, Context c){
        int resID = c.getResources().getIdentifier(fileName, "drawable", c.getPackageName());
        if(resID!=0) return resID;
        else return R.drawable.default_art;
    }

    private static String makeNameFromUrl(String urlStr) {
        return urlStr.substring(urlStr.lastIndexOf("di_")+3, urlStr.lastIndexOf("_hi"));
    }


    public static String getArtistFromString(String data){
        String artistName="";
        if(data!=null && data.contains(" - ")) {
            artistName=data.substring(0, data.indexOf(" - "));
        }
        Log.d(TAG,"getArtistFromString");
        Log.d(TAG,"dataStr: "+ data);
        Log.d(TAG,"artistNameStr: "+ artistName);

        return artistName;
    }
    public static String getTrackFromString(String data){
        String trackName="";
        if(data!=null && data.contains(" - ")) {
            trackName=data.substring(data.indexOf(" - ")+3);
        }
        Log.d(TAG,"getTrackFromString");
        Log.d(TAG,"dataStr: "+ data);
        Log.d(TAG,"trackName: "+ trackName);
        return trackName;
    }

}
