package com.consuminurigni.stellarj.common;

public class NetUtils {

	public static String ipv4ToString(byte[] ip) {
		return ip[0]+"."+ip[1]+"."+ip[2]+"."+ip[3];
	}
}
