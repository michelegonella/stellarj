package com.consuminurigni.stellarj.metering;


public interface Metrics {

	Counter newCounter(String string, String string2, String string3);

	Meter newMeter(String string, String string2, String string3, String string4);

	Timer newTimer(String string, String string2, String string3);

}
