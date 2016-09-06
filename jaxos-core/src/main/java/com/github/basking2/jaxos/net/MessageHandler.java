package com.github.basking2.jaxos.net;

/**
 * Used by {@link BaseMessage} to dispatch message types.
 */
public interface MessageHandler {
    void handlePrepare(PrepareMessage msg);
    void handlePromise(PromiseMessage msg);
    void handlePropose(ProposeMessage msg);
    void handleAccept(AcceptMessage msg);
}
