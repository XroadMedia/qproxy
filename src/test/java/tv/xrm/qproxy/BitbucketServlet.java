package tv.xrm.qproxy;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This servlet will happily accept data and do nothing with them. Useful as a "stub" recipient for load testing.
 */
@WebServlet(name = "bitbucket", urlPatterns = "/bitbucket")
public final class BitbucketServlet extends HttpServlet {

    private final AtomicLong requestCount = new AtomicLong(0);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int allBytesRead = 0;

        try (InputStream in = req.getInputStream()) {
            final byte[] bytes = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(bytes)) != -1) {
                allBytesRead += bytesRead;
            }
        }

        resp.setContentType("text/plain");
        resp.getWriter().printf("received and recycled %d bytes, now counting %d requests", allBytesRead, requestCount.incrementAndGet());
        resp.setStatus(200);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.getWriter().printf("counted %d requests so far", requestCount.get());
    }
}
