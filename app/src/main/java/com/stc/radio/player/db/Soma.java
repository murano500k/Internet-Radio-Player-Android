package com.stc.radio.player.db;

import java.util.Random;

/**
 * Created by artem on 10/13/16.
 */

public class Soma extends Station   {
	@Override
	public String getArtUrl(){

		return "http://somafm.com/img3/"+getKey()+"-400.jpg";
	}

	@Override
	public String getUrl() {
		int num=new Random().nextInt(1)+1;

		return "http://ice"+num+".somafm.com/"+getKey()+"-128-aac";
	}
}
