package com.consumimurigni.stellarj.main;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.consumimurigni.stellarj.ledger.LedgerManager;
import com.consuminurigni.stellarj.xdr.Hash;

public abstract class Application {
	private final MetricRegistry metrics = new MetricRegistry();

	public MetricRegistry getMetrics() {
		return metrics;
	}

	public Meter newMeter(String domain, String type, String name, String scope) {
		// TODO Auto-generated method stub
		return null;
	}

	public Counter newCounter(String domain, String type, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer newTimer(String domain, String type, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public VirtualClock getClock() {
		// TODO Auto-generated method stub
		return null;
	}

	public LedgerManager getLedgerManager() {
		// TODO Auto-generated method stub
		return null;
	}

	public Config getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	public void syncOwnMetrics() {
		// TODO Auto-generated method stub
		
	}

	//seconds
	public long timeNow() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Hash getNetworkID() {
		// TODO Auto-generated method stub
		return null;
	}

	public String toJsonString(Object v) {
		// TODO Auto-generated method stub
		return null;
	}

	public OverlayManager getOverlayManager() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getStateHuman() {
		// TODO Auto-generated method stub
		return null;
	}

	public PersistentState getPersistentState() {
		// TODO Auto-generated method stub
		return null;
	}

	public JdbcTemplate getDatabase() {
		// TODO Auto-generated method stub
		return null;
	}

	public TransactionTemplate getTransactionTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

}
