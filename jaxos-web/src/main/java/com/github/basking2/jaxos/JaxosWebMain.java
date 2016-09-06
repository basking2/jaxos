package com.github.basking2.jaxos;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.sdsai.jaxos.dao.LearnerDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 */
public class JaxosWebMain {
    public static final Logger LOG = LoggerFactory.getLogger(JaxosWebMain.class);

    public static void main(final String[] args) throws Exception {

        redirectLogging();

        startServer();
    }

    private static void startServer() throws Exception
    {
        final JaxosConfiguration config = new JaxosConfiguration();

        HttpServer server = new HttpServer();

        final LearnerDao learnerDao = new LearnerDao();
        final JaxosFacade jaxosClient = new JaxosFacade(config, learnerDao);

        server.getServerConfiguration().addHttpHandler(
                new JaxosApiHttpHandler(jaxosClient, learnerDao),
                HttpHandlerRegistration.builder().contextPath("/jaxos/api/v1").build()
        );

        server.getServerConfiguration().addHttpHandler(
                new JaxosUiHttpHandler(jaxosClient, learnerDao),
                HttpHandlerRegistration.builder().contextPath("/jaxos").build()
        );

        server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                LOG.info("Unhandled.");
                response.getOutputBuffer().write("Not found.");
                response.setStatus(HttpStatus.NOT_FOUND_404);
                response.finish();
            }
        });

        final Integer port = config.getInt("jaxos.web.port", 8080);
        final String bind = config.getString("jaxos.web.bind", "0.0.0.0");

        NetworkListener networkListener = new NetworkListener("jaxos", bind, port);

        networkListener.setTransport(nioTransportBuild());

        server.addListener(networkListener);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down.");
            server.shutdown();
        }));

        synchronized (server) {
            server.wait();
        }
    }

    private static TCPNIOTransport nioTransportBuild() {
        return TCPNIOTransportBuilder.newInstance().
                setKeepAlive(true).
                setTcpNoDelay(true).
                setIOStrategy(WorkerThreadIOStrategy.getInstance()).
                /*
                setSelectorThreadPoolConfig(
                        ThreadPoolConfig.defaultConfig().copy().setCorePoolSize(50).setMaxPoolSize(50).setQueueLimit(-1)
                ).
                setWorkerThreadPoolConfig(
                        ThreadPoolConfig.defaultConfig().copy().setCorePoolSize(50).setMaxPoolSize(50).setQueueLimit(-1)
                ).
                */
                build();
    }

    private static void redirectLogging() {
        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of your application
        SLF4JBridgeHandler.install();
    }
}
