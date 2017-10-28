package com.consuminurigni.stellarj.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Assert {
	private static final Logger log = LogManager.getLogger();

	public static void assertTrue(boolean cond) throws IllegalStateException {
		if(! cond) {
			abort();
		}
	}

//
	public static void abort() throws IllegalStateException {
		IllegalStateException e = new IllegalStateException();
		try {
			throw e;
		} catch(IllegalStateException ex) {
			log.fatal(ex);
			System.err.println(Exceptions.toStackTrace(ex));
		}
//		assert false;
		throw e;
	}

	public static <T> T assertNotNull(T value) {
		if(value == null) {
			throw new NullPointerException();
		}
		return value;
	}


	public static <T> T assertNotNull(T value, String msg) {
		if(value == null) {
			throw new NullPointerException(msg);
		}
		return value;
	}

}
