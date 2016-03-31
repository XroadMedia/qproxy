package tv.xrm.qproxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InMemoryStorage implements RequestStorage {
    private final Map<String, Request> requestMap = new HashMap<>();
    private int idCounter = 0;

    @Override
    public String createRequestId() {
        return Integer.toString(idCounter++);
    }

    @Override
    public void store(Request request, String id) throws IOException {
        requestMap.put(id, Request.withId(request, id));
    }

    @Override
    public Request retrieve(String id) throws IOException {
        Request r = requestMap.get(id);
        if (r == null) {
            throw new IOException("not found");
        }
        return r;
    }

    @Override
    public List<Request> retrieve() {
        return new ArrayList<>(requestMap.values());
    }

    @Override
    public void delete(String id) {
        requestMap.remove(id);
    }
}
