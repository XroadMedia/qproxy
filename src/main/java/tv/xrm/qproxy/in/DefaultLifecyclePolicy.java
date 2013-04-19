package tv.xrm.qproxy.in;


import tv.xrm.qproxy.LifecyclePolicy;
import tv.xrm.qproxy.Request;

import javax.servlet.http.HttpServletResponse;

class DefaultLifecyclePolicy implements LifecyclePolicy {

    private static final int MAX_RETRIES = 4;
    private static final int RETRY_DELAY_BASE_S = 3;
    private static final long MAX_REQUEST_AGE_MS = 8 * 60 * 60 * 1000;

    @Override
    public boolean isSuccessfullyDelivered(final int httpStatusCode) {
        return httpStatusCode >= HttpServletResponse.SC_OK && httpStatusCode < HttpServletResponse.SC_MULTIPLE_CHOICES;
    }

    @Override
    public long shouldRetryIn(final Request req) {
        if (req.getRetryCount() <= MAX_RETRIES) {
            return 1000L * (long) Math.pow(RETRY_DELAY_BASE_S, req.getRetryCount());
        } else {
            return DO_NOT_RETRY;
        }
    }

    @Override
    public boolean shouldForget(final Request req) {
        final long ageMillis = System.currentTimeMillis() - req.getReceivedTimestamp();
        return ageMillis > MAX_REQUEST_AGE_MS;
    }

    @Override
    public boolean shouldRetryOnStatus(final int httpStatusCode) {
        return httpStatusCode == 503;
    }
}
