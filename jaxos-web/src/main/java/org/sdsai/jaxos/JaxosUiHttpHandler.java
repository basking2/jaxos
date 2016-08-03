package org.sdsai.jaxos;

import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.sdsai.jaxos.dao.LearnerDao;
import org.sdsai.jaxos.paxos.Proposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is more of the HTML end point.
 */
public class JaxosUiHttpHandler extends HttpHandler {
    public static final String PREFIX = "/html";

    private static final Logger LOG = LoggerFactory.getLogger(JaxosUiHttpHandler.class);

    final private JaxosFacade jaxosClient;
    final private LearnerDao learnerDao;
    final private int timeout = 2;
    final private TimeUnit timeunit = TimeUnit.MINUTES;

    public JaxosUiHttpHandler(final JaxosFacade jaxosClient, final LearnerDao learnerDao) {
        this.jaxosClient = jaxosClient;
        this.learnerDao = learnerDao;
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        String path = request.getHttpHandlerPath();

        if (request.getMethod() == Method.GET) {

            if ("".equals(path) || "/".equals(path)) {
                path = "/index.html";
            }

            if (path.contains("/../") || path.endsWith("/..")) {
                response.getNIOWriter().write("Invalid path element found.");
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                response.finish();
                return;
            }

            final InputStream in = JaxosUiHttpHandler.class.getResourceAsStream(PREFIX + path);

            if (in == null) {
                response.getNIOWriter().write("Resource not found: " + path);
                response.setStatus(HttpStatus.NOT_FOUND_404);
                response.finish();
                return;
            }

            if (path.endsWith(".css")) {
                response.setContentType("text/css");
            } else if (path.endsWith(".js")) {
                response.setContentType("application/javascript");
            } else {
                response.setContentType("text/html");
            }

            IOUtils.copy(in, response.getOutputStream());
            response.finish();
        }
        else {
            final String instance = request.getParameter("instance");
            final String value = request.getParameter("proposal");
            final String type = request.getParameter("type");

            if ("multipaxos".equals(type)) {
                final Future<Proposal<ByteBuffer>> v = jaxosClient.mutiPaxos(instance, value, timeout, timeunit);
                serveFutureProposal(request, response, v);
            }
            else if ("paxos".equals(type)) {
                final Future<Proposal<ByteBuffer>> v = jaxosClient.paxos(instance, value, timeout, timeunit);
                serveFutureProposal(request, response, v);
            }
        }
    }

    private void serveFutureProposal(
            final Request request,
            final Response response,
            final Future<Proposal<ByteBuffer>> future
    ) {
        response.suspend();

        try {
            final Proposal<ByteBuffer> proposal = future.get(timeout, timeunit);
            response.setContentType("text/plain");
            response.getNIOOutputStream().write(proposal.getValue().array(), 0, proposal.getValue().limit());
        } catch (final IOException | InterruptedException | ExecutionException | TimeoutException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            try {
                response.getNIOWriter().write(e.getMessage());
            } catch (IOException e1) {
                // nop
            }
            LOG.error("waiting", e);
        }

        response.resume();
    }
}
