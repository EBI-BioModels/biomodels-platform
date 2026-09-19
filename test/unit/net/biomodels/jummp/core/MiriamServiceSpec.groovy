package net.biomodels.jummp.core

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Covers JBM-776: MiriamService used to open a connection to the identifiers.org registry from inside
 * updateMiriamResources(), so any test of it depended on the network. The connection now goes through
 * openRegistryExport(), which these specs stub.
 */
@TestFor(MiriamService)
class MiriamServiceSpec extends Specification {
    private File registryExport
    private List<String> requestedUrls

    void setup() {
        registryExport = File.createTempFile("miriam-", ".xml")
        service.registryExport = registryExport
        requestedUrls = []
    }

    void cleanup() {
        registryExport.delete()
    }

    private void stubRegistry(Closure stream) {
        service.metaClass.openRegistryExport = { String url ->
            requestedUrls << url
            stream.call()
        }
    }

    void "updateMiriamResources saves the export it is given, without reaching the network"() {
        given:
        stubRegistry { new ByteArrayInputStream("<miriam>registry</miriam>".getBytes("UTF-8")) }

        when:
        service.updateMiriamResources("http://registry.example.org/export")

        then:
        requestedUrls == ["http://registry.example.org/export"]
        registryExport.text == "<miriam>registry</miriam>"
    }

    void "updateMiriamResources asks for the default export URL if it is not given one"() {
        given:
        stubRegistry { new ByteArrayInputStream("<miriam/>".getBytes("UTF-8")) }

        when:
        service.updateMiriamResources()

        then:
        requestedUrls == [service.DEFAULT_EXPORT_URL]
    }

    void "updateMiriamResources logs an unreachable registry instead of throwing"() {
        given:
        stubRegistry { throw new IOException("registry unreachable") }

        when:
        service.updateMiriamResources("http://registry.example.org/export")

        then:
        notThrown(IOException)
        requestedUrls.size() == 1
    }
}
