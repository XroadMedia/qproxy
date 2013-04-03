package tv.xrm.qproxy;

import java.io.IOException;
import java.util.List;

public interface RequestStorage {

    /**
     * Store the given request, returning a unique ID.
     */
    String store(Request request) throws IOException;

    /**
     * Retrieve the request identified by ID.
     */
    Request retrieve(String id) throws IOException;

    /**
     * Retrieve all requests currently in storage. Typically used for cleanup of leftover requests.
     */
    List<Request> retrieve();

    /**
     * Delete the request identified by ID, if it exists.
     */
    void delete(String id);

}
