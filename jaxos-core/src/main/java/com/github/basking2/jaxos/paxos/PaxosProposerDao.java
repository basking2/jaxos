package com.github.basking2.jaxos.paxos;

/**
 * This stores the state of Proposers for all Paxos instances.
 *
 * The PaxosAcceptorDao will have a similar interface, but they should not
 * store and retreive the same data.
 *
 * Also, implementations of this may "forget" values after a suitable timeout.
 */
public interface PaxosProposerDao<V> {
    /**
     * @param instance
     * @param proposal
     */
    void storeProposal(String instance, Proposal<V> proposal);

    /**
     * @param instance
     * @return
     */
    Proposal<V> loadProposal(String instance);

    /**
     * Count that a promise has been received from an {@link Acceptor}.
     */
    Integer countPromise(String instance);

    /**
     * Set the count of a promise to 0.
     *
     * @param instance
     */
    void resetPromise(String instance);
}
