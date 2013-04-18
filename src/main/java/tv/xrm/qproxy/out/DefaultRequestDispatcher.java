package tv.xrm.qproxy.out;


import com.ning.http.client.*;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.yammer.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.RequestDispatcher;
import tv.xrm.qproxy.RequestQueue;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.yammer.metrics.MetricRegistry.name;

/**
 * Takes items from a RequestQueue and delivers them.
 */
public final class DefaultRequestDispatcher implements RequestDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRequestDispatcher.class);

    private static final int NUM_THREADS = 3;

    private final com.yammer.metrics.Timer requestTimer;

    private final RequestQueue q;

    private final AsyncHttpClient client = new AsyncHttpClient();

    public DefaultRequestDispatcher(final RequestQueue queue, final MetricRegistry metricRegistry) {
        this.q = Objects.requireNonNull(queue);

        requestTimer = metricRegistry.timer(name(DefaultRequestDispatcher.class, queue.getQueueId(), "outgoing-requests"));
    }

    @Override
    public void start() {
        LOG.debug("dispatcher starting up for queue {}", q);
        ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            service.submit(new Dispatcher());
        }
    }

    class Dispatcher implements Runnable {
        @Override
        public void run() {

            try {
                while (true) {
                    tv.xrm.qproxy.Request req;

                    try {
                        req = q.take();
                    } catch (RequestQueue.RequestQueueException e) {
                        LOG.warn("failed to read a request object, skipping", e);
                        continue;
                    }

                    try {
                        LOG.debug("retrieved {}", req);
                        postRequest(req);
                        LOG.debug("dispatched {}", req);
                        q.cleanup(req.getId());
                    } catch (IOException e) {
                        LOG.warn("exception trying to dispatch " + req, e);
                        q.requeue(req);
                    }
                }
            } catch (InterruptedException ignored) {
                // just return, but set interrupted status (app probably shutting down)
                Thread.currentThread().interrupt();
            }
        }

        private void postRequest(final tv.xrm.qproxy.Request req) throws IOException, InterruptedException {
            final com.yammer.metrics.Timer.Context timerContext = requestTimer.time();

            try {
                final BodyGenerator bodyGenerator;
                if (req.getBodyStream() instanceof SeekableByteChannel) {
                    bodyGenerator = new StreamingNonchunkingBodyGenerator((SeekableByteChannel) req.getBodyStream());
                } else {
                    bodyGenerator = new InputStreamBodyGenerator(Channels.newInputStream(req.getBodyStream()));
                }

                Request httpRequest = new RequestBuilder("POST")
                        .setUrl(req.getUri().toString())
                        .setHeaders(req.getHeaders())
                        .setBody(bodyGenerator)
                        .build();

                try {
                    Response result = client.executeRequest(httpRequest).get();
                    final int status = result.getStatusCode();

                    if (status >= HttpServletResponse.SC_OK && status < HttpServletResponse.SC_MULTIPLE_CHOICES) {
                        LOG.info("req: {} response: {} {}", req, status, result.getStatusText());
                    } else {
                        LOG.warn("req: {} response: {} {}", req, status, result.getStatusText());
                    }

                    if (result.getStatusCode() == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
                        throw new IOException("service unavailable, retry later");
                    }
                } catch (ExecutionException e) {
                    throw new IOException(e);
                }
            } finally {
                timerContext.stop();
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + "{" + q + "}";
    }
}
