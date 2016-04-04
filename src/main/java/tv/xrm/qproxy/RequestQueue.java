package tv.xrm.qproxy;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class RequestQueue {
    private static final Logger LOG = LoggerFactory.getLogger(RequestQueue.class);

    private final Timer delayTimer = new Timer("RequestQueue_delayTimer", true);
    private final LinkedBlockingQueue<IdRetries> requestQueue;

    private final String queueId;
    private final RequestStorage storage;
    private final int enqueuingWaitMillis;

    private static final class IdRetries {
        final String id;
        final int retries;

        IdRetries(String id, int retries) {
            this.id = id;
            this.retries = retries;
        }
    }

    public RequestQueue(final String queueId, final RequestStorage storage, final MetricRegistry metricRegistry,
                        final int capacity,
                        final int enqueuingWaitMillis) {
        LOG.debug("creating request queue {} with storage {} and capacity {}", queueId, storage, capacity);
        this.requestQueue = new LinkedBlockingQueue<>(capacity);
        this.queueId = queueId;
        this.storage = storage;
        this.enqueuingWaitMillis = enqueuingWaitMillis;

        metricRegistry.register(name(RequestQueue.class, queueId, "queue-length"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return requestQueue.size();
            }
        });
    }

    public Request enqueue(final Request req) {
        String id = null;
        try {
            id = storage.store(req);
            addToQueue(id, req.getRetryCount());
            return Request.withId(req, id);
        } catch (IOException e) {
            storage.delete(id);
            throw new RequestQueueException("unable to enqueue request " + req, e);
        }
    }

    private void addToQueue(String id, int retries) {
        try {
            if (!requestQueue.offer(new IdRetries(id, retries), enqueuingWaitMillis, TimeUnit.MILLISECONDS)) {
                throw new RequestQueueException("unable to enqueue " + id + ", giving up");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestQueueException("interrupted trying to enqueue " + id + ", giving up", e);
        }
    }

    public Request take() throws InterruptedException {
        final IdRetries entry = requestQueue.take();
        try {
            Request retrieved = storage.retrieve(entry.id);
            return Request.withRetries(retrieved, entry.retries);
        } catch (IOException e) {
            throw new RequestQueueException("failed to retrieve " + entry + " from storage", e);
        }
    }

    public void requeue(final Request req, final long delayMillis) {
        if (delayMillis > 0) {
            LOG.trace("scheduling retry for {} in >={} ms", req, delayMillis);
            delayTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    addToQueue(req.getId(), req.getRetryCount());
                }
            }, delayMillis);
        } else {
            LOG.trace("scheduling retry for {} without delay", req, delayMillis);
            addToQueue(req.getId(), req.getRetryCount());
        }
    }

    public void cleanup(final String id) {
        storage.delete(id);
    }

    public static final class RequestQueueException extends RuntimeException {
        public RequestQueueException(String message) {
            super(message);
        }

        public RequestQueueException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public String getQueueId() {
        return queueId;
    }

    @Override
    public String toString() {
        return super.toString() + "{" + getQueueId() + "}";
    }
}
