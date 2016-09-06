package com.github.basking2.jaxos.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import com.github.basking2.jaxos.paxos.Promise;
import org.junit.Assert;
import org.junit.Test;
import com.github.basking2.jaxos.paxos.Proposal;

/**
 */
public class BaseMessageTest {

    final SocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), 3000);

    final Protocol protocol = null;

    public BaseMessageTest() throws UnknownHostException {
    }

    @Test
    public void testPrepare() throws IOException {
        final PrepareMessage msg = new PrepareMessage("hi", 2L, addr, protocol);
        final BaseMessage bm = BaseMessage.decode(msg.encode(), addr, protocol);
        Assert.assertTrue(bm instanceof PrepareMessage);
        final PrepareMessage msg2 = (PrepareMessage)bm;
        Assert.assertEquals("hi", msg2.instance);
        Assert.assertEquals(2, msg2.proposalN);
    }

    @Test
    public void testPromise() throws IOException {
        final byte[] bbarray = new byte[] { 1, 2, 3 };
        final ByteBuffer bb = ByteBuffer.wrap(bbarray);
        final PromiseMessage msg = new PromiseMessage("hi", new Promise<ByteBuffer>(3L, new Proposal<ByteBuffer>(4l, bb)), addr, protocol);
        final BaseMessage bm = BaseMessage.decode(msg.encode(), addr, protocol);
        Assert.assertTrue(bm instanceof PromiseMessage);
        final PromiseMessage msg2 = (PromiseMessage)bm;
        Assert.assertEquals("hi", msg2.instance);
        Assert.assertEquals((Long)3l, msg2.promise.getN());
        Assert.assertEquals((Long)4l, msg2.promise.getProposal().getN());
        bb.rewind();
        Assert.assertEquals(0, msg2.promise.getProposal().getValue().compareTo(bb));
    }

    @Test
    public void testPropose() throws IOException {
        final byte[] bbarray = new byte[] { 1, 2, 3 };
        final ByteBuffer bb = ByteBuffer.wrap(bbarray);
        final ProposeMessage msg = new ProposeMessage("hi", new Proposal<ByteBuffer>(4l, bb), addr, protocol);
        final BaseMessage bm = BaseMessage.decode(msg.encode(), addr, protocol);
        Assert.assertTrue(bm instanceof ProposeMessage);
        final ProposeMessage msg2 = (ProposeMessage)bm;
        Assert.assertEquals("hi", msg2.instance);
        Assert.assertEquals((Long)4l, msg2.proposal.getN());
        bb.rewind();
        Assert.assertEquals(0, msg2.proposal.getValue().compareTo(bb));
    }

    @Test
    public void testAccept() throws IOException {
        final byte[] bbarray = new byte[] { 1, 2, 3 };
        final ByteBuffer bb = ByteBuffer.wrap(bbarray);
        final AcceptMessage msg = new AcceptMessage("hi", new Proposal<ByteBuffer>(4l, bb), addr, protocol);
        final BaseMessage bm = BaseMessage.decode(msg.encode(), addr, protocol);
        Assert.assertTrue(bm instanceof AcceptMessage);
        final AcceptMessage msg2 = (AcceptMessage)bm;
        Assert.assertEquals("hi", msg2.instance);
        Assert.assertEquals((Long)4l, msg2.proposal.getN());
        bb.rewind();
        Assert.assertEquals(0, msg2.proposal.getValue().compareTo(bb));
    }
}
