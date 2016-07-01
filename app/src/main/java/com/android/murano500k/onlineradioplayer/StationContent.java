package com.android.murano500k.onlineradioplayer;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class StationContent {

    public static final String[] contentArr = {
            "http://pub2.diforfree.org:8000/di_djmixes_hi",
            "http://pub2.diforfree.org:8000/di_dub_hi",
            "http://pub2.diforfree.org:8000/di_drumandbass_hi",
            "http://pub2.diforfree.org:8000/di_liquiddnb_hi",
            "http://pub2.diforfree.org:8000/di_darkdnb_hi",
            "http://pub2.diforfree.org:8000/di_bigbeat_hi",
            "http://pub2.diforfree.org:8000/di_breaks_hi",
            "http://pub2.diforfree.org:8000/di_deeptech_hi",
            "http://pub2.diforfree.org:8000/di_dubtechno_hi",
            "http://pub2.diforfree.org:8000/di_jungle_hi",
            "http://pub2.diforfree.org:8000/di_bassline_hi",
            "http://pub2.diforfree.org:8000/di_chillhop_hi",
            "http://pub2.diforfree.org:8000/di_vocaltrance_hi",
            "http://pub2.diforfree.org:8000/di_undergroundtechno_hi",
            "http://pub2.diforfree.org:8000/di_minimal_hi",
            "http://pub2.diforfree.org:8000/di_chillhop_hi",
            "http://pub2.diforfree.org:8000/di_umfradio_hi"
    };

    public static final List<Station> STATION_LIST = new ArrayList<Station>();

    public static final Map<String, Station> ITEM_MAP = new HashMap<String, Station>();

    private static final int COUNT = 25;

    static {
        // Add some sample items.
        for (int i = 0; i < contentArr.length; i++) {
            addItem(createStationItem(i, contentArr[i]));
        }
    }

    private static void addItem(Station item) {
        STATION_LIST.add(item);
        ITEM_MAP.put(item.id, item);
    }

    private static Station createStationItem(int position,String urlStr) {
        return new Station(String.valueOf(position), makeNameFromUrl(urlStr),urlStr);
    }

    private static String makeNameFromUrl(String urlStr) {
        return urlStr.substring(urlStr.lastIndexOf("di_")+3, urlStr.lastIndexOf("_hi"));
    }

    public static class Station {
        public final String id;
        public String name;
        public String url;
        public Bitmap image;


        public Station(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }


    }
}
