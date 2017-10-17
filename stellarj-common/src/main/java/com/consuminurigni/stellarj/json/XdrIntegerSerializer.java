package com.consuminurigni.stellarj.json;

import java.lang.reflect.Type;

import com.consuminurigni.stellarj.xdr.XdrInteger;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class XdrIntegerSerializer implements JsonSerializer<XdrInteger> {

	@Override
	public JsonElement serialize(XdrInteger src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.asBigInteger());
	}

}
