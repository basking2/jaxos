package com.github.basking2.jaxos.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.github.basking2.jaxos.util.FutureFailure;
import com.github.basking2.jaxos.paxos.Acceptor;
import com.github.basking2.jaxos.paxos.Learner;
import com.github.basking2.jaxos.paxos.PaxosAcceptorDao;
import com.github.basking2.jaxos.paxos.PaxosProposerDao;
import com.github.basking2.jaxos.paxos.Promise;
import com.github.basking2.jaxos.paxos.Proposal;
import com.github.basking2.jaxos.paxos.Proposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * A collection of Acceptors and Learners such that this Enseble can accept things.
 *
 * This sits above the protocol that will get messages between all endpoints.
 */
public class JaxosEnsemble implements AutoCloseable {

	final Logger LOG = LoggerFactory.getLogger(JaxosEnsemble.class);

	/**
	 * The wire protocol.
	 */
	private final List<Protocol> protocols;

	private Majority majority = SIMPLE_MAJORITY;

	private PaxosAcceptorDao<ByteBuffer> acceptorDao;
	private PaxosProposerDao<ByteBuffer> proposerDao;
	private Acceptor<ByteBuffer> acceptor;
	private Proposer<ByteBuffer> proposer;

	/**
	 * How are learned values communicated up to the application.
	 */
	private Learner.Listener<ByteBuffer> learnerListener;

	private Learner<ByteBuffer> learner;

	/**
	 * @param learner A built learner gets this.
	 */
	public JaxosEnsemble(
			PaxosProposerDao<ByteBuffer> proposerDao,
			PaxosAcceptorDao<ByteBuffer> acceptorDao,
			Learner.Listener<ByteBuffer> learner
			)
	{
		this.proposerDao = proposerDao;
		this.acceptorDao = acceptorDao;
		this.protocols = new ArrayList<Protocol>();
		this.acceptor = null;
		this.learnerListener = learner;

		setQuorum(0);
	}

	public void setQuorum() {
		setQuorum(majority.quorum(numAcceptors()));
	}

	private void setQuorum(final int quorum){

		this.learner = new Learner<ByteBuffer>(quorum, learnerListener);
		try {
		    // Build a new acceptor.
			this.acceptor = new Acceptor<ByteBuffer>(
                    (instance, proposal) ->{
                        allProtocols(protocol -> {
                            final AcceptMessage msg = new AcceptMessage(instance, proposal, null, protocol);
                            protocol.sendLearners(msg);
                        });
                    },
			        acceptorDao
            );

            // Build a proposer.
			this.proposer = new Proposer<ByteBuffer>(
					quorum,
                    // How do we send a prepare.
                    (instance, n) -> {
                        allProtocols(p -> {
                            p.sendPrepare(instance, n);
                        });
                    },
                    // How do we handle a promise.
                    (instance, promise) -> {
                        final CompletableFuture<Promise<ByteBuffer>> future = prepares.getIfPresent(instance);
                        if (future != null) {
                            future.complete(promise);
                        }
					},
					proposerDao);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public void addProtocol(final Protocol protocol) {
		this.protocols.add(protocol);
		protocol.setMessageHandler(new JaxosManagerMessageHandler());
		setQuorum();
	}

	private int numAcceptors() {
		return protocols.stream().map(p->p.numAcceptors()).reduce((x,y) -> x + y).orElse(0);
	}

	/**
	 * Set the way a quorum is calculated.
	 * 
	 * @param majority
	 */
	public void setMajority(final Majority majority) {
		this.majority = majority;
	}
	
	class JaxosManagerMessageHandler implements MessageHandler {

		@Override
		public void handlePrepare(final PrepareMessage msg) {
			try {
				final Promise<ByteBuffer> promise = acceptor.prepare(msg.instance, msg.proposalN);
				final PromiseMessage promiseMessage = new PromiseMessage(msg.instance, promise, msg.addr, msg.protocol);
				msg.protocol.send(promiseMessage, msg.addr);
			}
			catch (final IOException e) {
				LOG.error("Handling prepare.", e);
			}
		}

		@Override
		public void handlePromise(final PromiseMessage msg) {
			proposer.promise(msg.instance, msg.promise);
		}

		@Override
		public void handlePropose(final ProposeMessage msg) {
			try {
				acceptor.accept(msg.instance, msg.proposal);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}

		@Override
		public void handleAccept(final AcceptMessage msg) {
			LOG.info("Learned {}", msg);
			learner.learn(msg.instance, msg.proposal);
		}
	}

	private LoadingCache<String, CompletableFuture<Promise<ByteBuffer>>> prepares = CacheBuilder.newBuilder().
			expireAfterWrite(2, TimeUnit.MINUTES).
			removalListener(new RemovalListener<String, CompletableFuture<Promise<ByteBuffer>>>(){
				@Override
				public void onRemoval(RemovalNotification<String, CompletableFuture<Promise<ByteBuffer>>> notification) {
					notification.getValue().completeExceptionally(new Exception("Canceled."));
				}}).
			build(new CacheLoader<String, CompletableFuture<Promise<ByteBuffer>>>(){
				@Override
				public CompletableFuture<Promise<ByteBuffer>> load(String instance) throws Exception {
					CompletableFuture<Promise<ByteBuffer>> future = new CompletableFuture<Promise<ByteBuffer>>();
					return future;
				}});


	
	/**
	 * The user may request a promise to propose a value from our set of acceptors.
	 *
	 * @param instance The instance.
	 * @param n The value N of the proposal.
	 * @return A future that will yield a promise or an exception if it times out.
	 */
	public Future<Promise<ByteBuffer>> prepare(final String instance, final Long n) {
		try {
			final CompletableFuture<Promise<ByteBuffer>> future = prepares.get(instance);

			proposer.prepare(instance, n);
			
			return future;
		} catch (ExecutionException e) {
			LOG.error(e.getMessage(), e);
			return new FutureFailure<Promise<ByteBuffer>>(e);
		}
	}
	
	public void propose(final String instance, final Long n, final ByteBuffer data) {
		allProtocols(p -> {
			final ProposeMessage msg = new ProposeMessage(instance, new Proposal<ByteBuffer>(n, data), null, p);
			p.sendAcceptors(msg);
		});
	}

	public static interface Majority {
		int quorum(final int acceptors);
	}

	public static class PercentMajority implements Majority {
		final float percent;

		public PercentMajority(final float percent) {
			this.percent = percent;
		}

		@Override
		public int quorum(final int acceptors) {
			return (int)(acceptors * percent);
		}
	}

	public static final Majority ALL = new Majority() {
		@Override
		public int quorum(final int acceptors) {
			return acceptors;
		}
	};

	public static final Majority SIMPLE_MAJORITY = new Majority() {
		@Override
		public int quorum(final int acceptors) {
			return acceptors / 2 + 1;
		}
	};

	public static final Majority ONE = new Majority() {
		@Override
		public int quorum(final int acceptors) {
			return 1;
		}
	};

	private void allProtocols(IOConsumer c) {
		for (final Protocol protocol : protocols) {
			try {
				c.accept(protocol);
			}
			catch (final Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void close() throws Exception {
		allProtocols(p -> p.close());
	}

	@FunctionalInterface
	private static interface IOConsumer {
		void accept(Protocol protocol) throws Exception;
	}
}
