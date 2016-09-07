package com.android.murano500k.newradio;

/**
 * Created by artem on 7/22/16.
 */
public class Constants {

	final public static boolean USE_RX= true;


	public static final String KEY_FAV_ONLY = "com.android.murano500k.KEY_FAV_ONLY";
	public static final String KEY_IS_SHUFFLE = "com.android.murano500k.KEY_IS_SHUFFLE";

	public static final String DATA_AUDIO_BUFFER_CAPACITY = "com.android.murano500k" +
			".DATA_AUDIO_BUFFER_CAPACITY";
	public static final String DATA_AUDIO_DECODE_CAPACITY = "com.android.murano500k" +
			".DATA_AUDIO_DECODE_CAPACITY";
	public static final int REQUSET_ADD_STATION = 543;

	public static final class STATE_AUDIO_DEVICE {
		public static final int PLUGGED= 1;
		public static final int UNPLUGGED= -1;
		public static final int UNKNOWN= 0;
	}

	final public static class UI_STATE{
		public static final int LOADING= 0;
		public static final int PLAYING= 1;
		public static final int IDLE= -1;
	}
	public static class INTENT{


		public static class SLEEP{
			public static final String SET = "com.android.murano500k.SLEEP_SET";
			public static final String CANCEL = "com.android.murano500k.SLEEP_CANCEL";
		}
		public static final String SET_BUFFER_SIZE = "com.android.murano500k.INTENT_SET_BUFFER_SIZE";
		public static final String UPDATE_STATIONS = "com.android.murano500k.UPDATE_STATIONS";
	}
	public static class EXTRAS{
		public static final String CALLED_BY = "com.android.murano500k.CALLED_BY";
	}

	public static final String NO_DATA = "co.mobiwise.library.NO_DATA";

	public static final String ACTION_SLEEP_UPDATE = "com.android.murano500k.ACTION_SLEEP_UPDATE";
	public static final String ACTION_SLEEP_CANCEL = "com.android.murano500k.ACTION_SLEEP_CANCEL";
	public static final String ACTION_SLEEP_FINISH = "com.android.murano500k.ACTION_SLEEP_FINISH";

	public static final String DATA_SLEEP_TIMER_LEFT_SECONDS = "com.android.murano500k.DATA_SLEEP_TIMER_LEFT_SECONDS";
	public static final String DATA_CURRENT_STATION_URL = "com.android.murano500k.DATA_CURRENT_STATION_URL";
	public static final String DATA_FAV_ONLY = "com.android.murano500k.DATA_FAV_ONLY";



	final public static String KEY_SELECTED_URL= "com.android.murano500k.KEY_SELECTED_URL";
	final public static String KEY_LIST_URLS= "com.android.murano500k.KEY_LIST_URLS";
	final public static String DATA_LIST_URLS= "com.android.murano500k.DATA_LIST_URLS";




	public static final int NOTIFICATION_ID = 1431;


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
