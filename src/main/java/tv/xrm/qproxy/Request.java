package tv.xrm.qproxy;

import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * A request to be proxied.
 */
public final class Request {
    private final URI uri;
    private final Map<String, Collection<String>> headers;
    private final ReadableByteChannel bodyStream;
    private final String id;
    private final long receivedTimestamp;

    private final int retryCount;

    public Request(URI uri, Map<String, Collection<String>> headers, ReadableByteChannel bodyStream, String id, int retryCount, long receivedTimestamp) {
        this.uri = uri;
        this.headers = headers;
        this.bodyStream = bodyStream;
        this.id = id;
        this.retryCount = retryCount;
        this.receivedTimestamp = receivedTimestamp;
    }

    public static Request withId(final Request original, final String id) {
        return new Request(original.getUri(), original.getHeaders(), original.getBodyStream(),
                id,
                original.getRetryCount(), original.getReceivedTimestamp());
    }

    public static Request withRetries(final Request original, final int retryCount) {
        return new Request(original.getUri(), original.getHeaders(), original.getBodyStream(), original.getId(),
                retryCount,
                original.getReceivedTimestamp());
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

    public long getReceivedTimestamp() {
        return receivedTimestamp;
    }

    @Override
    public String toString() {
        return "Request{" +
                "uri=" + uri +
                ", id='" + id + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Request) {
            Request other = (Request) o;
            return Objects.equals(uri, other.uri) &&
                    Objects.equals(headers, other.headers) &&
                    Objects.equals(bodyStream, other.bodyStream) &&
                    Objects.equals(id, other.id) &&
                    Objects.equals(receivedTimestamp, other.receivedTimestamp) &&
                    Objects.equals(retryCount, other.retryCount);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, headers, bodyStream, id, receivedTimestamp, retryCount);
    }
}
