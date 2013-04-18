qproxy
======

This is a "queueing proxy" for HTTP POST requests: it gets POST data and passes them on to another URL, buffering in between. The basic use case is introducing a message-queue-like entity between an HTTP client and server, without having to deal with messaging APIs and in particular without changing anything on the server side (it's only the client that needs to be changed, and only the URL it talks to).

qproxy is implemented as a Java servlet, so it needs a servlet container to run on (e.g. Tomcat, Jetty, GlassFish, JBoss AS, ...).


Build / Usage
-------------

You'll need at least Java SE 7, and additionally Maven 3 for building:

    mvn clean package

A simple way of deploying and running qproxy is using the Jetty plugin:

    mvn jetty:run

Once it's running, send your POST requests to the context root (in the above case, it's /) with the target url added as a request parameter called _uri_. Example using [curl](http://curl.haxx.se/):

    curl -XPOST 'http://localhost:8080/?uri=http://targethost/foo/bar' -d 'post data'

This will return immediately with an HTTP 202 (Accepted) code, and then qproxy will try to pass the request on to http://targethost/foo/bar as specified. This also includes the original request headers.


Limitations
-----------

This initial implementation does not give any delivery guarantees. A best effort is made to deliver messages exactly once, but they may also be lost or (unlikely, but possibly) delivered multiple times. Ordering is not guaranteed either, although it will tend to be _roughly_ first-in-first out.

POST data are streamed to and from filesystem storage. This is meant to keep memory usage to a minimum even for large payloads, but may not result in the best throughput.

The internal message queue has a fixed size. This is inflexible but provides some form of back-pressure to the client.

Each target URL is considered a logical queue, which is processed by a fixed number of concurrent HTTP client threads.

Short-term Roadmap
------------------

Many parameters are currently hardwired, and should be configurable instead.

Undeliverable requests should be deleted after a configurable amount of time.

There is no facility for monitoring, except by parsing the log. The idea is to provide some monitoring data (probably collected using [Metrics](http://metrics.codahale.com) through a servlet.
