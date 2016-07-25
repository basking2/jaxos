package org.sdsai.jaxos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.sdsai.jaxos.net.JaxosEnsemble;
import org.sdsai.jaxos.paxos.DefaultPaxosAcceptorDao;
import org.sdsai.jaxos.paxos.DefaultPaxosProposerDao;
import org.sdsai.jaxos.paxos.Learner;
import org.sdsai.jaxos.paxos.Promise;
import org.sdsai.jaxos.paxos.Proposal;
import org.sdsai.jaxos.util.FutureFinished;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 */
public class JaxosFacade implements Learner.Listener<ByteBuffer> {
	
	final Logger LOG = LoggerFactory.getLogger(JaxosFacade.class);
	final JaxosEnsemble ensemble;
	final long timeout = 2;
	final TimeUnit timeunit = TimeUnit.MINUTES;
	final LoadingCache<String, CompletableFuture<Proposal<ByteBuffer>>> cache = 
			CacheBuilder.newBuilder().
			expireAfterWrite(2, TimeUnit.MINUTES).
			removalListener(new RemovalListener<String, CompletableFuture<Proposal<ByteBuffer>>>(){
				@Override
				public void onRemoval(RemovalNotification<String, CompletableFuture<Proposal<ByteBuffer>>> removal) {
					removal.getValue().completeExceptionally(new TimeoutException());
				}
			}).
			build(new CacheLoader<String, CompletableFuture<Proposal<ByteBuffer>>>(){

				@Override
				public CompletableFuture<Proposal<ByteBuffer>> load(String subject) throws Exception {
					return new CompletableFuture<Proposal<ByteBuffer>>();
				}}
			);
	
	public JaxosFacade(final JaxosConfiguration config) throws IOException{

		ensemble = config.buildEnsemble(
				"web",
				new DefaultPaxosProposerDao<ByteBuffer>(timeout, timeunit),
				new DefaultPaxosAcceptorDao<ByteBuffer>(timeout, timeunit),
				this);
	}
	
	public Future<Proposal<ByteBuffer>> mutiPaxos(
			final String subject,
			final String value,
			final long timeout,
			final TimeUnit timeunit)
	{
		try {
			Future<Proposal<ByteBuffer>> future = cache.get(subject);
			ensemble.propose(subject, 0L, ByteBuffer.wrap(value.getBytes()));
			return future;
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Future<Proposal<ByteBuffer>> paxos(
			final String subject, 
			final String value, 
			final long timeout, 
			final TimeUnit timeunit)
	{
		final long n = (long)(Math.random() * Long.MAX_VALUE);
		final Future<Promise<ByteBuffer>> prepFuture = ensemble.prepare(subject, n);
		
		try {
			final Promise<ByteBuffer> promise = prepFuture.get(timeout, timeunit);
			
			if (promise.getProposal() == null) {
				final Future<Proposal<ByteBuffer>> future = cache.get(subject);
				ensemble.propose(subject, promise.getN(), ByteBuffer.wrap(value.getBytes()));
				
				return future;
			}
			else {
				return new FutureFinished<Proposal<ByteBuffer>>(promise.getProposal());
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void learn(String instance, Proposal<ByteBuffer> v) {
		try {
			final ByteBuffer bb = v.getValue();
			final byte[] stringData = new byte[bb.limit()];
			bb.get(stringData, 0, bb.limit());
			LOG.info("Learned {} for {}.", new String(stringData), instance);
			
			cache.get(instance).complete(v);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}	
	}
}