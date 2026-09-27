# Jetty as embedded HTTP server

## Status

Adopted

## Decision

To expose RESTful APIs over HTTP, the curator framework uses Jetty as its embedded HTTP server.

## Context

To increase the applicability of the curator framework, especially for automation purposes, it will expose a set of RESTful APIs over HTTP locally. Through such APIs, Markdown content can be exposed, as well as queries.

To do so, the curator framework needs an embedded HTTP server. Several HTTP server libraries exist for Java. One needs to be picked.

Enabling the HTTP server must be optional. By default it must be disabled.

## Options considered

1. [JBoss Undertow](https://undertow.io)
2. [Eclipse Jetty](https://jetty.org)
3. [Apache Tomcat](https://tomcat.apache.org)

Spring Boot (that the curator framework is **not** using!) supports Apache Tomcat (the default), Eclipse Jetty and JBoss Undertow. So these are the logical ones to consider.

I have a long history with both Jetty and Tomcat. I've always liked Jetty, and I've always disliked Tomcat. This knowledge is outdated, I admit, but I remember Tomcat as a J2EE beast: hungry for memory, and the internal code that I once had to review was little short of terrible. Jetty, on the other hand, was compact and sweet.

A comparison between the three can be found in [this Java Code Geeks article](https://examples.javacodegeeks.com/java-development/enterprise-java/spring/tomcat-vs-jetty-vs-undertow-comparison-of-spring-boot-embedded-servlet-containers/),

Undertow scores best when it comes to memory usage: it uses the least of it. That's a decisive factor. Jetty comes second.

I played around with Undertow (in this repository) and found the documentation lacking. The last message on the blog is from May 2020 and it describes release 2.1.0. The latest version today (July 2025) is 2.3.18. There's no mention of it on the site. The documentation goes no further than 2020. That's another decisive factor. Undertow comes last here.

So, I've decided to use Jetty. It's small enough, I have positive experiences with it from the past, and it's well-documented. It does mean I have to rebuild the few handlers I already built in Undertow.