package org.sdsai.jaxos;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
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

    private static void startServer() throws Exception {
        HttpServer server = new HttpServer();

        server.getServerConfiguration().addHttpHandler(
            new JaxosHttpHandler(),
            HttpHandlerRegistration.builder().contextPath("/jaxos/api/v1").build()
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

        server.addListener(new NetworkListener("jaxos", "0.0.0.0", 8080));
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            LOG.info("Shutting down.");
            server.shutdown();
        }));

        synchronized(server) {
            server.wait();
        }
    }

    private static void redirectLogging() {
        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of your application
        SLF4JBridgeHandler.install();
    }
}