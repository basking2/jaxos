package org.sdsai.jaxos.net;

import org.apache.commons.configuration.Configuration;
import org.sdsai.jaxos.util.CipherUtil;
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
import java.nio.channels.spi.AbstractSelector;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 */
public class ProtocolTcp implements Protocol {
    private final Logger LOG = LoggerFactory.getLogger(ProtocolTcp.class);
    private final CipherUtil cipherUtil;
    private final Selector selector;
    private final ServerSocketChannel socketChannel;
    private MessageHandler messageHandler;
    private List<? extends SocketAddress> learners = new ArrayList<SocketAddress>();
    private List<? extends SocketAddress> acceptors = new ArrayList<SocketAddress>();

    private final Thread receiver;

    public ProtocolTcp(
            final InetSocketAddress bind,
            final Configuration configuration,
            final List<? extends SocketAddress> acceptors,
            final List<? extends SocketAddress> learners
    ) throws IOException {
        this.acceptors = acceptors;
        this.learners = learners;
        this.cipherUtil = new CipherUtil(configuration);
        this.selector = (AbstractSelector)Selector.open();
        this.socketChannel = ServerSocketChannel.open().bind(bind);
        this.socketChannel.configureBlocking(false);
        this.socketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
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
                                        channel.configureBlocking(false);
                                        channel.register(selector, SelectionKey.OP_READ, new KeyAttachment());
                                    } catch (final IOException e) {
                                        LOG.error("Accepting connection", e);
                                    }
                                }
                                else if (key.isReadable() && key.channel() instanceof SocketChannel) {
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
                sc.close();
            } else if (msg.buffer.position() >= 4){
                msg.length = msg.buffer.getInt(0);

                // If we don't nave enough room in the current buffer, resize it.
                if (msg.length > msg.buffer.capacity()) {
                    ByteBuffer old = msg.buffer;
                    msg.buffer = ByteBuffer.allocate(msg.length);
                    old.rewind();
                    msg.buffer.put(old);
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
            final ByteBuffer cipherBuffer = ByteBuffer.allocate(29 + BaseMessage.MAX_DATA_SIZE);
            final SocketAddress addr = sc.getRemoteAddress();
            final byte[] buffer = cipherUtil.decrypt(msg.buffer.array(), 4, msg.length-4);
            final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            BaseMessage.decode(byteBuffer, addr, ProtocolTcp.this, messageHandler);
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

    @Override
    public void setMessageHandler(MessageHandler messageHandler) {

    }

    @Override
    public void sendPrepare(String instance, Long n) throws IOException {

    }

    @Override
    public void sendLearners(BaseMessage msg) throws IOException {

    }

    @Override
    public void sendAcceptors(BaseMessage msg) throws IOException {

    }

    @Override
    public void send(BaseMessage msg) throws IOException {

    }

    @Override
    public int numAcceptors() {
        return acceptors.size();
    }

    @Override
    public void close() throws Exception {
        if (selector.isOpen()) {
            selector.close();
        }

        if (socketChannel.isOpen()) {
            socketChannel.close();
        }
    }

    static class KeyAttachment {
        public ByteBuffer buffer = ByteBuffer.allocate(1024);
        /**
         * The length of the whole message, including the 4 bytes to store this integer.
         *
         * Thus the minimum value of a valid message is 4.
         */
        public int length = -1;
    }
}
