package com.android.murano500k.newradio;

/**
 * Created by artem on 7/22/16.
 */
public class Constants {
	public static final String INTENT_UPDATE_STATIONS = "com.android.murano500k.INTENT_UPDATE_STATIONS";
	public static final String INTENT_UPDATE_FAVORITE_STATION = "com.android.murano500k.INTENT_UPDATE_FAVORITE_STATION";
	public static final String INTENT_HANDLE_CONNECTIVITY = "com.android.murano500k.INTENT_HANDLE_CONNECTIVITY";
	public static final String INTENT_PLAY_PAUSE = "com.android.murano500k.INTENT_PLAY_PAUSE";
	public static final String INTENT_RESUME_PLAYBACK = "com.android.murano500k.INTENT_RESUME_PLAYBACK";
	public static final String INTENT_PAUSE_PLAYBACK = "com.android.murano500k.INTENT_PAUSE_PLAYBACK";
	public static final String INTENT_PLAY_NEXT = "com.android.murano500k.INTENT_PLAY_NEXT";
	public static final String INTENT_PLAY_RANDOM = "com.android.murano500k.INTENT_PLAY_RANDOM";
	public static final String INTENT_PLAY_PREV = "com.android.murano500k.INTENT_PLAY_PREV";
	public static final String INTENT_SLEEP_TIMER_SET= "com.android.murano500k.INTENT_SLEEP_TIMER_SET";
	public static final String INTENT_SLEEP_TIMER_CANCEL = "com.android.murano500k.INTENT_SLEEP_TIMER_CANCEL";
	public static final String INTENT_CLOSE_NOTIFICATION = "com.android.murano500k.INTENT_CLOSE_NOTIFICATION";
	public static final String INTENT_OPEN_APP = "com.android.murano500k.INTENT_OPEN_APP";
	public static final String ACTION_MEDIAPLAYER_STOP = "co.mobiwise.library.ACTION_STOP_MEDIAPLAYER";

	public static final String INTENT_MEDIA_BUTTON = "com.android.murano500k.INTENT_MEDIA_BUTTON";
	public static final String ACTION_SLEEP_UPDATE = "com.android.murano500k.ACTION_SLEEP_UPDATE";
	public static final String ACTION_SLEEP_CANCEL = "com.android.murano500k.ACTION_SLEEP_CANCEL";
	public static final String ACTION_SLEEP_FINISH = "com.android.murano500k.ACTION_SLEEP_FINISH";

	public static final String DATA_SLEEP_TIMER_LEFT_SECONDS = "com.android.murano500k.DATA_SLEEP_TIMER_LEFT_SECONDS";
	public static final String DATA_CURRENT_STATION_URL = "com.android.murano500k.DATA_CURRENT_STATION_URL";
	public static final String DATA_STATION_INDEX = "com.android.murano500k.DATA_STATION_INDEX";
	public static final String DATA_STATION_FAVORITE = "com.android.murano500k.DATA_STATION_FAVORITE";

	public static final String DATA_FAV_ONLY = "com.android.murano500k.DATA_FAV_ONLY";



	final public static String KEY_SELECTED_INDEX= "com.android.murano500k.KEY_SELECTED_INDEX";

	final public static String UI_STATE_LOADING= "com.android.murano500k.UI_STATE_LOADING";
	final public static String UI_STATE_PLAYING= "com.android.murano500k.UI_STATE_PLAYING";
	final public static String UI_STATE_IDLE= "com.android.murano500k.UI_STATE_IDLE";

	final public static int AUDIO_DEVICE_STATE_PLUGGED= 1;
	final public static int AUDIO_DEVICE_STATE_UN_PLUGGED= 0;

	public static final int NOTIFICATION_ID = 1441;


	public static final String NO_URL= "com.android.murano500k.NO_URL";


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


}
