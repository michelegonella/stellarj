package com.consuminurigni.stellarj.json;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.consuminurigni.stellarj.xdr.Uint32;
import com.google.gson.Gson;


public class GsonTest {

	@Test
	public void testMapsAndLists() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("x", Arrays.asList("y", 1, true));
		Map<String, Object> m1 = new LinkedHashMap<>();
		m1.put("z", Arrays.asList(m));
		String json = GsonFactory.create().toJson(m1);
		Assert.assertEquals("{\"z\":[{\"x\":[\"y\",1,true]}]}", json);
	}

	@Test
	public void testCustomNumber() {
		Uint32 n  = Uint32.ofPositiveInt(2147483647);
		n = n.plus(2147483647);
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("x", n);
		Gson gson = GsonFactory.create();
		String json = gson.toJson(m);
		System.err.println(json);
	}

}
