# jaxos
Java paxos implementation for uses external to an application.

## Motivation

It is desirable that collective decisions be made over the public Internet related to private server deployments in separate data centers.

To serve this need it seems reasonable to isolate the Paxos algorithm into a discrete service that can have its security managed separately and offers encrypted and authenticated decision making assuming perfect forward secrecy.

## Bugs

1. Duplicated Accept messages can misinform a learner.
   Learners might benefit from optional logic to
   know the source of the learned information.

## Improvements

1. Allow for configuring Paxos to be MultiPaxos. That is, a single Learner and a single Proposer
   allows Phase 1 (pepare) to be skipped.
