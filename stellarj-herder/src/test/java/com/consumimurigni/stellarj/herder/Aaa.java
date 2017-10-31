package com.consumimurigni.stellarj.herder;

import static org.junit.Assert.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class Aaa {

	@Test
	public void test() {
		System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "INFO");
		Logger log = LogManager.getLogger();
		log.error("BAU {}",() -> 100);
	}

}
