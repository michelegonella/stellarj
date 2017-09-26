package com.consumimurigni.stellarj.xdr;

import java.util.Arrays;

public class Vectors {

	public static <T> T[] emplace_back(T[] arr, T elm) {
		T[] res = Arrays.copyOf(arr, arr.length + 1);
		res[arr.length] = elm;
		return res;
	}

	public static <T> T[] erase(T[] arr, int pos) {
		T[] res = Arrays.copyOf(arr, arr.length - 1);
		for(int i = pos + 1; i < arr.length; i++) {
			res[i - 1] = arr[i];
		}
		return res;
	}
}
