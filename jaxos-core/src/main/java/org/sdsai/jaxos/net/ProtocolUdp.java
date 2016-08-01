package org.sdsai.jaxos.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.sdsai.jaxos.util.CipherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the network protocol for moving messages around.
 *
 * There are several types of messages.
 * <ol>
 * <li>Prepare - Sent by a proposer to an acceptor.</li>
 * <li>Promise - Set by an acceptor to a proposer.</li>
 * <li>Proposal - Sent by a proposer to an acceptor.</li>
 * <li>Accept - Sent by an acceptor to learners.</li>
 * </ol>
 *
 * The only state that this protocol layer maintains is prepares.
 */
public class ProtocolUdp extends AbstractProtocol {

	private final Logger LOG = LoggerFactory.getLogger(ProtocolUdp.class);
	private final DatagramChannel datagramChannel;
	private MessageHandler messageHandler;

	private final Thread receiver;

	/**
	 * 
	 * @param configuration This configuration is used to build description bits.
	 * @param acceptors The list of acceptors to consult.
	 * @param learners The list of the learners to consult.
	 * @throws IOException If binding the server socket fails.
	 */
	public ProtocolUdp(
			final InetSocketAddress bind,
			final Configuration configuration,
			final List<? extends SocketAddress> acceptors,
			final List<? extends SocketAddress> learners
			) throws IOException {
        super(bind, configuration, acceptors, learners);
		this.datagramChannel = DatagramChannel.open().bind(bind);
		this.datagramChannel.configureBlocking(false);
		this.datagramChannel.register(this.selector, SelectionKey.OP_READ);
		this.receiver = new Thread() {
			@Override
			public void run() {
				while (selector.isOpen()) {
					try {
						final int ready = selector.select();
						if (ready > 0) {
							final Set<SelectionKey> keys = selector.selectedKeys();
							for (final SelectionKey key : keys) {
								if (key.isReadable()) {
									if (key.channel() instanceof DatagramChannel) {
									    recv((DatagramChannel)key.channel());
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

	private void recv(final DatagramChannel dgc) {
	    try {
            final ByteBuffer cipherBuffer = ByteBuffer.allocate(29 + BaseMessage.MAX_DATA_SIZE);
            final SocketAddress addr = dgc.receive(cipherBuffer);
            final ByteBuffer buffer = cipherUtil.decrypt(cipherBuffer);
            LOG.info("Decoding msg from {} into handler.", addr);
            BaseMessage.decode(buffer, addr, ProtocolUdp.this, messageHandler);
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

	@Override
	public void close() throws Exception {
		selector.close();
		datagramChannel.close();
		receiver.join();
	}

	protected void send(final BaseMessage msg, final SocketAddress addr) throws IOException {
		final ByteBuffer buffer = msg.encode();
		final ByteBuffer encryptedBuffer = cipherUtil.encrypt(buffer);

		datagramChannel.send(encryptedBuffer, addr);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMessageHandler(final MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

}
