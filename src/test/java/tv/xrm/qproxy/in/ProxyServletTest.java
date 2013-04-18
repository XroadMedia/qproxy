package tv.xrm.qproxy.in;

import org.junit.Test;

import java.net.URISyntaxException;

public class ProxyServletTest {

    @Test(expected = URISyntaxException.class)
    public void rejectsRelativeUrl() throws Exception {
        ProxyServlet.checkAsURI("/fooo");
    }

    @Test(expected = URISyntaxException.class)
    public void rejectsOpaqueUrl() throws Exception {
        ProxyServlet.checkAsURI("mailto:hopsi@flopsi");
    }

    @Test
    public void acceptsAbsoluteUrl() throws Exception {
        ProxyServlet.checkAsURI("http://user:pw@foo.bar/baz");
    }

}
