package tv.xrm.qproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RequestQueue {
    private static final int CAPACITY = 1024;

    private static final int ENQUEUING_TIMEOUT_S = 3;

    private static final int MAX_RETRIES = 4;
    private static final int RETRY_DELAY_BASE_S = 3;

    private static final Logger LOG = LoggerFactory.getLogger(RequestQueue.class);

    private final Timer delayTimer = new Timer("RequestQueue_delayTimer", true);

    private static final class IdRetries {
        final String id;
        final int retries;

        IdRetries(String id, int retries) {
            this.id = id;
            this.retries = retries;
        }
    }

    private final LinkedBlockingQueue<IdRetries> requestQueue = new LinkedBlockingQueue<>(CAPACITY);

    private final RequestStorage storage;

    public RequestQueue(final RequestStorage storage) {
        this.storage = storage;
        LOG.debug("created request queue for storage {}", storage);
    }

    public Request enqueue(final Request req) throws IOException {
        String id = storage.store(req);
        addToQueue(id, req.getRetryCount());
        return new Request(req, id);
    }

    private void addToQueue(String id, int retries) {
        try {
            if(!requestQueue.offer(new IdRetries(id, retries), ENQUEUING_TIMEOUT_S, TimeUnit.SECONDS)) {
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
        }
        catch(IOException e) {
            throw new RequestQueueException("failed to retrieve " + entry + " from storage", e);
        }
    }

    public void requeue(final Request req) {
        final int retries = req.getRetryCount() + 1;

        if (retries <= MAX_RETRIES) {
            final long delay = 1000L * (long) Math.pow(RETRY_DELAY_BASE_S, retries);
            LOG.debug("scheduling retry for {} in {} ms", req, delay);
            delayTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    addToQueue(req.getId(), retries);
                }
            }, delay);
        } else {
            LOG.warn("giving up retries for {}, maximum of {} attempts reached", req, MAX_RETRIES);
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

}
