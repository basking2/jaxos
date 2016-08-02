package org.sdsai.jaxos.dao;

import org.sdsai.jaxos.paxos.Proposal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * How the web interface will store learned facts.
 */
public class LearnerDao {
    final Map<String, Proposal<ByteBuffer>> map;

    public LearnerDao() {
        this.map = new HashMap<String, Proposal<ByteBuffer>>();
    }

    public void put(final String subject, final Proposal<ByteBuffer> proposal) throws IOException {
        map.put(subject, proposal);
    }

    public Proposal<ByteBuffer> get(final String subject) throws IOException {
        return map.get(subject);
    }
}
