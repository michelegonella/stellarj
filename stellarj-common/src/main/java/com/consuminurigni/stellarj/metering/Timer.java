package com.consuminurigni.stellarj.metering;

import java.util.concurrent.TimeUnit;

/*
final Timer.Context context = timer.time();
try {
    // handle request
} finally {
    context.stop();
}
*/
public interface Timer {

	void update(long amount, TimeUnit unit);

	TimerContext time();

}
