package com.android.murano500k.onlineradioplayer.coverarttask;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

//import android.provider.DocumentsContract.Document;

public class AsyncGetReleaseGroupIDFromMusicBrainzTask extends AsyncTask<String, Integer, String>{
    private static final String TAG = "AlbumArtHelper";

    String ns=null;

	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		String albumName=params[0];
		String artistName=params[1];
		//String musicBrainzSearchUrl="http://www.musicbrainz.org/ws/2/release-group?query="+albumName+" AND artist:" +artistName+ " AND primarytype:album NOT secondarytype:live";
		String musicBrainzSearchUrl="http://www.musicbrainz.org/ws/2/release-group?query="+albumName+" AND artist:" +artistName;
		musicBrainzSearchUrl=musicBrainzSearchUrl.replaceAll(" ", "%20");
		Log.i("URL", musicBrainzSearchUrl);
		InputStream musicBrainzSearchIs=retrieveXMLFromURL(musicBrainzSearchUrl);

		if(musicBrainzSearchIs==null)
		{
			return null;
		}	
		
		String releaseGroupID=null;
		try {
			releaseGroupID=parse(musicBrainzSearchIs);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoReleaseGroupsInSearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return releaseGroupID;
	}
	
	private InputStream retrieveXMLFromURL(String URL){
		
		HttpResponse response;
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpGet=new HttpGet(URL);
		Bitmap bitmap=null;
		InputStream is=null;

		try {			
			response = httpclient.execute(httpGet);
			
			StatusLine statusLine=response.getStatusLine();
			int statusLineCode=statusLine.getStatusCode();
			
			if(statusLineCode==200)
			{
				HttpEntity httpEntity = response.getEntity();
				is=httpEntity.getContent();
			}
			else 
			{
	            Log.i("HttpRequestFailureRsn", statusLine.getReasonPhrase());
	        }  
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return is;
		
	}
	
    public String parse(InputStream in) throws XmlPullParserException, IOException, NoReleaseGroupsInSearchException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }
    
    private String readReleaseGroupID(XmlPullParser parser) throws XmlPullParserException, IOException, NoReleaseGroupsInSearchException{
    	String releaseGroupID=null;
        parser.require(XmlPullParser.START_TAG, ns, "release-group-list");
        int numOfReleaseGroups=Integer.parseInt(parser.getAttributeValue("", "count"));
        if(numOfReleaseGroups == 0){
        	throw new NoReleaseGroupsInSearchException();
        }
        Log.i("Parser",parser.getName());
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
            	Log.i("Parser",parser.getName());
                continue;
            }
            String name = parser.getName();
            Log.i("Parser",name);
            if (name.equals("release-group")) {
            	releaseGroupID=parser.getAttributeValue("", "id");
            	Log.i("ReleaseGroupID",releaseGroupID);
            	break;
            } else {
                skip(parser);
            }
        }  
        return releaseGroupID;
    }
    
    private String readFeed(XmlPullParser parser) throws XmlPullParserException, IOException, NoReleaseGroupsInSearchException {
    	String releaseGroupID=null;
        parser.require(XmlPullParser.START_TAG, ns, "artist");
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            Log.i("Parser",name);
            if (name.equals("release-group-list")) {
                releaseGroupID=readReleaseGroupID(parser);
                break;
            } else {
               skip(parser);
            }
        }  
        return releaseGroupID;
    }
    
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
     }
    
   class NoReleaseGroupsInSearchException extends Exception{
	   public NoReleaseGroupsInSearchException(){
		   super("No release groups in search");
	   }
    }
    

}  

