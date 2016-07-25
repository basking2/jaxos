package org.sdsai.jaxos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.sdsai.jaxos.paxos.Proposal;

/**
 */
public class JaxosHttpHandler extends HttpHandler {

	private int timeout = 2;
	private TimeUnit timeunit = TimeUnit.MINUTES;
	private JaxosConfiguration config = new JaxosConfiguration();
	private JaxosFacade jaxosClient = new JaxosFacade(config);
	public static final String MULTI_PAXOS = "/multipaxos/";
	public static final String PAXOS = "/paxos/";

	public JaxosHttpHandler() throws IOException {

	}

	@Override
	public void service(Request request, Response response) throws Exception {
		final String path = request.getHttpHandlerPath();

        if (request.getMethod() == Method.POST || request.getMethod() == Method.PUT) {
            if (path.startsWith(MULTI_PAXOS)) {

                final String subject = path.substring(MULTI_PAXOS.length());
                final String value = new String(IOUtils.toByteArray(request.getInputStream()));

                response.suspend();

                final Future<Proposal<ByteBuffer>> proposalFuture = jaxosClient.mutiPaxos(subject, value, timeout, timeunit);
                final Proposal<ByteBuffer> proposal = proposalFuture.get();
                serveProposal(proposal, response);
            } else if (path.startsWith(PAXOS)) {

                final String subject = path.substring(PAXOS.length());
                final String value = new String(IOUtils.toByteArray(request.getInputStream()));

                response.suspend();

                final Future<Proposal<ByteBuffer>> proposalFuture = jaxosClient.paxos(subject, value, timeout, timeunit);
                final Proposal<ByteBuffer> proposal = proposalFuture.get();
                serveProposal(proposal, response);
            } else {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                response.getWriter().write("Unhandled url: " + request.getHttpHandlerPath());
                response.finish();
            }
        } else {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            response.getWriter().write("Method not allowed: "+request.getMethod());
            response.finish();
        }
	}

	private void serveProposal(final Proposal<ByteBuffer> proposal, final Response response) throws IOException {
		if (proposal != null) {
			response.setHeader("X-Jaxos-Proposal-Number", proposal.getN().toString());
			final ByteBuffer buffer = proposal.getValue();			
			response.getNIOOutputStream().write(
					buffer.array(),
					buffer.arrayOffset(),
					buffer.limit()
					);
		}
		else {
			response.setHeader("X-Jaxos-Proposal-Number", "-1");
			response.setStatus(HttpStatus.NO_CONTENT_204);
		}
		response.resume();
	}

}
