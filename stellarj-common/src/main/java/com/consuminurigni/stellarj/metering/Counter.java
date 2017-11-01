package com.consuminurigni.stellarj.metering;

public interface Counter {

	void set_count(int size);

	void inc();

	void dec();

	long getCount();

}
