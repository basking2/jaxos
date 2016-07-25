package org.sdsai.jaxos.paxos;

/**
 */
public class Proposer<V> {
    private final int quorum;

    private PaxosProposerDao<V> dao;
    private Preparer preparer;
    private PromiseHandler<V> promiser;

    /**
     * 
     * @param quorum How many Acceptors to require promises from before proposing.
     * @param preparer How to communicate with instances.
     * @param promiser How to 
     * @param dao
     */
    public Proposer(
        final int quorum,
        final Preparer preparer,
        final PromiseHandler<V> promiser,
        final PaxosProposerDao<V> dao
    ) {
        this.quorum = quorum;
        this.preparer = preparer;
        this.promiser = promiser;
        this.dao = dao;
    }

    /**
     * Choose a value by gathering promises, and perhaps issuing a proposal.
     *
     * @return The accepted proposal. The value may be different than what we submit.
     */
    public void prepare(final String instance, final Long n) {

        dao.resetPromise(instance);

        // Phase 1 - Propose and wait for promises to come back.
        preparer.prepareAll(instance, n);
    }

    /**
     * Called when a {@link Acceptor} response to a prepare call.
     * @param newPromise
     * @throws Exception
     */
    public void promise(final String instance, final Promise<V> newPromise) {

        int votes = dao.countPromise(instance);

        // Load any proposal we've been told was accepted. Typically NULL with no contention.
        Proposal<V> proposal = dao.loadProposal(instance);

        // A proposal was already selected by the acceptor, incorporate it.
        if (newPromise.getProposal() != null) {
            if (proposal == null || proposal.getN() < newPromise.getProposal().getN()) {
                proposal = newPromise.getProposal();
                dao.storeProposal(instance, proposal);
            }
        }

        if (votes == quorum) {
            promiser.promise(instance, new Promise<V>(newPromise.getN(), proposal));
        }
    }
    
    /**
     * Handle a {@link Promise} when it has been made by the majority of {@link Acceptor} nodes.
     */
    @FunctionalInterface
    public static interface PromiseHandler<V> {
        /**
         * Handle a {@link Promise} when it has been made by the majority of {@link Acceptor} nodes.
         * @param instance The instance.
         * @param promise The promise.
         */
        void promise(String instance, Promise<V> promise);
    }
    
    /**
     * Performs the act of asking all {@link Acceptor} s to promise to accept a {@link Proposal}.
     */
    @FunctionalInterface
    public static interface Preparer {
        /**
         * Propose to all {@link Acceptor} instances.
         *
         * This will result in the {@link Proposer} receiving {@link Promise}s from the
         * {@link Acceptor}. The {@link Promise} will either comfirm
         * that the number n was accepted and the {@link Acceptor} will not accept any
         * proposal with a number lower than n, or it will contains
         * an already accepted {@link Proposal}.
         *
         * @param n
         */
        void prepareAll(String instance, Long n);
    }

}
