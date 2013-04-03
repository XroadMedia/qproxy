package tv.xrm.qproxy.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.xrm.qproxy.Request;
import tv.xrm.qproxy.RequestStorage;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public final class FileStorage implements RequestStorage {

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

            writeUri(request.getUri(), targetChannel);
            writeHeaders(request.getHeaders(), targetChannel);
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
        URI uri = readUri(sourceChannel);
        Map<String, Collection<String>> headers = marshalling.unmarshal(readHeaders(sourceChannel));

        LOG.debug("retrieved {} {}", id, uri);

        return new Request(uri, headers, sourceChannel, id);
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

    private URI readUri(FileChannel sourceChannel) throws IOException {
        int length = readLength(sourceChannel);
        ByteBuffer uriBuffer = ByteBuffer.allocate(length);
        sourceChannel.read(uriBuffer);
        uriBuffer.flip();
        return URI.create(new String(uriBuffer.array(), StandardCharsets.US_ASCII));
    }

    private void writeUri(URI uri, FileChannel targetChannel) throws IOException {
        final byte[] uriBytes = uri.toASCIIString().getBytes(StandardCharsets.US_ASCII);
        writeLength(targetChannel, uriBytes.length);
        targetChannel.write(ByteBuffer.wrap(uriBytes));
    }

    private void writeHeaders(Map<String, Collection<String>> headers, FileChannel targetChannel) throws IOException {
        final byte[] headerBytes = marshalling.marshal(headers);
        writeLength(targetChannel, headerBytes.length);
        targetChannel.write(ByteBuffer.wrap(headerBytes));
    }

    private byte[] readHeaders(FileChannel sourceChannel) throws IOException {
        int length = readLength(sourceChannel);
        ByteBuffer headersBuffer = ByteBuffer.allocate(length);
        sourceChannel.read(headersBuffer);
        headersBuffer.flip();
        return headersBuffer.array();
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
}
