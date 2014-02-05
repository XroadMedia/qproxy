package tv.xrm.qproxy;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class QueueRegistryTest {

    @Test
    public void splitsACouple() {
        URI u = URI.create("http://foo.bar:80/blaz/bar/boo/sap/phew?bla=zap&meep=zing");

        String cut = new QueueRegistry(null, 3).aggregateUri(u);

        assertEquals("http://foo.bar:80/blaz/bar/boo", cut);
    }

    @Test
    public void splitsRegardlessOfTrailingSlash() {
        URI u = URI.create("http://foo.bar:80/blaz/bar/boo?bla=zap&meep=zing");
        String cut = new QueueRegistry(null, 3).aggregateUri(u);
        assertEquals("http://foo.bar:80/blaz/bar/boo", cut);

        u = URI.create("http://foo.bar:80/blaz//bar/boo/?bla=zap&meep=zing");
        cut = new QueueRegistry(null, 3).aggregateUri(u);
        assertEquals("http://foo.bar:80/blaz/bar/boo", cut);
    }

    @Test
    public void splitsRegardlessOfMultiSlash() {
        URI u = URI.create("http://foo.bar:80////");
        String cut = new QueueRegistry(null, 3).aggregateUri(u);
        assertEquals("http://foo.bar:80/", cut);
    }

    @Test
    public void splitsToHost() {
        URI u = URI.create("http://foo.bar:80/blaz/bar/boo/sap/phew?bla=zap&meep=zing");

        String cut = new QueueRegistry(null, 0).aggregateUri(u);

        assertEquals("http://foo.bar:80/", cut);
    }

    @Test
    public void splitsToHostRegardlessOfTrailingSlash() {
        URI u = URI.create("http://foo.bar:80");

        String cut = new QueueRegistry(null, 0).aggregateUri(u);

        assertEquals("http://foo.bar:80/", cut);
    }

    @Test
    public void splitsNoLimit() {
        URI u = URI.create("http://foo.bar:80/blaz/bar/boo/sap/phew?bla=zap&meep=zing");

        String cut = new QueueRegistry(null, -1).aggregateUri(u);

        assertEquals("http://foo.bar:80/blaz/bar/boo/sap/phew", cut);
    }

}
