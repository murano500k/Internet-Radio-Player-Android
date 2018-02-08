package com.stc.radio.player;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by artem on 1/25/18.
 */

public class ErrorHandler {
    private static final String TAG = "ErrorHandler";
    private static final String ERROR_LOG_DIR = "/sdcard/irp_error_log";
    private final Context mContext;

    HashMap<String,ExoPlaybackException>errorsMap;

    public ErrorHandler(Context context){
        this.mContext=context;
        errorsMap=new HashMap<>();
        File dir = new File(ERROR_LOG_DIR);

        if(!dir.exists()) {
            dir.mkdirs();
        }

    }
    public void addError(ExoPlaybackException e){
        String timestamp=new Date().toString();
        errorsMap.put(timestamp,e);
        Log.e(TAG, "addError: "+timestamp+"| "+getReadableError(e));
    }
    public static String getReadableError(ExoPlaybackException e){
        switch (e.type){
            case ExoPlaybackException.TYPE_RENDERER:
                return e.getRendererException().getMessage();
            case ExoPlaybackException.TYPE_SOURCE:
                return e.getSourceException().getMessage();
            case ExoPlaybackException.TYPE_UNEXPECTED:
                return e.getUnexpectedException().getMessage();
        }
        return "no data";
    }
    public static boolean sameErrors(ExoPlaybackException e1,ExoPlaybackException e2){
        if(e1==null && e2==null)return true;
        else if(e1==null || e2==null) return false;
        if(e1.type!=e2.type)return false;
        switch (e1.type) {
            case ExoPlaybackException.TYPE_RENDERER:
                return e1.getRendererException().getMessage().contains(
                        e2.getRendererException().getMessage()
                );
            case ExoPlaybackException.TYPE_SOURCE:
                return e1.getSourceException().getMessage().contains(
                        e2.getSourceException().getMessage()
                );
            case ExoPlaybackException.TYPE_UNEXPECTED:
                return e1.getUnexpectedException().getMessage().contains(
                        e2.getUnexpectedException().getMessage()
                );
        }
        return false;
    }


    public void writeErrorsToFile() throws IOException {

        String logFile= "log_"+new Date().getTime();
        File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"error_log");
        if(!file.exists()){
            file.mkdir();
        }

        try{
            File gpxfile = new File(file, logFile);
           // Log.w(TAG, "writeErrorsToFile() called"+gpxfile.getAbsolutePath());
            FileWriter writer = new FileWriter(gpxfile);
            for (String timestamp :
                    errorsMap.keySet()) {
                ExoPlaybackException e=errorsMap.get(timestamp);
                String line = timestamp+"\n\t"+e.getMessage()+" "+e.getCause().toString()+"\n\t"+
                        e.getCause().getCause().toString()
                        +"\n\n";
                writer.append(line);
               // Log.w(TAG, "writeErrorsToFile: "+line );
            }
            writer.flush();
            writer.close();

        }catch (Exception e){
            e.printStackTrace();

        }

        errorsMap.clear();
    }

}
