qproxy
======

This is a "queueing proxy" for HTTP POST requests: it gets POST data and passes them on to another URL, buffering in between. The basic use case is introducing a message-queue-like entity between an HTTP client and server, without having to deal with messaging APIs and in particular without changing anything on the server side (it's only the client that needs to be changed, and only the URL it talks to).

qproxy is implemented as a Java servlet, so it needs a servlet container to run on (e.g. Tomcat, Jetty, GlassFish, JBoss AS, ...).

Limitations
-----------

This initial implementation does not give any delivery guarantees. A best effort is made to deliver messages exactly once, but they may also be lost or (unlikely, but possibly) delivered multiple times.

POST data are streamed to and from filesystem storage. This is meant to keep memory usage to a minimum even for large payloads, but may not result in the best throughput.

Each target URL is considered a logical queue, which is processed by a fixed number of concurrent HTTP client threads.
