package org.sdsai.jaxos.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.sdsai.jaxos.paxos.Acceptor;
import org.sdsai.jaxos.paxos.Proposer;

public class PrepareMessage extends BaseMessage {
    public final long proposalN;
    public PrepareMessage(final String instance, final long proposalN, final SocketAddress addr, final Protocol protocol) {
        super(PREPARE_MSG, instance, addr, protocol);
        this.proposalN = proposalN;
    }

    /**
     * The {@link Proposer} shall use this to send a prepare call to the remote server.
     *
     * The remote side will call {@link Acceptor#prepare(String, Long)} on this.
     */
    @Override
    public ByteBuffer encode() throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate(17);

        buffer.put(type); // Put 1 byte.
        buffer.putInt(instance.length()); // Put 8 bytes.
        buffer.putLong(proposalN); // Put 8 bytes.
        buffer.put(instance.getBytes());

        return buffer;
    }
}
