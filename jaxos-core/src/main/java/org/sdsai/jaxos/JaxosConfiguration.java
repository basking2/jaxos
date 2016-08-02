package org.sdsai.jaxos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.sdsai.jaxos.net.JaxosEnsemble;
import org.sdsai.jaxos.net.Protocol;
import org.sdsai.jaxos.net.ProtocolTcp;
import org.sdsai.jaxos.net.ProtocolUdp;
import org.sdsai.jaxos.paxos.Learner;
import org.sdsai.jaxos.paxos.PaxosAcceptorDao;
import org.sdsai.jaxos.paxos.PaxosProposerDao;
import org.sdsai.util.AppConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class JaxosConfiguration extends AppConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(JaxosConfiguration.class);

    public JaxosConfiguration() {
        super("jaxos");
    }

    public List<JaxosEnsemble> buildEnsembles(
    		final PaxosProposerDao<ByteBuffer> proposerDao,
    		final PaxosAcceptorDao<ByteBuffer> acceptorDao,
    		final Learner.Listener<ByteBuffer> learner
		) throws IOException {
    	
    	final List<JaxosEnsemble> ensembles = new ArrayList<JaxosEnsemble>();
    	
    	final String ensemblesStr = getString(name+".ensembles");
    	
    	if (ensemblesStr == null) {
    		return ensembles;
    	}

    	for (final String ensembleName : ensemblesStr.split("\\s*,\\s*")) {
    		final JaxosEnsemble ensemble = buildEnsemble(ensembleName, proposerDao, acceptorDao, learner);
    		if (ensemble != null) {
    			ensembles.add(ensemble);
    		}
    	}
    	    	
    	return ensembles;
    }
    
    public JaxosEnsemble buildEnsemble(
    		final String ensembleName,
    		final PaxosProposerDao<ByteBuffer> proposerDao,
    		final PaxosAcceptorDao<ByteBuffer> acceptorDao,
    		final Learner.Listener<ByteBuffer> learner
    	) throws IOException {
    	final JaxosEnsemble ensemble = new JaxosEnsemble(proposerDao, acceptorDao, learner);
    	
    	// Udp Acceptors, Learners and Bind.
    	final List<InetSocketAddress> udpAcceptors = protoAddresses(String.format("%s.ensemble.%s.acceptors", name, ensembleName), "udp");
    	final List<InetSocketAddress> udpLearners = protoAddresses(String.format("%s.ensemble.%s.learners", name, ensembleName), "udp");
    	final List<InetSocketAddress> udpBind = protoAddresses(String.format("%s.ensemble.%s.bind", name, ensembleName), "udp");

    	// Tcp Acceptors, Learners and Bind.
    	final List<InetSocketAddress> tcpAcceptors = protoAddresses(String.format("%s.ensemble.%s.acceptors", name, ensembleName), "tcp");
    	final List<InetSocketAddress> tcpLearners = protoAddresses(String.format("%s.ensemble.%s.learners", name, ensembleName), "tcp");
    	final List<InetSocketAddress> tcpBind = protoAddresses(String.format("%s.ensemble.%s.bind", name, ensembleName), "tcp");

    	String.format("%s.ensemble.%s.quorum", name, ensembleName);
    	String.format("%s.ensemble.%s.multi", name, ensembleName);
    	    	
    	// Build UDP Protocols
    	for (final InetSocketAddress bind : udpBind) {
    	    LOG.info("Binding udp to {}", bind);
    		Protocol protocol = new ProtocolUdp(bind, this, udpAcceptors, udpLearners);
    		ensemble.addProtocol(protocol);
    	}

		// Build TCP Protocols
		for (final InetSocketAddress bind : tcpBind) {
            LOG.info("Binding tcp to {}", bind);
			Protocol protocol = new ProtocolTcp(bind, this, tcpAcceptors, tcpLearners);
			ensemble.addProtocol(protocol);
		}

    	return ensemble;
    }
    
    
    public List<InetSocketAddress> protoAddresses(final String property, final String proto) {
    	final List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
        final String[] addressesStrings = getStringArray(property);

    	if (addressesStrings != null) {
    		for (final String addressString : addressesStrings) {
    			final String protoAddr[] = addressString.split("/", 2);
    			if (protoAddr.length == 2) {
    				if (protoAddr[0].equals(proto)) {
    					int colonIndex = protoAddr[1].lastIndexOf(':');
    					if (colonIndex != -1) {
    						try {
    							final Integer port = Integer.valueOf(protoAddr[1].substring(colonIndex+1));
    							final String hostname = protoAddr[1].substring(0, colonIndex);
    							addresses.add(new InetSocketAddress(hostname, port));
    						}
    						catch (final NumberFormatException e) {
    							// Nop
    						}
    					}
    				}
    			}
    		}
    	}
    	
    	return addresses;
    }

}
