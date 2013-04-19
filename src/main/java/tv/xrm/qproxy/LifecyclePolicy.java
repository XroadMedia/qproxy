package tv.xrm.qproxy;


/**
 * Defines when a request is considered successfully delivered, and what to do if it isn't.
 */
public interface LifecyclePolicy {

    int DO_NOT_RETRY = -1;

    /**
     * Based on the HTTP response status code, can we consider this request successfully delivered?
     *
     * @param httpStatusCode
     * @return
     */
    boolean isSuccessfullyDelivered(int httpStatusCode);


    /**
     * Based on the HTTP response status code, should retries even be considered?
     *
     * @param httpStatusCode
     * @return
     */
    boolean shouldRetryOnStatus(int httpStatusCode);

    /**
     * Based on its own data, should this request be retried, and how soon?
     *
     * @param req
     * @return Zero or positive milliseconds delay before retry. Negative if it should not be retried.
     */
    long shouldRetryIn(Request req);

    /**
     * Should this request be forgotten, i.e. never retried again?
     *
     * @param req
     * @return True if it should be forgotten, false if not.
     */
    boolean shouldForget(Request req);

}
