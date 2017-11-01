package com.consuminurigni.stellarj.common;

import java.time.Duration;
import java.util.function.Consumer;

import javax.annotation.Nullable;

public class VirtualTimer {

//	public VirtualTimer(Application app) {
//		// TODO Auto-generated constructor stub
//	}

	public VirtualTimer(VirtualClock virtualClock) {
		// TODO Auto-generated constructor stub
	}

	public void cancel() {
		// TODO Auto-generated method stub
		
	}

	public void expires_from_now(Duration timeout) {
		// TODO Auto-generated method stub
		
	}

	public void async_wait(VirtualTimerCallback onSuccess, VirtualTimerErrorCallback onError) {
		// TODO Auto-generated method stub
		
	}

	public void async_wait(VirtualTimerErrorCallback onError) {
		// TODO Auto-generated method stub
		
	}

	public static void onFailureNoop(@Nullable VirtualTimerErrorCode e) {
		
	}
}
