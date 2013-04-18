package tv.xrm.qproxy;

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

    public QueueRegistry(RequestQueueAndDispatcherFactory factory) {
        this.factory = factory;
    }

    public interface RequestQueueAndDispatcherFactory {
        RequestQueue getQueue(String id);

        RequestDispatcher getDispatcher(RequestQueue queue);
    }

    public RequestQueue getQueue(final URI uri) {
        String cutUri;
        try {
            cutUri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null).toString();
        } catch (URISyntaxException e) {
            cutUri = uri.toString();
        }

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

}