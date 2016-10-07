package com.stc.radio.player;


import java.io.FileNotFoundException;
import java.util.ArrayList;

public class M3UParser {

	private static final String TAG = "M3U_Parser";

	public M3UParser() throws Exception {

	}

	public M3UHolder parseString(String s) throws FileNotFoundException {
		if (s!=null && s.length()>0 && !s.equals("")) {
			String stream = s;
			stream = stream.replace("#EXTM3U", "").trim();
			if(stream.startsWith("#EXTINF:"))	stream = stream.replaceFirst("#EXTINF:.{1,6},", "");
			String[] arr = stream.split("#EXTINF:.{1,6},");
			ArrayList<String> urls=new ArrayList<>();
			ArrayList<String> data=new ArrayList<>();
			{
				for (int n = 0; n < arr.length; n++) {
					//Log.w(TAG, "arr"+n+"="+arr[n]);
					if (arr[n].contains("http")) {
						String res=arr[n].replaceAll("\n", "");
						//res=res.replaceAll(" ", "");
						String url = res.substring(res.indexOf("http://"));
						String name = res.substring(0,res.indexOf("http://"));
						urls.add(url);
						data.add(name);
					}
				}
			}
			return new M3UHolder(data, urls);
		}
		return null;
	}

	public class M3UHolder {
		private ArrayList<String> data, url;

		M3UHolder(ArrayList<String> names, ArrayList<String> urls) {
			this.data = names;
			this.url = urls;
		}

		public ArrayList<String> getNames() {
			return data;
		}
		public ArrayList<String> getUrls() {
			return url;
		}

		public int getSize() {
			if (url != null)
				return url.size();
			return 0;
		}

		public String getName(int n) {
			return data.get(n);
		}

		public String getUrl(int n) {
			return url.get(n);
		}
	}
}
