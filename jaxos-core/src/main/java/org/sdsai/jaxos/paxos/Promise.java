package org.sdsai.jaxos.paxos;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This represents a promise from an Acceptor in Paxos.
 *
 * A promise number shall never be 0 or lower. Such a number
 * in a Promise reply indicates an error from the Acceptor
 * and the Proposer should abandon its current proposal.
 */
public class Promise<V> implements Serializable {
    /**
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	public static final Promise DENIED = new Promise(-1L);

    private final Long n;
    private final Proposal<V> proposal;

    public Promise(final Long n, final Proposal<V> proposal) {
        this.n = n;
        this.proposal = proposal;
    }

    public Promise(final Long n) {
        this(n, null);
    }
    public Promise() {
        n = 0L;
        proposal = null;
    }

    public Long getN() {
        return n;
    }

    public Proposal<V> getProposal() {
        return proposal;
    }

    public static class PromiseComparator<V> implements Comparator<Promise<V>>
    {
        @Override
        public int compare(final Promise<V> o1, final Promise<V> o2) {
            if (o1 == o2) {
                return 0;
            }

            if (o1 == null) {
                return -1;
            }

            if (o2 == null) {
                return 1;
            }

            if (o1.getN() < o2.getN()) {
                return -1;
            }

            if (o1.getN() > o2.getN()) {
                return 1;
            }

            return 0;
        }
    }

    /**
     * Is this promise message a denial?
     *
     * If true, then there is no value for {@link #getN()} and null is returned by {@link #getProposal()}.
     *
     * @return
     */
    public boolean isDenied() {
        return n <= 0;
    }

    /**
     * Return a proposal with the number set to {@link #getProposal()}'s value, if not null or {@link #getN()}.
     * @param value The value for the proposal if there is no existing proposal.
     * @return A proposal to satisfy this promise.
     */
    public Proposal<V> toProposal(final V value) {
        if (proposal != null) {
            return new Proposal<V>(n, proposal.getValue());
        } else {
            return new Proposal<V>(n, value);
        }
    }
}
