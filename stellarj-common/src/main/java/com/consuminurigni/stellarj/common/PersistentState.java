package com.consuminurigni.stellarj.common;

public class PersistentState {
	public     enum Entry
    {
        kLastClosedLedger,
        kHistoryArchiveState,
        kForceSCPOnNextLaunch,
        kLastSCPData,
        kDatabaseSchema,
//        kLastEntry
    }

	public void setState(Entry klastscpdata, String scpState) {
		// TODO Auto-generated method stub
		
	}

	public String getState(Entry klastscpdata) {
		// TODO Auto-generated method stub
		return null;
	};

}
