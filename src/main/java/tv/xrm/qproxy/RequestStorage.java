package tv.xrm.qproxy;

import java.io.IOException;
import java.util.List;

public interface RequestStorage {

    String createRequestId();

    /**
     * Store the given request, returning a unique ID.
     */
    void store(Request request, String id) throws IOException;

    /**
     * Retrieve the request identified by ID.
     */
    Request retrieve(String id) throws IOException;

    /**
     * Retrieve all requests currently in storage. Typically used for cleanup of leftover requests.
     */
    List<Request> retrieve();

    /**
     * Delete the request identified by id, if it exists. Do not complain if it's null or invalid.
     */
    void delete(String id);

}
