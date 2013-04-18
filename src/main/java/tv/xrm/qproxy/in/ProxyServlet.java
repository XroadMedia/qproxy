package tv.xrm.qproxy.in;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.QueueRegistry;
import tv.xrm.qproxy.Request;
import tv.xrm.qproxy.RequestQueue;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.util.*;

@WebServlet("/")
public class ProxyServlet extends HttpServlet {
    private static final long serialVersionUID = 1l;

    private static final Logger LOG = LoggerFactory.getLogger(ProxyServlet.class);

    private QueueRegistry queueRegistry;

    @Override
    public void init() throws ServletException {
        queueRegistry = Objects.requireNonNull((QueueRegistry) getServletContext().getAttribute(Setup.Q_REGISTRY),
                "QueueRegistry not initialized?");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final URI uri;
        try {
            final String uriParam = req.getParameter("uri");
            if (uriParam == null || uriParam.isEmpty()) {
                throw new URISyntaxException("uri", "must not be null or empty");
            }
            uri = new URI(uriParam);
        } catch (URISyntaxException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        try (InputStream is = req.getInputStream()) {
            RequestQueue queue = queueRegistry.getQueue(uri);
            Map<String, Collection<String>> headers = extractHeaders(req);
            Request idRequest = queue.enqueue(new Request(uri, headers, Channels.newChannel(is)));
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            resp.setHeader("X-XRM-Stored-As", idRequest.getId());
        } catch (IOException | RuntimeException e) {
            LOG.warn("failed to process request", e);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    private Map<String, Collection<String>> extractHeaders(final HttpServletRequest req) {
        final Map<String, Collection<String>> map = new HashMap<>();
        final Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            List<String> values = Collections.list(req.getHeaders(headerName));

            if (!headerToIgnore(headerName)) {
                map.put(headerName, values);
            }
        }
        return map;
    }

    private boolean headerToIgnore(String header) {
        return header.equalsIgnoreCase("Transfer-Encoding")
                || header.equalsIgnoreCase("Content-Length");
    }
}
