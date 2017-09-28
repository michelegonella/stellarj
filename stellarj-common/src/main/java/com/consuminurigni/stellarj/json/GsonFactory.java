package com.consuminurigni.stellarj.json;

import com.consuminurigni.stellarj.xdr.Uint32;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonFactory {

	public static Gson create() {
		GsonBuilder gsonb = new GsonBuilder();
		gsonb.registerTypeAdapter(Uint32.class, new XdrIntegerSerializer());
		Gson gson = gsonb.create();
		return gson;
	}

}
