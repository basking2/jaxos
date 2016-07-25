package org.sdsai.jaxos.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.sdsai.jaxos.paxos.Promise;
import org.sdsai.jaxos.paxos.Proposal;

/**
 */
public class PromiseMessage extends BaseMessage {
    public final Promise<ByteBuffer> promise;
    public PromiseMessage(final String instance, final Promise<ByteBuffer> promise, final SocketAddress addr, final Protocol protocol) {
        super(PROMISE_MSG, instance, addr, protocol);
        this.promise = promise;
    }

    /**
     * @throws IOException
     */
    @Override
    public ByteBuffer encode() throws IOException
    {

        final ByteBuffer buffer;
        final Proposal<ByteBuffer> acceptedProposal = promise.getProposal();

        if (acceptedProposal == null) {
            buffer = ByteBuffer.allocate(25+ instance.getBytes().length);
            buffer.put(type);
            buffer.putInt(instance.getBytes().length);
            buffer.putLong(promise.getN());
            buffer.put(instance.getBytes());
            buffer.putLong(0L);
            buffer.putInt(0);
        } else {
            final ByteBuffer acceptedBuffer = acceptedProposal.getValue();
            buffer = ByteBuffer.allocate(25 + instance.getBytes().length + acceptedBuffer.limit());
            buffer.put(type);
            buffer.putInt(instance.getBytes().length);
            buffer.putLong(promise.getN());
            buffer.put(instance.getBytes());
            buffer.putLong(acceptedProposal.getN());
            buffer.putInt(acceptedBuffer.limit());
            acceptedBuffer.position(0);
            buffer.put(acceptedBuffer);
        }

        return buffer;
    }

}
