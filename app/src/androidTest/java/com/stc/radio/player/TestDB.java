package com.stc.radio.player;

import android.support.test.runner.AndroidJUnit4;

import com.stc.radio.player.db.DbHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import timber.log.Timber;

import static com.stc.radio.player.db.DbHelper.insertStations;

/**
 * Created by artem on 9/21/16.
 */
@RunWith(AndroidJUnit4.class)
public class TestDB extends TestBase {


	@Test
	public void testCreate(){
		Timber.v("Hello timber");

	}
}
