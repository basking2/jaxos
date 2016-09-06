package com.github.basking2.jaxos.net;

import java.io.IOException;
import java.net.SocketAddress;

import org.sdsai.jaxos.paxos.*;

/**
 * This is the network layer for Jaxos.
 *
 * It is concerned with sending and receving messages.
 *
 * The Protocol object contains access to Listeners and Acceptors.
 */
public interface Protocol extends AutoCloseable {

    /**
     * Receive all network messages as they arrive to the given {@link MessageHandler}.
     *
     * @param messageHandler
     */
    void setMessageHandler(MessageHandler messageHandler);

    /**
     * A {@link Proposer} sends prepare messages to {@link Acceptor}s by this method.
     * 
     * @param instance What is to be decided.
     * @param n Any values other than 0 to number the proposal to be selected.
     * @throws IOException
     */
    void sendPrepare(String instance, Long n) throws IOException;

    /**
     * When an {@link Acceptor} learns a value it reports it to as many {@link Learner}s.
     * 
     * @param msg The proposal that has been accepted.
     * @throws IOException
     */
    void sendLearners(AcceptMessage msg) throws IOException;
    
    /**
     * Send a message to all {@link Acceptor}s. 
     * 
     * This is typically a {@link PrepareMessage} or {@link ProposeMessage} message.
     * @param msg
     * @throws IOException
     */
    void sendAcceptors(BaseMessage msg) throws IOException;
    
    /**
     * Send a message to the address contained in the message object.
     * 
     * @param msg
     * @throws IOException
     */
    void send(BaseMessage msg, SocketAddress addr) throws IOException;

    /**
     * Typically used for quorum calculations.
     *
     * @return
     */
    int numAcceptors();
}
