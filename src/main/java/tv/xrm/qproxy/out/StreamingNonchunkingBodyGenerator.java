package tv.xrm.qproxy.out;

import com.ning.http.client.Body;
import com.ning.http.client.BodyGenerator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * An async-http-client BodyGenerator that streams with a known content length (e.g. for reading from a file of
 * known (and fixed) size), without chunking.
 */
final class StreamingNonchunkingBodyGenerator implements BodyGenerator {
    private final long contentLength;
    private final SeekableByteChannel channel;

    public StreamingNonchunkingBodyGenerator(final SeekableByteChannel channel) throws IOException {
        this.channel = channel;
        this.contentLength = channel.size() - channel.position();
    }

    @Override
    public Body createBody() throws IOException {
        return new StreamingBody();
    }

    private final class StreamingBody implements Body {
        @Override
        public long getContentLength() {
            return contentLength;
        }

        @Override
        public long read(ByteBuffer buffer) throws IOException {
            return channel.read(buffer);
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }

}
