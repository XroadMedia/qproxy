package tv.xrm.qproxy.in;


import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.servlets.MetricsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.*;
import tv.xrm.qproxy.out.DefaultRequestDispatcher;
import tv.xrm.qproxy.storage.FileStorage;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Initialization of the application.
 */
@WebListener
public class Setup implements ServletContextListener {

    public static final String PATH_PROPERTY = "qproxy.dataDirectory";

    private static final String DEFAULT_DATA_ROOT = System.getProperty("java.io.tmpdir");

    private static final Logger LOG = LoggerFactory.getLogger(Setup.class);

    private static final long MAX_REQUEST_AGE_MS = 8 * 60 * 60 * 1000;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final Path basedir = getBasedir();
        final RequestStorage storage = new FileStorage(basedir);

        final MetricRegistry metricRegistry = new MetricRegistry("qproxy");

        final QueueRegistry qReg = new QueueRegistry(new QueueRegistry.RequestQueueAndDispatcherFactory() {
            @Override
            public RequestQueue getQueue(final String id) {
                return new RequestQueue(id, storage, metricRegistry);
            }

            @Override
            public RequestDispatcher getDispatcher(final RequestQueue queue) {
                return new DefaultRequestDispatcher(queue, metricRegistry);
            }
        });

        final ServletContext sc = sce.getServletContext();
        sc.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);

        ServletRegistration proxySr = sc.addServlet("proxy", new ProxyServlet(qReg, metricRegistry));
        proxySr.addMapping("/");

        ServletRegistration metricsSr = sc.addServlet("metrics", new MetricsServlet());
        metricsSr.addMapping("/metrics");

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
        final RequestStorage fs = new FileStorage(basedir);

        final List<Request> leftovers = fs.retrieve();
        for (final Request req : leftovers) {
            final long ageMillis = System.currentTimeMillis() - req.getReceivedTimestamp();

            if (ageMillis <= MAX_REQUEST_AGE_MS) {
                LOG.info("processing leftover request {}", req);
                RequestQueue q = qReg.getQueue(req.getUri());
                try {
                    q.enqueue(req);
                } catch (IOException e) {
                    LOG.warn("unable to enqueue leftover request {}; skipping", req, e);
                }
            } else {
                LOG.info("skipping leftover request {} because it's too old", req);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
