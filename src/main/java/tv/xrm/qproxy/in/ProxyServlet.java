package tv.xrm.qproxy.in;

import com.google.common.base.Strings;
import com.yammer.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.QueueRegistry;
import tv.xrm.qproxy.Request;
import tv.xrm.qproxy.RequestQueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.util.*;

import static com.yammer.metrics.MetricRegistry.name;

/**
 * Main servlet that accepts POST requests and puts them on a queue.
 */
public class ProxyServlet extends HttpServlet {
    private static final long serialVersionUID = 1l;

    private static final Logger LOG = LoggerFactory.getLogger(ProxyServlet.class);

    private final QueueRegistry queueRegistry;

    private final com.yammer.metrics.Timer requestTimer;

    public ProxyServlet(final QueueRegistry queueRegistry, final MetricRegistry metricRegistry) {
        this.queueRegistry = queueRegistry;

        requestTimer = metricRegistry.timer(name(ProxyServlet.class, "incoming-requests"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final com.yammer.metrics.Timer.Context timerContext = requestTimer.time();
        try {
            final URI uri;
            try {
                final String uriParam = req.getParameter("url");
                uri = checkAsURI(uriParam);
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
        } finally {
            timerContext.stop();
        }
    }

    static URI checkAsURI(final String uriParam) throws URISyntaxException {
        if (Strings.isNullOrEmpty(uriParam)) {
            throw new URISyntaxException("url", "must not be null or empty");
        }
        final URI uri = new URI(uriParam);

        if (!uri.isAbsolute() || uri.isOpaque()) {
            throw new URISyntaxException("url", "must be absolute and hierarchical");
        }

        return uri;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.getWriter().print("qproxy is up and running. Send POST requests to this path, add target URL as request parameter 'url'.");
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
