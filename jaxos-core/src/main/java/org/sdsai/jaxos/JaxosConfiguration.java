package org.sdsai.jaxos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.sdsai.jaxos.net.JaxosEnsemble;
import org.sdsai.jaxos.net.Protocol;
import org.sdsai.jaxos.net.ProtocolUdp;
import org.sdsai.jaxos.paxos.Learner;
import org.sdsai.jaxos.paxos.PaxosAcceptorDao;
import org.sdsai.jaxos.paxos.PaxosProposerDao;

/**
 */
public class JaxosConfiguration extends CompositeConfiguration {
    public JaxosConfiguration() {
        // Add system properties.
        addConfiguration(new SystemConfiguration());

        // Load classpath properties.
        try {
            final PropertiesConfiguration classpathProperties = new PropertiesConfiguration();
            classpathProperties.load(this.getClass().getResourceAsStream("/jaxos.properties"));
            addConfiguration(classpathProperties);
        } catch (final ConfigurationException e) {
            // Nop.
        }

        // Load an optional properties file.
        try {
            final File jaxosProperties = new File("jaxos.properties");
            if (jaxosProperties.canRead()) {
                final PropertiesConfiguration fileProperties = new PropertiesConfiguration();

                try (final InputStream is = new FileInputStream(jaxosProperties)) {
                    fileProperties.load(is);
                }
                catch (final IOException e ){
                    // Nop.
                }

                addConfiguration(fileProperties);
            }
        }
        catch (final ConfigurationException e) {
            // Nop.
        }
    }

    public List<JaxosEnsemble> buildEnsembles(
    		final PaxosProposerDao<ByteBuffer> proposerDao,
    		final PaxosAcceptorDao<ByteBuffer> acceptorDao,
    		final Learner.Listener<ByteBuffer> learner
		) throws IOException {
    	
    	final List<JaxosEnsemble> ensembles = new ArrayList<JaxosEnsemble>();
    	
    	final String ensemblesStr = getString("jaxos.ensembles");
    	
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
    	final List<InetSocketAddress> udpAcceptors = protoAddresses(String.format("jaxos.ensemble.%s.acceptors", ensembleName), "udp");
    	final List<InetSocketAddress> udpLearners = protoAddresses(String.format("jaxos.ensemble.%s.learners", ensembleName), "udp");
    	final List<InetSocketAddress> udpBind = protoAddresses(String.format("jaxos.ensemble.%s.bind", ensembleName), "udp");

    	// Tcp Acceptors, Learners and Bind.
    	final List<InetSocketAddress> tcpAcceptors = protoAddresses(String.format("jaxos.ensemble.%s.acceptors", ensembleName), "tcp");
    	final List<InetSocketAddress> tcpLearners = protoAddresses(String.format("jaxos.ensemble.%s.learners", ensembleName), "tcp");
    	final List<InetSocketAddress> tcpBind = protoAddresses(String.format("jaxos.ensemble.%s.bind", ensembleName), "tcp");

    	String.format("jaxos.ensemble.%s.quorum", ensembleName);
    	String.format("jaxos.ensemble.%s.multi", ensembleName);
    	    	
    	// Build UDP Protocols
    	for (final InetSocketAddress bind : udpBind) {
    		Protocol protocol = new ProtocolUdp(bind, this, udpAcceptors, udpLearners);
    		ensemble.addProtocol(protocol);
    	}

    	return ensemble;
    }
    
    
    public List<InetSocketAddress> protoAddresses(final String property, final String proto) {
    	final List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
    	final String addressesString = getString(property);

    	if (addressesString != null) {
    		for (final String addressString : addressesString.split("\\s*,\\s*")) {
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
