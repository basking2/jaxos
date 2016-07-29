package org.sdsai.jaxos.net;

import org.apache.commons.configuration.Configuration;
import org.sdsai.jaxos.util.CipherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 */
public class ProtocolTcp {
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
                                    final ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                                    final SocketChannel channel = serverChannel.accept();
                                    channel.configureBlocking(false);
                                    channel.register(selector, SelectionKey.OP_READ, new Msg());
                                }
                                else if (key.isReadable()) {
                                    if (key.channel() instanceof DatagramChannel) {
                                        final SocketChannel sc = (SocketChannel) key.channel();
                                        final Msg msg = (Msg)key.attachment();

                                        if (msg.length <= 0) {
                                            int read = sc.read(msg.buffer);
                                            if (read < 0) {
                                                sc.close();
                                            } else if (msg.buffer.position() >= 4){


                                                // FIXME
                                                // FIXME  - you are here.
                                                // FIXME
                                                msg.length = msg.buffer.getInt(0);
                                                // If we don't nave enough room in the current buffer, resize it.
                                                if (msg.length > msg.buffer.capacity() + 4) {
                                                    ByteBuffer old = msg.buffer;
                                                    msg.buffer = ByteBuffer.allocate(msg.length);
                                                    msg.buffer.put(old.array(), 4, old.position());
                                                }
                                            }
                                        }

                                        final ByteBuffer cipherBuffer = ByteBuffer.allocate(29 + BaseMessage.MAX_DATA_SIZE);
                                        final SocketAddress addr = dgc.receive(cipherBuffer);
                                        final ByteBuffer buffer = cipherUtil.decrypt(cipherBuffer);
                                        LOG.info("Decoding msg from {} into handler.", addr);
                                        BaseMessage.decode(buffer, addr, ProtocolTcp.this, messageHandler);
                                    }
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

    static class Msg {
        public ByteBuffer buffer = ByteBuffer.allocate(1024);
        public int length = -1;
    }
}
