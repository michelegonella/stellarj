package com.consuminurigni.stellarj.common;

import java.util.function.Consumer;

import javax.annotation.Nullable;

public interface VirtualTimerErrorCallback extends Consumer<VirtualTimerErrorCode>{
	@Override
	void accept(@Nullable VirtualTimerErrorCode t);
}
