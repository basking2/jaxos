package com.github.basking2.jaxos.paxos;

/**
 * {@link Acceptor} instances must remember what they promise and what they accept.
 */
public interface PaxosAcceptorDao<V> {

    /**
     * Store a proposal by an Acceptor.
     * @param instance
     * @param proposal
     */
    void storeProposal(String instance, Proposal<V> proposal);

    /**
     * Load a proposal by an Acceptor.
     * @param instance
     * @return The last proposal or null, if none.
     */
    Proposal<V> loadProposal(String instance);

    Long loadPromise(String instance);

    void storePromise(String instance, Long promise);

}
