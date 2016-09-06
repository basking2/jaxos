package com.github.basking2.jaxos.net;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.*;

/**
 */
public class ProtocolTcp extends AbstractProtocol {
    private final Logger LOG = LoggerFactory.getLogger(ProtocolTcp.class);
    private final ServerSocketChannel socketChannel;
    private MessageHandler messageHandler;
    private ConnectionsMap connections;
    private final Thread receiver;

    public ProtocolTcp(
            final InetSocketAddress bind,
            final Configuration configuration,
            final List<? extends SocketAddress> acceptors,
            final List<? extends SocketAddress> learners
    ) throws IOException {
        super(bind, configuration, acceptors, learners);

        this.socketChannel = ServerSocketChannel.open().bind(bind);
        this.socketChannel.configureBlocking(false);
        this.socketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        this.connections = new ConnectionsMap();
        this.receiver = new Thread() {
            @Override
            public void run() {
                while (selector.isOpen()) {
                    try {
                        final int ready = selector.select();
                        if (ready > 0) {
                            final Set<SelectionKey> keys = selector.selectedKeys();
                            for (final SelectionKey key : keys) {
                                if (key.isAcceptable()) {
                                    try {
                                        final ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                                        final SocketChannel channel = serverChannel.accept();
                                        LOG.info("Accepted connection {}", channel.getRemoteAddress());
                                        connections.put(channel.getRemoteAddress(), channel);
                                    } catch (final IOException e) {
                                        LOG.error("Accepting connection", e);
                                    }
                                }
                                else if (key.isReadable()) {
                                    final SocketChannel sc = (SocketChannel) key.channel();
                                    final KeyAttachment msg = (KeyAttachment)key.attachment();
                                    recv(sc, msg);
                                }
                            }

                            keys.clear();
                        }
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        };
        this.receiver.setDaemon(true);
        this.receiver.start();
    }

    private void populateMsg(final SocketChannel sc, final KeyAttachment msg) throws IOException {
        // We need to get the message length.
        if (msg.length <= 0) {
            final int read = sc.read(msg.buffer);
            if (read < 0) {
                // Receive error.
                LOG.error("Read negative length. Closing connection to {}.", sc.getRemoteAddress());
                sc.close();
            } else if (msg.buffer.position() >= 4){
                msg.length = msg.buffer.getInt(0);

                // If we don't nave enough room in the current buffer, resize it.
                if (msg.length > msg.buffer.capacity()) {
                    ByteBuffer old = msg.buffer;
                    msg.buffer = ByteBuffer.allocate(msg.length);
                    old.rewind();
                    msg.buffer.put(old);
                } else {
                    msg.buffer.limit(msg.length);
                }
            }
        }
        // We have the length. Just read into the buffer until we have the data.
        else if (msg.buffer.position() != msg.length){
            final int read = sc.read(msg.buffer);
            if (read < 0) {
                sc.close();
            }
        }
    }

    private void processMsg(final SocketChannel sc, final KeyAttachment msg) throws IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException, InvalidKeyException, InvalidKeySpecException {
        if (msg.buffer.remaining() == 0) {
            final SocketAddress addr = sc.getRemoteAddress();
            final byte[] buffer = cipherUtil.decrypt(msg.buffer.array(), 4, msg.length-4);
            final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            BaseMessage.decode(byteBuffer, addr, this, messageHandler);
            msg.reset();
        }
    }

    private void recv(final SocketChannel sc, final KeyAttachment msg) {
        try {
            populateMsg(sc, msg);
            processMsg(sc, msg);
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void send(final BaseMessage msg, final SocketAddress addr) throws IOException {

        final byte[] encryptedData;
        try {
            final ByteBuffer buffer = msg.encode();
            encryptedData = cipherUtil.encrypt(buffer.array(), 0, buffer.position());
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException| BadPaddingException| IllegalBlockSizeException| InvalidParameterSpecException| InvalidAlgorithmParameterException e) {
            throw new IOException("Encrypting message", e);
        }

        final SocketChannel chan = connections.get(addr);

        final ByteBuffer marshalled = ByteBuffer.allocate(4 + encryptedData.length);
        marshalled.putInt(4+encryptedData.length).put(encryptedData).flip();
        chan.write(marshalled);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        super.close();

        if (socketChannel.isOpen()) {
            socketChannel.close();
        }

        receiver.join();
    }

    static class KeyAttachment {
        public ByteBuffer buffer;

        /**
         * The length of the whole message, including the 4 bytes to store this integer.
         *
         * Thus the minimum value of a valid message is 4.
         */
        public int length;

        KeyAttachment() {
            reset();
        }

        public void reset() {
            buffer = ByteBuffer.allocate(1024);
            length = -1;
        }
    }


    private class ConnectionsMap {
        private Map<SocketAddress, SocketChannel> connections = new HashMap<SocketAddress, SocketChannel>();

        public boolean containsKey(final SocketAddress addr) {
            synchronized (connections) {
                return connections.containsKey(addr);
            }
        }

        public void put(final SocketAddress addr, final SocketChannel chan) throws IOException {
            synchronized(connections) {
                chan.configureBlocking(false);
                chan.register(selector, SelectionKey.OP_READ, new KeyAttachment());
                final SocketChannel other = connections.put(addr, chan);
                if (other != null) {
                    other.close();
                }
            }
        }

        public SocketChannel get(final SocketAddress addr) throws IOException {
            synchronized (connections) {
                final SocketChannel chan;
                if (connections.containsKey(addr)) {
                    chan = connections.get(addr);
                } else {
                    chan = SocketChannel.open(addr);
                    chan.configureBlocking(false);
                    chan.register(selector, SelectionKey.OP_READ, new KeyAttachment());
                    final SocketChannel other = connections.put(addr, chan);

                    try {
                        if (other != null) {
                            other.close();
                        }
                    } catch (IOException e) {
                        // Nop.
                    }

                }

                return chan;
            }
        }
    }
}
