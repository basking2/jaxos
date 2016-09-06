package com.github.basking2.jaxos.paxos;

import java.io.IOException;

/**
 * An acceptor. For normal paxos the value of N shoul be &gt; 1.
 * For multi paxos, which skips the prepare step, the value of N should be 0.
 */
public class Acceptor<V> {
    private Listener<V> listener;
    private PaxosAcceptorDao<V> dao;

    /**
     * @param listener When a value is accepted this listener informs learners
     *                 and takes any other necessary action.
     * @param dao How the acceptor persists data.
     * @throws IOException If loading the previous state fails.
     */
    public Acceptor(final Listener<V> listener, final PaxosAcceptorDao<V> dao) throws IOException {
        this.dao = dao;
        this.listener = listener;
    }

    public Acceptor(final Listener<V> listener) throws IOException {
        this(listener, null);
    }

	@SuppressWarnings("unchecked")
	public Promise<V> prepare(final String instance, final Long n) {

	    final Proposal<V> previouslyAccepted = dao.loadProposal(instance);

        // Send back that we've already accepted somthing.
        if (previouslyAccepted != null) {
            return new Promise<V>(n, previouslyAccepted);
        }

        // Send back that we haven't accepted anything, but we've promised to another proposer.
        Long promiseNumber = dao.loadPromise(instance);
        if ( n <= promiseNumber) {
            return Promise.DENIED;
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
		listener.accept(instance, proposal);    	
    }

    /**
     * @param proposal The proposal to accept.
     * @return True if the Acceptor should accept the proposal. False otherwise.
     */
    protected boolean acceptImpl(final Proposal<V> proposal) {
        return true;
    }

    @FunctionalInterface
    public static interface Listener<V> {
        void accept(final String instance, final Proposal<V> v);
    }

}
