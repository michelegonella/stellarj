package com.consuminurigni.stellarj.metering;

public interface Meter {

	void mark();

	void mark(int byteCount);

}
