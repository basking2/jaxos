package com.github.basking2.jaxos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.basking2.jaxos.net.JaxosEnsemble;
import com.github.basking2.jaxos.paxos.DefaultPaxosAcceptorDao;
import com.github.basking2.jaxos.paxos.DefaultPaxosProposerDao;
import com.github.basking2.jaxos.paxos.Learner;
import com.github.basking2.jaxos.paxos.Proposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxosMain {
	
	private static final Logger LOG = LoggerFactory.getLogger(JaxosMain.class);

	private static final Object waitToExit = new Object();
	
	private static long timeout = 2;
	private static TimeUnit timeunit = TimeUnit.MINUTES;
	

	public static void main(final String[] args) throws IOException {
		final JaxosConfiguration config = new JaxosConfiguration();

		final List<JaxosEnsemble> ensembles = config.buildEnsembles(
				new DefaultPaxosProposerDao<ByteBuffer>(timeout, timeunit),
				new DefaultPaxosAcceptorDao<ByteBuffer>(timeout, timeunit),
				new Learner.Listener<ByteBuffer>() {

					@Override
					public void learn(String instance, Proposal<ByteBuffer> v) {
						System.out.println("LEARNED "+v.getN());
					}

				}
				);
		
		Runtime.getRuntime().addShutdownHook(new Thread( ()-> {
			for (final JaxosEnsemble ensemble: ensembles) {
				try {
				    LOG.info("Closing ensemble "+ensemble);
					ensemble.close();
				}
				catch (final Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}

			synchronized (waitToExit) {
				waitToExit.notifyAll();
			}
		}));

		// Sleep until exit.
		synchronized (waitToExit) {
			while (true) {
				try {
					waitToExit.wait();
				} catch (InterruptedException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}
}
