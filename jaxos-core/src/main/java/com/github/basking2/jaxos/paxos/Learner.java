package com.github.basking2.jaxos.paxos;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class Learner<V> {
    private final int quorum;

    private final Map<Proposal<V>, Long> values = new HashMap<Proposal<V>, Long>();

    // The eventual winner.
    Proposal<V> winner = null;

    // Listener to call when a winner is decided.
    Listener<V> listener = null;

    public Learner(final int quorum, final Listener<V> listener) {
        this.quorum = quorum;
        this.listener = listener;
    }

    public Learner(final int quorum) {
        this(quorum, null);
    }

    /**
     * @param p
     * @return True when a value is set.
     */
    public boolean learn(final String instance, final Proposal<V> p) {
        // Get the votes count.
        Long votes = values.get(p.getValue());
        if (votes == null) {
            votes = 0L;
        }
        
        votes = votes + 1;

        // Increment it.
        values.put(p, votes);

        // Check if we have a winner. Use == because we only want to signal once.
        if (votes == quorum) {

            winner = p;

            final Listener<V> listener = this.listener;
            if (listener != null) {
                listener.learn(instance, p);
            }

            return true;
        }

        return false;
    }

    /**
     * @param listener A listener or null.
     */
    public void setLearnerListener(final Listener<V> listener) {
        this.listener = listener;
    }

    /**
     * @return Null or the value in the case of a winning decision.
     */
    public V getValue() {
        return winner == null? null : winner.getValue();
    }
    
    /**
     * When a {@link Learner} learns a value this may be triggered.
     */
    @FunctionalInterface
    public static interface Listener<V> {
        void learn(final String instance, final Proposal<V> v);
    }

}
