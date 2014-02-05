package tv.xrm.qproxy;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Initializes and keeps queues and allows to find a queue for a given Request.
 */
public class QueueRegistry {

    private final ConcurrentMap<String, RequestQueue> map = new ConcurrentHashMap<>();
    private final RequestQueueAndDispatcherFactory factory;
    private final int levels;

    public QueueRegistry(RequestQueueAndDispatcherFactory factory, int levels) {
        this.factory = factory;
        this.levels = levels;
    }

    public interface RequestQueueAndDispatcherFactory {
        RequestQueue getQueue(String id);

        RequestDispatcher getDispatcher(RequestQueue queue);
    }

    public RequestQueue getQueue(final URI uri) {
        final String cutUri = aggregateUri(uri);

        RequestQueue q = map.get(cutUri);
        if (q == null) {
            q = factory.getQueue(cutUri);
            RequestQueue prevQ = map.putIfAbsent(cutUri, q);
            if (prevQ != null) {
                q = prevQ;
            } else {
                // new one was used
                RequestDispatcher dispatcher = factory.getDispatcher(q);
                dispatcher.start();
            }
        }
        return q;
    }

    String aggregateUri(final URI uri) {
        String cutUri;
        try {
            String path = uri.getPath();

            if (levels > 0) {
                Iterable<String> split = Iterables.limit(Splitter.on('/').omitEmptyStrings().split(path), levels);
                path = "/" + Joiner.on('/').join(split);
            } else if (levels == 0) {
                path = "/";
            }

            cutUri = new URI(uri.getScheme(), uri.getAuthority(), path, null, null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("unable to create URI from " + uri);
        }

        return cutUri;
    }

}