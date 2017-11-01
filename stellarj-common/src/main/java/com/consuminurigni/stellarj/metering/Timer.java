package com.consuminurigni.stellarj.metering;

import java.util.concurrent.TimeUnit;

public interface Timer {

	void update(long amount, TimeUnit unit);

}
