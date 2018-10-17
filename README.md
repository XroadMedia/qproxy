qproxy
======

This is a "queueing proxy" for HTTP POST requests: it gets POST data and passes them on to another URL, buffering in between. The basic use case is introducing a message-queue-like entity between an HTTP client and server, without having to deal with messaging APIs and in particular without changing anything on the server side (it's only the client that needs to be changed, and only the URL it talks to).

qproxy is implemented as a Java servlet, so it needs a servlet container to run on (e.g. Tomcat, Jetty, GlassFish, JBoss AS, ...).


Build / Usage
-------------

You'll need at least Java SE 8, and additionally Maven 3 for building:

    mvn clean package

A simple way of deploying and running qproxy is using the Jetty plugin:

    mvn jetty:run

Once it's running, send your POST requests to the context root (in the above case, it's /) with the target url added as a request parameter called _url_. Example using [curl](http://curl.haxx.se/):

    curl -XPOST 'http://localhost:8080/?url=http://targethost/foo/bar' -d 'post data'

This will return immediately with an HTTP 202 (Accepted) code, and then qproxy will try to pass the request on to http://targethost/foo/bar as specified. This also includes the original request headers.

Runtime metrics can be found at /metrics:

    curl -XGET 'http://localhost:8080/metrics'


Configuration
-------------

Various parameters are set through a configuration file. The default is part of the WAR, an alternative overriding file can be set through a system property, e.g.

    java -Dqproxy.configFile=/etc/qproxy/myconfig.properties ...

### Mapping target URLs to queues

By default, each combination of the same protocol, host, port and the first path element (if any) are handled by the same queue. That is, http://foo:80/bar/bla and http://foo:80/bar/zap will be managed by one single queue. This behaviour can be controlled with the pathAggregationLevels configuration parameter.

Query parameters are never considered for this mapping.

Limitations (and possible roadmap items)
----------------------------------------

This initial implementation does not give any delivery guarantees. A best effort is made to deliver messages exactly once, but they may also be lost or (unlikely, but possibly) delivered multiple times. Ordering is not guaranteed either, although it will tend to be _roughly_ first-in-first-out.

POST data are streamed to and from filesystem storage. This is meant to keep memory usage to a minimum even for large payloads, but may not result in the best throughput. It also eats up open file descriptors, so you may have to increase the limits for those.

The internal message queues have a fixed size. This is inflexible but provides some form of back-pressure to the client.

Each queue is processed by a fixed number of concurrent HTTP client threads, and queues are never pruned. This is unsuitable for use cases with many different and/or constantly changing target URLs.

