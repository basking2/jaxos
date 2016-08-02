package org.sdsai.jaxos;

import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is more of the HTML end point.
 */
public class JaxosUiHttpHandler extends HttpHandler {
    public static final String PREFIX = "/html";

    @Override
    public void service(Request request, Response response) throws Exception {
        String path = request.getHttpHandlerPath();

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
            response.getNIOWriter().write("Resource not found: "+path);
            response.setStatus(HttpStatus.NOT_FOUND_404);
            response.finish();
            return;
        }

        response.setContentType("text/html");
        IOUtils.copy(in, response.getOutputStream());
        response.finish();
    }
}
