package org.sdsai.jaxos.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureFailure<T> implements Future<T> {
	
	final Throwable throwable;
	
	public FutureFailure(final Throwable throwable) {
		this.throwable = throwable;
	}

	@Override
	public boolean cancel(boolean arg0) {
		return true;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		throw new ExecutionException(throwable);
	}

	@Override
	public T get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		throw new ExecutionException(throwable);
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
