package net.biomodels.jummp.core

import com.sun.net.httpserver.HttpServer
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Covers JBM-781: what MiriamService.openRegistryExport() does with the answers a registry can give. It talks to a
 * server on the local machine, so nothing here needs the network. The old export URL answers 301 with a Location on
 * another protocol, which java.net.URL.openStream() does not follow and does not report either.
 */
@TestFor(MiriamService)
class MiriamServiceDownloadSpec extends Specification {
    private HttpServer server
    private String base

    void setup() {
        server = HttpServer.create(new InetSocketAddress(InetAddress.loopbackAddress, 0), 0)
        base = "http://127.0.0.1:${server.address.port}"
    }

    void cleanup() {
        server.stop(0)
    }

    /** Answers requests for the path with the status, headers and body. */
    private void answer(String path, int status, String body = "", Map<String, String> headers = [:]) {
        server.createContext(path) { exchange ->
            headers.each { k, v -> exchange.responseHeaders.add(k, v) }
            byte[] bytes = body.getBytes("UTF-8")
            exchange.sendResponseHeaders(status, bytes.length ? bytes.length : -1)
            if (bytes.length) {
                exchange.responseBody.withStream { it.write(bytes) }
            }
            exchange.close()
        }
    }

    void "the body of a 200 answer is returned"() {
        given:
        answer("/export", 200, "<miriam/>")
        server.start()

        expect:
        service.openRegistryExport("$base/export").text == "<miriam/>"
    }

    void "a redirect with a relative Location is followed"() {
        given:
        answer("/old", 301, "", [Location: "/new"])
        answer("/new", 200, "<miriam/>")
        server.start()

        expect:
        service.openRegistryExport("$base/old").text == "<miriam/>"
    }

    void "a redirect with an absolute Location is followed"() {
        given:
        answer("/old", 301, "", [Location: "$base/new"])
        answer("/new", 200, "<miriam/>")
        server.start()

        expect:
        service.openRegistryExport("$base/old").text == "<miriam/>"
    }

    void "an answer other than 200 is an error, not an empty stream"() {
        given:
        answer("/export", status, "not here")
        server.start()

        when:
        service.openRegistryExport("$base/export")

        then:
        IOException e = thrown()
        e.message.contains("HTTP $status")

        where:
        status << [404, 500]
    }

    void "a redirect that never ends is an error"() {
        given:
        answer("/loop", 302, "", [Location: "/loop"])
        server.start()

        when:
        service.openRegistryExport("$base/loop")

        then:
        IOException e = thrown()
        e.message.contains("redirect")
    }

    void "a redirect without a Location is an error"() {
        given:
        answer("/export", 301)
        server.start()

        when:
        service.openRegistryExport("$base/export")

        then:
        IOException e = thrown()
        e.message.contains("Location")
    }

    void "only http and https addresses are downloaded"() {
        when:
        service.openRegistryExport(address)

        then:
        thrown(IOException)

        where:
        address << ["file:///etc/hosts", "ftp://registry.example.org/export"]
    }
}
