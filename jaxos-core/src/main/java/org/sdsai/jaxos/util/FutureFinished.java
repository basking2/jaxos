package org.sdsai.jaxos.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureFinished<T> implements Future<T> {
	final T v;
	
	public FutureFinished(final T v) {
		this.v = v;
	}

	@Override
	public boolean cancel(boolean arg0) {
		return false;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return v;
	}

	@Override
	public T get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		return get();
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

}
