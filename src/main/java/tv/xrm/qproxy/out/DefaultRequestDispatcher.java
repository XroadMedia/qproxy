package tv.xrm.qproxy.out;


import com.codahale.metrics.MetricRegistry;
import com.ning.http.client.*;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.LifecyclePolicy;
import tv.xrm.qproxy.RequestDispatcher;
import tv.xrm.qproxy.RequestQueue;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Takes items from a RequestQueue and delivers them.
 */
public final class DefaultRequestDispatcher implements RequestDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRequestDispatcher.class);

    private final AsyncHttpClient client = new AsyncHttpClient();

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
                final BodyGenerator bodyGenerator;
                if (ch instanceof SeekableByteChannel) {
                    bodyGenerator = new StreamingNonchunkingBodyGenerator((SeekableByteChannel) ch);
                } else {
                    bodyGenerator = new InputStreamBodyGenerator(Channels.newInputStream(ch));
                }

                Request httpRequest = new RequestBuilder("POST")
                        .setUrl(req.getUri().toString())
                        .setHeaders(req.getHeaders())
                        .setBody(bodyGenerator)
                        .build();

                try {
                    Response result = client.executeRequest(httpRequest).get();
                    final int status = result.getStatusCode();

                    if (lifecyclePolicy.isSuccessfullyDelivered(status)) {
                        LOG.debug("req: {} response: {} {}", req, status, result.getStatusText());
                    } else {
                        LOG.warn("req: {} response: {} {}", req, status, result.getStatusText());
                        if (lifecyclePolicy.shouldRetryOnStatus(status)) {
                            throw new IOException("service unavailable, retry later");
                        } else {
                            LOG.warn("giving up on request {}", req);
                        }
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
