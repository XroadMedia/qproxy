package tv.xrm.qproxy.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.Request;
import tv.xrm.qproxy.RequestStorage;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class FileStorage implements RequestStorage {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorage.class);
    private static final String SUFFIX = ".req";
    private static final int SIZEOF_INT = 4;

    private final Marshalling marshalling = new Marshalling();

    private final Path baseDir;

    public FileStorage(final Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String store(Request request) throws IOException {
        Path target = Files.createTempFile(baseDir, Long.toString(System.currentTimeMillis()), SUFFIX);
        String id = target.getFileName().toString();

        try (ReadableByteChannel inChannel = request.getBodyStream();
             FileChannel targetChannel = FileChannel.open(target, StandardOpenOption.WRITE)) {

            writeStorageBlock(new StorageBlock(request.getUri(), request.getHeaders(), request.getReceivedTimestamp()), targetChannel);
            targetChannel.transferFrom(inChannel, targetChannel.position(), Integer.MAX_VALUE);
        } catch (IOException | RuntimeException e) {
            Files.delete(target);
            throw e;
        }

        return id;
    }

    @Override
    public Request retrieve(final String id) throws IOException {
        final Path source = baseDir.resolve(Objects.requireNonNull(id));

        FileChannel sourceChannel = FileChannel.open(source, StandardOpenOption.READ);
        StorageBlock stb = readStorageBlock(sourceChannel);

        LOG.debug("retrieved {} {}", id, stb.getUri());

        return new Request(stb.getUri(), stb.getHeaders(), sourceChannel, id, 0, stb.getReceivedTimestamp());
    }

    @Override
    public List<Request> retrieve() {
        final String glob = "*" + SUFFIX;
        List<Request> result = new LinkedList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, glob)) {
            for (Path file : stream) {
                String id = file.getFileName().toString();
                try {
                    result.add(retrieve(id));
                } catch (IOException e) {
                    LOG.warn("unable to read file {}; skipping", id, e);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("unable to list directory " + baseDir);
        }

        return result;
    }

    @Override
    public void delete(final String id) {
        final Path source = baseDir.resolve(id);
        try {
            Files.delete(source);
        } catch (IOException e) {
            LOG.warn("unable to delete " + id, e);
        }
    }

    private void writeStorageBlock(StorageBlock stb, FileChannel targetChannel) throws IOException {
        final byte[] bytes = marshalling.marshal(stb);
        writeLength(targetChannel, bytes.length);
        targetChannel.write(ByteBuffer.wrap(bytes));
    }

    private StorageBlock readStorageBlock(FileChannel sourceChannel) throws IOException {
        int length = readLength(sourceChannel);
        ByteBuffer buffer = ByteBuffer.allocate(length);
        sourceChannel.read(buffer);
        buffer.flip();

        return marshalling.unmarshal(buffer.array());
    }

    private void writeLength(FileChannel targetChannel, final int length) throws IOException {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(SIZEOF_INT).putInt(length);
        lengthBuffer.rewind();
        targetChannel.write(lengthBuffer);
    }

    private int readLength(FileChannel sourceChannel) throws IOException {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(SIZEOF_INT);
        sourceChannel.read(lengthBuffer);
        lengthBuffer.flip();
        return lengthBuffer.asIntBuffer().get();
    }

    @Override
    public String toString() {
        return "FileStorage{" +
                "baseDir=" + baseDir +
                '}';
    }

    static final class StorageBlock {
        private final URI uri;
        private final Map<String, Collection<String>> headers;
        private final long receivedTimestamp;

        @JsonCreator
        public StorageBlock(@JsonProperty("uri") URI uri,
                            @JsonProperty("headers") Map<String, Collection<String>> headers,
                            @JsonProperty("receivedTimestamp") long receivedTimestamp) {
            this.uri = uri;
            this.headers = headers;
            this.receivedTimestamp = receivedTimestamp;
        }

        public URI getUri() {
            return uri;
        }

        public Map<String, Collection<String>> getHeaders() {
            return headers;
        }

        public long getReceivedTimestamp() {
            return receivedTimestamp;
        }
    }
}
