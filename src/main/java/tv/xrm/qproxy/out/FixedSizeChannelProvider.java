package tv.xrm.qproxy.out;

import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.util.InputStreamContentProvider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;

final class FixedSizeChannelProvider implements ContentProvider {
    private final long contentLength;
    private final InputStreamContentProvider wrappedProvider;

    public FixedSizeChannelProvider(final ReadableByteChannel channel) throws IOException {
        if (channel instanceof SeekableByteChannel) {
            SeekableByteChannel skable = (SeekableByteChannel) channel;
            this.contentLength = skable.size() - skable.position();
        } else {
            this.contentLength = -1;
        }

        this.wrappedProvider = new InputStreamContentProvider(Channels.newInputStream(channel));
    }

    @Override
    public long getLength() {
        return this.contentLength;
    }

    @Override
    public Iterator<ByteBuffer> iterator() {
        return this.wrappedProvider.iterator();
    }
}
