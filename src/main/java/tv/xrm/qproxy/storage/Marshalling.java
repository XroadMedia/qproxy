package tv.xrm.qproxy.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        reader = mapper.readerFor(TYPE_REFERENCE);
    }

    public byte[] marshal(final FileStorage.StorageBlock o) throws JsonProcessingException {
        return writer.writeValueAsBytes(o);
    }

    public FileStorage.StorageBlock unmarshal(final byte[] jsonBytes) throws IOException {
        return reader.readValue(jsonBytes);
    }
}