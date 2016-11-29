package com.stc.radio.player.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.io.File;

/**
 * Created by artem on 10/10/16.
 */

public interface BitmapManager {

	Bitmap getArtBitmap(File file);
	File getArtFile(String name);
	Bitmap drawableToBitmap (Drawable drawable);

}
