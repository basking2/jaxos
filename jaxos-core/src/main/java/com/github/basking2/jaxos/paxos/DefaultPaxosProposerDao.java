package com.github.basking2.jaxos.paxos;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache that times out values.
 */
public class DefaultPaxosProposerDao<V> implements PaxosProposerDao<V> {

    final Cache<String, Proposal<V>> proposals;

    final LoadingCache<String, Integer> promises;

    public DefaultPaxosProposerDao(final long timeout, TimeUnit timeunit) {
        proposals = CacheBuilder.newBuilder().
                expireAfterWrite(timeout, timeunit).
                build();
        promises = CacheBuilder.newBuilder().
                expireAfterWrite(timeout, timeunit).
                build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) throws Exception {
                        return 0;
                    }
                });
    }

    @Override
    public void storeProposal(String instance, Proposal<V> proposal) {
        proposals.put(instance, proposal);
    }

    @Override
    public Proposal<V> loadProposal(String instance) {
        return proposals.getIfPresent(instance);
    }

    @Override
    public Integer countPromise(String instance) {
        try {
            Integer i = promises.get(instance) + 1;
            
            promises.put(instance, i);
            
            return i;
        } catch (ExecutionException e) {
            throw new RuntimeException("Unexpected error.", e);
        }
    }

    @Override
    public void resetPromise(String instance) {
        promises.put(instance, 0);
    }
}
