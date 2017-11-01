package com.consuminurigni.stellarj.common;

public class BufUtils {

	public static byte[] concat(byte[]...bs) {
		int len = 0;
		for(int i = 0; i < bs.length; i++) {
			len += bs[i].length;
		}
		byte[] res = new byte[len];
		int cur = 0;
		for(int i = 0; i < bs.length; i++) {
			int length = bs[i].length;
			System.arraycopy(bs[i], 0, res, cur, length);
			cur += length;
		}
		return res;
	}

}
