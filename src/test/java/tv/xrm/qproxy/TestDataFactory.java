package tv.xrm.qproxy;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class TestDataFactory {
    private TestDataFactory() {
    }

    public static ReadableByteChannel channelFromString(String input) {
        return Channels.newChannel(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    public static String stringFromChannel(ReadableByteChannel input) throws IOException {
        byte[] bytes = ByteStreams.toByteArray(Channels.newInputStream(input));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static Map<String, Collection<String>> generateHeaders() {
        Map<String, Collection<String>> map = new HashMap<>();
        map.put("Accept", Arrays.asList("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
        map.put("Foo", Arrays.asList("bar", "baz"));
        return map;
    }

    public static Request generateRequest() {
        return new Request(
                URI.create("http://foo.bar"),
                generateHeaders(),
                channelFromString(Double.toString(Math.random())),
                Double.toString(Math.random()),
                0,
                System.currentTimeMillis()
        );
    }
}
