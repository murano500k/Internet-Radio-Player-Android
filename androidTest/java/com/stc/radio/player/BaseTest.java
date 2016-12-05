package com.stc.radio.player;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by artem on 11/17/16.
 */
public abstract class BaseTest extends TestCase {
    static Instrumentation inst;
    public String packageName;

    public static String BASE_PATH_VIDEO = "/sdcard/test/rvideo/";
    public static String BASE_PATH_AUDIO = "/sdcard/test/raudio/";
    public static String BASE_PATH_IMAGE = "/sdcard/test/rimage/";
    public static String BASE_PATH_URL = "/sdcard/test/urls.txt";
    public static String BASE_PATH_APP = "/sdcard/test/rapk/";
    public String TAG = "BaseTest";
    static String ASSERT_MESSAGE_PREFIX;
    TestActivity activity;
    static String fileName;


    public void initBase(Instrumentation inst,  String msgPrefix){
        this.ASSERT_MESSAGE_PREFIX=msgPrefix;
        this.inst = inst;
    }

    public void launchExternalIntent(final Intent intent){
        try {
            inst.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    assertNotNull(ASSERT_MESSAGE_PREFIX+"getLaunchIntentForPackage error. app not installed",intent);
                    inst.getTargetContext().startActivity(intent);
                    //inst.startActivitySync(intent);
                    SystemClock.sleep(5000);
                    Log.w(ASSERT_MESSAGE_PREFIX+"active package: "+ inst.getUiAutomation().getRootInActiveWindow().getPackageName(), inst.getUiAutomation().getRootInActiveWindow().getPackageName()+"");
                    assertNotNull(ASSERT_MESSAGE_PREFIX+"active package: "+ inst.getUiAutomation().getRootInActiveWindow().getPackageName(), inst.getUiAutomation().getRootInActiveWindow().getPackageName());
                }
            });
        }catch (ActivityNotFoundException e){
            fail(ASSERT_MESSAGE_PREFIX+"ERROR ActivityNotFoundException " +packageName);
        }
    }

    public int getRandomSeconds(){
        Random random= new Random();
        return random.nextInt(6)-2;
    }

    public TestActivity launchMainActivity(){
        Intent intent = new Intent();
        intent.setClass(inst.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
        //return activity;
        return  (TestActivity) inst.startActivitySync(intent);
    }


    List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            inFiles.add(file);
        }
        return inFiles;
    }

    private void showToast(final Activity act,final String text) {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public ArrayList<String> readFile(final String path) throws IOException {
        ArrayList<String>strLineList=new ArrayList<>();
        String strLine;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"))) {
            while ((strLine = reader.readLine()) != null) {
                strLineList.add(strLine);
            }
        } catch (final IOException e) {
            fail(""+ e.getMessage());
        }
        return strLineList;
    }


    public void setAssertSamplePrefix(String samplePrefix){
        ASSERT_MESSAGE_PREFIX += "[ "+samplePrefix + " ] ";
        Log.w("testRandom", ASSERT_MESSAGE_PREFIX + "started");
    }

    public MainActivity killLaunchMainActivity(Activity activity){
        if(activity!=null) {
            inst.callActivityOnPause(activity);
            inst.callActivityOnStop(activity);
            inst.callActivityOnDestroy(activity);
        }
        Intent intent = new Intent();
        intent.setClass(inst.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP );

        return  (MainActivity) inst.startActivitySync(intent);
    }

}
