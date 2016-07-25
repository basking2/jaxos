package org.sdsai.jaxos.paxos;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 */
public class DefaultPaxosAcceptorDao<V> implements PaxosAcceptorDao<V> {

    private Cache<String, Proposal<V>> proposals;
    private LoadingCache<String, Long> promises;

    public DefaultPaxosAcceptorDao(final long timeout, final TimeUnit timeunit) {
        proposals = CacheBuilder.newBuilder().
                expireAfterWrite(timeout, timeunit).
                build();
        promises = CacheBuilder.newBuilder().
                expireAfterWrite(timeout, timeunit).
                build(new CacheLoader<String, Long>() {
                    @Override
                    public Long load(String key) throws Exception {
                        return 0L;
                    }
                });
    }

    /**
     * Store a proposal by an Acceptor.
     * @param instance
     * @param proposal
     */
    @Override
    public void storeProposal(String instance, final Proposal<V> proposal) {
        proposals.put(instance, proposal);
    }

    /**
     * Load a proposal by an Acceptor.
     * @param instance
     * @return The last proposal or null, if none.
     */
    public Proposal<V> loadProposal(String instance) {
        return proposals.getIfPresent(instance);
    }

    @Override
    public Long loadPromise(String instance) {
    	try {
			return promises.get(instance);
		} catch (ExecutionException e) {
			throw new RuntimeException("failed to initilize promises w/ 0 constant.", e);
		}
    }

    @Override
    public void storePromise(String instance, Long promise) {
    	promises.put(instance, promise);
    }
}
