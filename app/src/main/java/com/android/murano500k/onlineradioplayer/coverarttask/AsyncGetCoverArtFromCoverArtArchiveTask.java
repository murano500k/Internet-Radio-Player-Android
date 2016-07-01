package com.android.murano500k.onlineradioplayer.coverarttask;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class AsyncGetCoverArtFromCoverArtArchiveTask extends AsyncTask<String, Integer, String>{
    private static final String TAG = "AlbumArtHelper";

	
	File appDirectory = new File(Environment.getExternalStorageDirectory()+File.separator+"Musika");
    
    
	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		String releaseGroupID=params[0];
		 //put the downloaded file here
		appDirectory.mkdirs();
		return DownloadUsingReleaseGroupID(releaseGroupID);
		//return null;
	}

	
    public String DownloadUsingReleaseGroupID(String releaseGroupID) {  //this is the downloader method
        try {
                //URL url = new URL("http://yoursite.com/&quot; + imageURL); //you can write here any link
        		URL url=new URL("http://coverartarchive.org/release-group/"+releaseGroupID+"/front");
        		String fileName=appDirectory+"/"+releaseGroupID+".jpg";
                File file = new File(fileName);

                long startTime = System.currentTimeMillis();
                Log.d("ImageManager", "download begining");
                Log.d("ImageManager", "download url:" + url);
                Log.d("ImageManager", "downloaded file name:" + fileName);
                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();

                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                InputStream is = ucon.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);

                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */
                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                }

                /* Convert the Bytes read to a String. */
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(baf.toByteArray());
                fos.close();
            Log.d(TAG, "File: " + file.getAbsolutePath());

            return "SUCCESS";

        } catch (IOException e) {
                Log.d("ImageManager", "Error: " + e);
                return "FAIL";
        }

}
}
