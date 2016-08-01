package org.sdsai.jaxos.net;

import org.apache.commons.configuration.Configuration;
import org.sdsai.jaxos.util.CipherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.ArrayList;
import java.util.List;

/**
 */
public abstract class AbstractProtocol implements Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractProtocol.class);
    protected final CipherUtil cipherUtil;
    private List<? extends SocketAddress> learners;
    private List<? extends SocketAddress> acceptors;

    protected final Selector selector;

    protected AbstractProtocol(
        final InetSocketAddress bind,
        final Configuration configuration,
        final List<? extends SocketAddress> acceptors,
        final List<? extends SocketAddress> learners
    ) throws IOException {
        this.selector = (AbstractSelector)Selector.open();
        this.acceptors = acceptors;
        this.learners = learners;
        this.cipherUtil = new CipherUtil(configuration);
    }

    protected abstract void send(BaseMessage msg, SocketAddress addr) throws IOException;

    @Override
    public void close() throws Exception {
        if (selector.isOpen()) {
            selector.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendLearners(final AcceptMessage msg) throws IOException {
        for (final SocketAddress addr : learners) {
            LOG.info("Sending to learner {}", addr);
            send(msg, addr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAcceptors(BaseMessage msg) throws IOException {
        for (final SocketAddress addr : acceptors) {
            LOG.info("Sending to acceptor {}", addr);
            send(msg, addr);
        }
    }

    /**
     * {@inheritDoc}
     * @param n
     * @throws IOException
     */
    @Override
    public void sendPrepare(final String instance, Long n) throws IOException {
        sendAcceptors(new PrepareMessage(instance, n, null, this));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(BaseMessage msg) throws IOException {
        send(msg, msg.addr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numAcceptors() {
        return acceptors.size();
    }

}
