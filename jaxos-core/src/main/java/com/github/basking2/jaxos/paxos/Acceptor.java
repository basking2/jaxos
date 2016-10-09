package com.github.basking2.jaxos.paxos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An acceptor. For normal paxos the value of N should be &gt; 1.
 * For multi paxos, which skips the prepare step, the value of N should be 0.
 */
public class Acceptor<V> {
    private AcceptHandler<V> acceptHandler;
    private PaxosAcceptorDao<V> dao;

    /**
     * @param dao How the acceptor persists data.
     * @throws IOException If loading the previous state fails.
     */
    public Acceptor(final AcceptHandler<V> acceptHandler, final PaxosAcceptorDao<V> dao) throws IOException {
        this.acceptHandler = acceptHandler;
        this.dao = dao;
    }

	@SuppressWarnings("unchecked")
	public Promise<V> prepare(final String instance, final Long n) {

	    final Proposal<V> previouslyAccepted = dao.loadProposal(instance);

        // Send back that we've already accepted something.
        if (previouslyAccepted != null) {
            return new Promise<V>(n, previouslyAccepted);
        }

        // Send back that we haven't accepted anything, but we've promised to another proposer.
        Long promiseNumber = dao.loadPromise(instance);
        if ( n <= promiseNumber) {
            return Promise.ALREADY_PROMISED_NOT_YET_ACCEPTED;
        }

    	dao.storePromise(instance, n);
    	
    	return new Promise<V>(n, null);
    }

    public void accept(final String instance, final Proposal<V> proposal) throws Exception {
    	assert(proposal != null);
    	
    	final Long myN = dao.loadPromise(instance);
    	
    	if (myN != null && myN.equals(proposal.getN())) {
    		if (acceptImpl(proposal)) {
        		doAccept(instance, proposal);
    		}
    	}
    }

    /**
     * When we accept a value, we store it.
     * @param instance
     * @param proposal
     * @throws Exception
     */
    private void doAccept(final String instance, final Proposal<V> proposal) throws Exception {
		dao.storeProposal(instance, proposal);

        acceptHandler.accept(instance, proposal);
    }

    /**
     * @param proposal The proposal to accept.
     * @return True if the Acceptor should accept the proposal. False otherwise.
     */
    protected boolean acceptImpl(final Proposal<V> proposal) {
        return true;
    }

    /**
     * When an acceptor accepts a proposal this must communicate that acceptance to learners.
     *
     * @param <V>
     */
    @FunctionalInterface
    public static interface AcceptHandler<V> {
        void accept(final String instance, final Proposal<V> v);
    }

}
