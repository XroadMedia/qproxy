package tv.xrm.qproxy;

import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.Map;

/**
 * A request to be proxied.
 */
public final class Request {
    private final URI uri;
    private final Map<String, Collection<String>> headers;
    private final ReadableByteChannel bodyStream;
    private final String id;


    private final int retryCount;

    public Request(URI uri, Map<String, Collection<String>> headers, ReadableByteChannel bodyStream, String id, int retryCount) {
        this.uri = uri;
        this.headers = headers;
        this.bodyStream = bodyStream;
        this.id = id;
        this.retryCount = retryCount;
    }

    public Request(URI uri, Map<String, Collection<String>> headers, ReadableByteChannel bodyStream, String id) {
        this(uri, headers, bodyStream, id, 0);
    }

    public Request(URI uri, Map<String, Collection<String>> headers, ReadableByteChannel bodyStream) {
        this(uri, headers, bodyStream, null);
    }

    public Request(Request req, String id) {
        this(req.getUri(), req.getHeaders(), req.getBodyStream(), id);
    }

    public static Request withRetries(final Request original, final int retryCount) {
        return new Request(original.getUri(), original.getHeaders(), original.getBodyStream(), original.getId(), retryCount);
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, Collection<String>> getHeaders() {
        return headers;
    }

    public ReadableByteChannel getBodyStream() {
        return bodyStream;
    }

    public String getId() {
        return id;
    }

    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public String toString() {
        return "Request{" +
                "uri=" + uri +
                ", id='" + id + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }
}
