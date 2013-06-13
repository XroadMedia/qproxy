package tv.xrm.qproxy.in;


import tv.xrm.qproxy.LifecyclePolicy;
import tv.xrm.qproxy.Request;

import javax.servlet.http.HttpServletResponse;

class DefaultLifecyclePolicy implements LifecyclePolicy {
    private static final int MILLIS = 1000;

    private final int maxRetries;
    private final int retryDelayBaseSeconds;
    private final int maxRequestAgeSeconds;

    public DefaultLifecyclePolicy(int maxRetries, int retryDelayBaseSeconds, int maxRequestAgeSeconds) {
        this.maxRetries = maxRetries;
        this.retryDelayBaseSeconds = retryDelayBaseSeconds;
        this.maxRequestAgeSeconds = maxRequestAgeSeconds;
    }

    @Override
    public boolean isSuccessfullyDelivered(final int httpStatusCode) {
        return httpStatusCode >= HttpServletResponse.SC_OK && httpStatusCode < HttpServletResponse.SC_MULTIPLE_CHOICES;
    }

    @Override
    public long shouldRetryIn(final Request req) {
        if (req.getRetryCount() <= maxRetries) {
            return MILLIS * (long) Math.pow(retryDelayBaseSeconds, req.getRetryCount());
        } else {
            return DO_NOT_RETRY;
        }
    }

    @Override
    public boolean shouldForget(final Request req) {
        final long ageMillis = System.currentTimeMillis() - req.getReceivedTimestamp();
        return ageMillis > (maxRequestAgeSeconds * MILLIS);
    }

    @Override
    public boolean shouldRetryOnStatus(final int httpStatusCode) {
        return httpStatusCode == HttpServletResponse.SC_SERVICE_UNAVAILABLE;
    }
}
