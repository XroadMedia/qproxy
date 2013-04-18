package tv.xrm.qproxy.storage;


import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import tv.xrm.qproxy.Request;
import tv.xrm.qproxy.RequestStorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FileStorageTest {

    private Path tempFolder;
    private RequestStorage storage;


    @Before
    public void setup() throws IOException {
        tempFolder = Files.createTempDirectory(FileStorageTest.class.toString());
        storage = new FileStorage(tempFolder);
    }


    // @After
    public void teardown() throws IOException {
        Files.walkFileTree(tempFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    public void canStoreWithoutComplaint() throws Exception {
        Request r = new Request(new URI("http://foo.com/bar"), generateHeaders(), channelFromString("test"));
        assertNotNull(storage.store(r));
    }

    @Test
    public void canStoreAndRetrieve() throws Exception {
        final URI uri = new URI("http://foo.com/bar/12354?bla=boo");
        final Map<String, Collection<String>> headers = generateHeaders();
        final String data = "Съешь ещё этих мягких французских булок, да выпей же чаю";


        final Request r = new Request(uri, headers, channelFromString(data));
        String id = storage.store(r);


        final Request retrievedRequest = storage.retrieve(id);

        assertEquals(id, retrievedRequest.getId());
        assertEquals(r.getHeaders(), retrievedRequest.getHeaders());
        assertEquals(data, stringFromChannel(retrievedRequest.getBodyStream()));

    }

    private ReadableByteChannel channelFromString(String input) {
        return Channels.newChannel(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    private String stringFromChannel(ReadableByteChannel input) throws IOException {
        return IOUtils.toString(Channels.newInputStream(input), StandardCharsets.UTF_8);
    }

    private Map<String, Collection<String>> generateHeaders() {
        Map<String, Collection<String>> map = new HashMap<>();
        map.put("Accept", Arrays.asList("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
        map.put("Foo", Arrays.asList("bar", "baz"));
        return map;
    }

}
