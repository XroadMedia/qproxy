package tv.xrm.qproxy.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

final class Marshalling {
    private static final TypeReference<FileStorage.StorageBlock> TYPE_REFERENCE = new TypeReference<FileStorage.StorageBlock>() {
    };

    private final ObjectWriter writer;
    private final ObjectReader reader;

    public Marshalling() {
        ObjectMapper mapper = new ObjectMapper();
        writer = mapper.writer();
        reader = mapper.reader(TYPE_REFERENCE);
    }

    public byte[] marshal(final FileStorage.StorageBlock o) {
        try {
            return writer.writeValueAsBytes(o);
        } catch (final IOException e) {
            throw new IllegalStateException("JSON mapping failed", e);
        }
    }

    public FileStorage.StorageBlock unmarshal(final byte[] jsonBytes) {
        try {
            return reader.readValue(jsonBytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}