package tv.xrm.qproxy.in;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.*;
import tv.xrm.qproxy.out.DefaultRequestDispatcher;
import tv.xrm.qproxy.storage.FileStorage;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Initialization of the application
 */
@WebListener
public class Setup implements ServletContextListener {

    public static final String PATH_PROPERTY = "qproxy.dataDirectory";

    public static final String Q_REGISTRY = "qproxy.queueRegistry";

    private static final String DEFAULT_DATA_ROOT = System.getProperty("java.io.tmpdir");

    private static final Logger LOG = LoggerFactory.getLogger(Setup.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final Path basedir = getBasedir();
        final RequestStorage storage = new FileStorage(basedir);

        QueueRegistry qReg = new QueueRegistry(new QueueRegistry.RequestQueueAndDispatcherFactory() {
            @Override
            public RequestQueue getQueue() {
                return new RequestQueue(storage);
            }

            @Override
            public RequestDispatcher getDispatcher(final RequestQueue queue) {
                return new DefaultRequestDispatcher(queue);
            }
        });
        sce.getServletContext().setAttribute(Q_REGISTRY, qReg);

        cleanupLeftoverRequests(basedir, qReg);
    }

    private Path getBasedir() {
        final String pathProperty = System.getProperty(PATH_PROPERTY);

        final Path basedir = pathProperty != null ?
                FileSystems.getDefault().getPath(pathProperty) :
                FileSystems.getDefault().getPath(DEFAULT_DATA_ROOT).resolve("qproxy-queues");
        try {
            if (!Files.exists(basedir)) {
                Files.createDirectory(basedir);
            }
            return basedir;
        } catch (IOException e) {
            throw new IllegalStateException("unable to read/getDispatcher data directory " + basedir);
        }
    }

    private void cleanupLeftoverRequests(Path basedir, QueueRegistry qReg) {
        RequestStorage fs = new FileStorage(basedir);

        List<Request> leftovers = fs.retrieve();
        for (Request req : leftovers) {
            LOG.info("processing leftover request {}", req);
            RequestQueue q = qReg.getQueue(req.getUri());
            try {
                q.enqueue(req);
            } catch (IOException e) {
                LOG.warn("unable to enqueue leftover request {}; skipping", req, e);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
