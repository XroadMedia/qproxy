package tv.xrm.qproxy.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

final class Marshalling {
    private static final TypeReference<Map<String, Collection<String>>> MAP_LIST_REFERENCE = new TypeReference<Map<String, Collection<String>>>() {
    };

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectReader reader = mapper.reader(MAP_LIST_REFERENCE);

    public byte[] marshal(final Object o) {
        try {
            return mapper.writeValueAsBytes(o);
        } catch (final IOException e) {
            throw new IllegalStateException("JSON mapping failed", e);
        }
    }

    public Map<String, Collection<String>> unmarshal(final byte[] jsonBytes) {
        try {
            return reader.readValue(jsonBytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}