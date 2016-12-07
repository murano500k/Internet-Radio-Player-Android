package com.stc.radio.player;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TestActivity extends AppCompatActivity  implements SurfaceHolder.Callback {

    private static String TAG = "MediaFrameworkTest";
    private static SurfaceView mSurfaceView;
    private static ImageView mImageView;
    private PowerManager.WakeLock mWakeLock = null;
    public static Toast toast;

    public SurfaceView getSurfaceView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSurfaceView.setVisibility(VISIBLE);
                mImageView.setVisibility(GONE);
            }
        });
        return mSurfaceView;
    }

    public void setImage(final Bitmap b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSurfaceView.setVisibility(GONE);
                mImageView.setVisibility(VISIBLE);
                mImageView.setImageBitmap(b);
            }
        });
    }

    public int getMeasuredHeight() {
        return mImageView.getMeasuredHeight();
    }
    public int getMeasuredWidth() {
        return mImageView.getMeasuredWidth();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.surface_view);
        mSurfaceView = (SurfaceView)findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(this);

        mImageView = (ImageView)findViewById(R.id.imageView);
        mImageView.setVisibility(GONE);

        //Acquire the full wake lock to keep the device up
        //PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        //mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MediaFrameworkTest");
        //mWakeLock.setReferenceCounted(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume, acquire wakelock");
        super.onResume();
        //mWakeLock.acquire();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause, release wakelock");
        //mWakeLock.release();
        super.onPause();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        //Can do nothing in here. The test case will fail if the surface destroyed.
        Log.v(TAG, "Test application surface destroyed");
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //Do nothing in here. Just print out the log
        Log.v(TAG, "Test application surface changed");
    }

    public void surfaceCreated(SurfaceHolder holder) {

        holder.addCallback(this);
        Log.v(TAG, "Test application surface created");
    }

    public void startPlayback(String filename){
        String mimetype = MediaFormat.MIMETYPE_AUDIO_MPEG;
        Uri path = Uri.parse(filename);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, mimetype);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public void showToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(toast!=null ) {
                    toast.cancel();
                    toast=null;
                }
                toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }
}
