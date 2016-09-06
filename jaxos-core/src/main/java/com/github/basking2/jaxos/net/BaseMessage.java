package com.github.basking2.jaxos.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.github.basking2.jaxos.paxos.Promise;
import com.github.basking2.jaxos.paxos.Proposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic message of how Paxos is used.
 */
public abstract class BaseMessage {
	
	final static private Logger LOG = LoggerFactory.getLogger(BaseMessage.class);

    /**
     * Prepare message type.
     */
    public static final byte PREPARE_MSG = 1;

    /**
     * Propose message type. PDU = (type, id, proposal number, datalen, data bytes...).
     */
    public static final byte PROPOSE_MSG = 2;

    /**
     * Promise message type. PDU = (type, id, proposal number, accepted proposal number, datalen, data bytes...)
     */
    public static final byte PROMISE_MSG = 3;

    /**
     * Accept message type. PDU = (type, id, proposal number, datalen, data bytes...)
     */
    public static final byte ACCEPT_MSG = 4;

    /**
     * The largest message size a user may send, in bytes..
     */
    public static final int MAX_DATA_SIZE = 65000;

    /**
     * The type of class child class.
     */
    public final byte type;

    /**
     * An ID to specify which paxos instance should be interacted with.
     */
    public final String instance;

    /**
     * The source address. This is only necessary for datagram protocols, but can be defined for streaming protocols.
     */
    public final SocketAddress addr;

    /**
     * The {@link Protocol} object this was received on or null.
     */
    public final Protocol protocol;

    protected BaseMessage(final byte type, final String instance, final SocketAddress addr, final Protocol protocol) {
        this.type = type;
        this.instance = instance;
        this.addr = addr;
        this.protocol = protocol;
    }

    public abstract ByteBuffer encode() throws IOException;

    /**
     * @param buffer The buffer to decode.
     * @param addr The return address of the system that created this.
     * @param handler A handler to react to the produced message.
     * @throws IOException
     */
    public static void decode(
            final ByteBuffer buffer,
            final SocketAddress addr,
            final Protocol protocol,
            final MessageHandler handler
    ) throws IOException {
        final long bytesRead = buffer.limit();
        if (bytesRead < 13) {
            throw new IOException("Packet was too small to be a message.");
        }

        buffer.rewind();
        final byte type = buffer.get(); // Get 1 byte.
        final int instanceLen = buffer.getInt(); // Get 4 bytes.
        final long proposalN = buffer.getLong(); // Get 8 bytes.
        final byte[] instanceBytes = new byte[instanceLen];
        buffer.get(instanceBytes);
        final String instance = new String(instanceBytes);

        switch (type) {
            case PREPARE_MSG:
            	LOG.info("Decoding prepare: {}", proposalN);
                handler.handlePrepare(new PrepareMessage(instance, proposalN, addr, protocol));
                break;
            case PROMISE_MSG: {
            	LOG.info("Decoding promise.");
                final long accepted_proposal = buffer.getLong();
                final int datalen = buffer.getInt();

                // Insanity check.
                if (datalen != buffer.limit() - buffer.position()) {
                    throw new IOException("Datalength does not match buffer limit.");
                }
                
                if (accepted_proposal == 0) {
                    handler.handlePromise(new PromiseMessage(instance, new Promise<ByteBuffer>(proposalN), addr, protocol));
                } else {
                    final Proposal<ByteBuffer> proposal = new Proposal<ByteBuffer>(accepted_proposal, buffer.slice());
                    final Promise<ByteBuffer> promise = new Promise<ByteBuffer>(proposalN, proposal);
                    handler.handlePromise(new PromiseMessage(instance, promise, addr, protocol));
                }
                break;
            }
            case PROPOSE_MSG: {
            	LOG.info("Decoding propose.");
                final int datalen = buffer.getInt();

                // Insanity check.
                if (datalen != buffer.limit() - buffer.position()) {
                    throw new IOException("Data length does not match buffer limit.");
                }

                final Proposal<ByteBuffer> proposal = new Proposal<ByteBuffer>(proposalN, buffer.slice());
                handler.handlePropose(new ProposeMessage(instance, proposal, addr, protocol));
                break;
            }
            case ACCEPT_MSG: {
            	LOG.info("Decoding accept.");
                final int datalen = buffer.getInt();

                // Insanity check.
                if (datalen != buffer.limit() - buffer.position()) {
                    throw new IOException("Data length does not match buffer limit.");
                }

                Proposal<ByteBuffer> proposal = new Proposal<ByteBuffer>(proposalN, buffer.slice());
                handler.handleAccept(new AcceptMessage(instance, proposal, addr, protocol));
                break;
            }
            default:
                throw new IOException("Usupported message type: " + type);
        }
    }

    public static BaseMessage decode(
            final ByteBuffer buffer,
            final SocketAddress addr,
            final Protocol protocol
    ) throws IOException {
        final BaseMessage[] msg = new BaseMessage[1];

        decode(buffer, addr, protocol, new MessageHandler(){
            @Override public void handlePrepare(final PrepareMessage m) { msg[0] = m; }
            @Override public void handlePromise(final PromiseMessage m) { msg[0] = m; }
            @Override public void handlePropose(final ProposeMessage m) { msg[0] = m; }
            @Override public void handleAccept(final AcceptMessage m) { msg[0] = m; }
        });

        return msg[0];
    }
}