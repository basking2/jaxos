package com.github.basking2.jaxos.net;

import com.github.basking2.jaxos.JaxosConfiguration;
import com.github.basking2.jaxos.paxos.Proposal;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class ProtocolTcpTest {
    @Test
    public void sendAcceptTest() throws Exception {

        List<SocketAddress> acceptors = new ArrayList<SocketAddress>();
        List<SocketAddress> learners = new ArrayList<SocketAddress>();
        final InetSocketAddress addr = new InetSocketAddress("::1", 3001);
        acceptors.add(addr);
        learners.add(addr);
        final Protocol p = new ProtocolTcp(addr, new JaxosConfiguration(), acceptors, learners);

        final Object lock = new Object();
        final int result[] = new int[1];

        synchronized (lock) {

            p.setMessageHandler(new MessageHandler() {
                @Override
                public void handlePrepare(PrepareMessage msg) {
                    synchronized (lock) {
                        result[0] = msg.type;
                        lock.notifyAll();
                    }
                }

                @Override
                public void handlePromise(PromiseMessage msg) {
                    synchronized (lock) {
                        result[0] = msg.type;
                        lock.notifyAll();
                    }
                }

                @Override
                public void handlePropose(ProposeMessage msg) {
                    synchronized (lock) {
                        result[0] = msg.type;
                        lock.notifyAll();
                    }
                }

                @Override
                public void handleAccept(AcceptMessage msg) {
                    synchronized (lock) {
                        result[0] = msg.type;
                        lock.notifyAll();
                    }
                }
            });

            p.send(new AcceptMessage("a", new Proposal<ByteBuffer>(0L, ByteBuffer.allocate(0)), addr, p), addr);

            lock.wait();
        }

        Assert.assertEquals(4, result[0]);

    }
}
