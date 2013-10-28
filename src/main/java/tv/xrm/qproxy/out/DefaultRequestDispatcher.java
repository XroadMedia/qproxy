package tv.xrm.qproxy.out;


import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.LifecyclePolicy;
import tv.xrm.qproxy.RequestDispatcher;
import tv.xrm.qproxy.RequestQueue;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;


/**
 * Takes items from a RequestQueue and delivers them.
 */
public final class DefaultRequestDispatcher implements RequestDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRequestDispatcher.class);

    private  static final Joiner COMMA_JOINER = Joiner.on(',');

    private final HttpClient client = new HttpClient();

    private final com.codahale.metrics.Timer requestTimer;

    private final RequestQueue q;

    private final LifecyclePolicy lifecyclePolicy;

    private final int threadCount;

    public DefaultRequestDispatcher(final RequestQueue queue, final MetricRegistry metricRegistry, final LifecyclePolicy lifecyclePolicy,
                                    final int threadCount) {
        this.q = Objects.requireNonNull(queue);
        this.lifecyclePolicy = lifecyclePolicy;
        this.threadCount = threadCount;

        requestTimer = metricRegistry.timer(MetricRegistry.name(DefaultRequestDispatcher.class, queue.getQueueId(), "outgoing-requests"));
    }

    @Override
    public void start() {
        LOG.debug("dispatcher starting up for queue {}", q);

        try {
            client.start();
        } catch(Exception e) {
            throw new IllegalStateException("failed to start HTTP client", e);
        }

        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
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

                        long retry = lifecyclePolicy.shouldRetryIn(req);
                        if (retry >= 0) {
                            q.requeue(tv.xrm.qproxy.Request.withRetries(req, req.getRetryCount() + 1), retry);
                        } else {
                            LOG.warn("giving up on request {}", req);
                        }
                    }
                }
            } catch (InterruptedException ignored) {
                // just return, but set interrupted status (app probably shutting down)
                Thread.currentThread().interrupt();
            }
        }

        private void postRequest(final tv.xrm.qproxy.Request req) throws IOException, InterruptedException {
            final com.codahale.metrics.Timer.Context timerContext = requestTimer.time();

            try (ReadableByteChannel ch = req.getBodyStream()) {
                final ContentProvider contentProvider = new InputStreamContentProvider(Channels.newInputStream(ch));

                Request newRequest = client.POST(req.getUri()).content(contentProvider);

                // map headers into jetty request (concatenating multi headers)
                for(Map.Entry<String, Collection<String>> header : req.getHeaders().entrySet()) {
                    newRequest = newRequest.header(header.getKey(), COMMA_JOINER.join(header.getValue()));
                }

                try {
                    ContentResponse result = newRequest.send();
                    final int status = result.getStatus();

                    if (lifecyclePolicy.isSuccessfullyDelivered(status)) {
                        LOG.debug("req: {} response: {}", req, status);
                    } else {
                        LOG.warn("req: {} response: {}", req, status);
                        if (lifecyclePolicy.shouldRetryOnStatus(status)) {
                            throw new IOException("service unavailable, retry later");
                        } else {
                            LOG.warn("giving up on request {}", req);
                        }
                    }
                } catch (ExecutionException | TimeoutException e) {
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
