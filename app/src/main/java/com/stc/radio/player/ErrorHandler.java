package com.stc.radio.player;

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
    private static final String ERROR_LOG_FILE = "/sdcard/irp_error_log/log_";

    HashMap<String,ExoPlaybackException>errorsMap;

    public ErrorHandler(){
        errorsMap=new HashMap<>();
    }
    public void addError(ExoPlaybackException e){
        String timestamp=new Date().toString();
        errorsMap.put(timestamp,e);
        Log.e(TAG, "addError: "+timestamp+" ", e);
    }


    public void writeErrorsToFile() throws IOException {
        String logFile=ERROR_LOG_FILE+new Date().getTime();
        File file = new File(logFile);
        if(!file.exists()) {
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(logFile, true);
        for (String timestamp :
                errorsMap.keySet()) {
            String line = timestamp+" "+errorsMap.get(timestamp).getMessage();
            fileWriter.append(line);
        }
        fileWriter.flush();
        errorsMap.clear();
    }

}
