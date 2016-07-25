package org.sdsai.jaxos.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.sdsai.jaxos.paxos.Proposal;

/**
 */
public class ProposeMessage extends BaseMessage {
    public final Proposal<ByteBuffer> proposal;
    public ProposeMessage(final String instance, final Proposal<ByteBuffer> proposal, final SocketAddress addr, final Protocol protocol) {
        super(PROPOSE_MSG, instance, addr, protocol);
        this.proposal = proposal;
    }

    /**
     * @throws IOException
     */
    @Override
    public ByteBuffer encode() throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(17 + instance.getBytes().length + proposal.getValue().limit());
        buffer.put(type);
        buffer.putInt(instance.getBytes().length);
        buffer.putLong(proposal.getN());
        buffer.put(instance.getBytes());
        buffer.putInt(proposal.getValue().limit());
        proposal.getValue().position(0);
        buffer.put(proposal.getValue());

        return buffer;
    }

}
