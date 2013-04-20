package tv.xrm.qproxy.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tv.xrm.qproxy.Request;
import tv.xrm.qproxy.RequestStorage;
import tv.xrm.qproxy.TestDataFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
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

    @After
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
        Request r = TestDataFactory.generateRequest();
        assertNotNull(storage.store(r));
    }

    @Test
    public void canStoreAndRetrieve() throws Exception {
        final URI uri = new URI("http://foo.com/bar/12354?bla=boo");
        final Map<String, Collection<String>> headers = TestDataFactory.generateHeaders();
        final String data = "Съешь ещё этих мягких французских булок, да выпей же чаю";

        final Request r = new Request(uri, headers, TestDataFactory.channelFromString(data), null, 0, System.currentTimeMillis());
        String id = storage.store(r);

        final Request retrievedRequest = storage.retrieve(id);

        assertEquals(id, retrievedRequest.getId());
        assertEquals(r.getHeaders(), retrievedRequest.getHeaders());
        assertEquals(data, TestDataFactory.stringFromChannel(retrievedRequest.getBodyStream()));
    }

    @Test(expected = IOException.class)
    public void failsProperlyOnNonexistentId() throws IOException {
        storage.retrieve("nonexistent");
    }


}
