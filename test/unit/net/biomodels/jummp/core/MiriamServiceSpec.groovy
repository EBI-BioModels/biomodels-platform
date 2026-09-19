package net.biomodels.jummp.core

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/**
 * Covers JBM-776: MiriamService used to open a connection to the identifiers.org registry from inside
 * updateMiriamResources(), so any test of it depended on the network. The connection now goes through
 * openRegistryExport(), which these specs stub.
 *
 * Also covers JBM-781: a refresh that fails, or that downloads something that is not a registry export, must leave
 * the export that is already there as it was, instead of truncating it.
 */
@TestFor(MiriamService)
class MiriamServiceSpec extends Specification {
    private static final String PREVIOUS_EXPORT = "<miriam>previous</miriam>"
    // the shape of the real export, which has a default namespace
    private static final String NEW_EXPORT = '<?xml version="1.0" encoding="UTF-8"?>' +
        '<miriam xmlns="http://www.biomodels.net/MIRIAM/"><datatype id="MIR:00000001"/></miriam>'
    // what identifiers.org's home page, where the old export URL redirects to, looks like
    private static final String HOME_PAGE = "<!DOCTYPE html><html><head><title>Identifiers.org</title></head></html>"

    private File workingDirectory
    private File registryExport
    private List<String> requestedUrls

    void setup() {
        workingDirectory = Files.createTempDirectory("miriam-").toFile()
        registryExport = new File(workingDirectory, "miriam.xml")
        service.registryExport = registryExport
        requestedUrls = []
    }

    void cleanup() {
        workingDirectory.deleteDir()
    }

    private void existingExport() {
        registryExport.text = PREVIOUS_EXPORT
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

    void "a refresh that fails leaves the existing export as it was"() {
        given:
        existingExport()
        stubRegistry { throw new IOException("registry unreachable") }

        when:
        service.updateMiriamResources("http://registry.example.org/export")

        then:
        registryExport.text == PREVIOUS_EXPORT
        workingDirectory.list() as List == ["miriam.xml"]
    }

    void "an empty download leaves the existing export as it was"() {
        given: "the 301 the old URL answers with makes openStream() return no bytes and no error"
        existingExport()
        stubRegistry { new ByteArrayInputStream(new byte[0]) }

        when:
        service.updateMiriamResources("http://registry.example.org/export")

        then:
        registryExport.text == PREVIOUS_EXPORT
        workingDirectory.list() as List == ["miriam.xml"]
    }

    @Unroll("#description leaves the existing export as it was")
    void "a download that is not a registry export leaves the existing export as it was"() {
        given:
        existingExport()
        stubRegistry { new ByteArrayInputStream(body.getBytes("UTF-8")) }

        when:
        service.updateMiriamResources("http://registry.example.org/export")

        then:
        registryExport.text == PREVIOUS_EXPORT
        workingDirectory.list() as List == ["miriam.xml"]

        where:
        description                        | body
        "an HTML page"                     | HOME_PAGE
        "XML with another root element"    | "<registry><datatype/></registry>"
        "a download cut off half way"      | "<miriam><datatype id='MIR:00000001'>"
        "plain text"                       | "301 Moved Permanently"
    }

    void "a verified download replaces the existing export"() {
        given:
        existingExport()
        stubRegistry { new ByteArrayInputStream(NEW_EXPORT.getBytes("UTF-8")) }

        when:
        service.updateMiriamResources("http://registry.example.org/export")

        then:
        registryExport.text == NEW_EXPORT
        workingDirectory.list() as List == ["miriam.xml"]
    }

    void "a verified download creates the export if there is none yet"() {
        given:
        assert !registryExport.exists()
        stubRegistry { new ByteArrayInputStream(NEW_EXPORT.getBytes("UTF-8")) }

        when:
        service.updateMiriamResources("http://registry.example.org/export")

        then:
        registryExport.text == NEW_EXPORT
    }

    void "the refreshed export can be read by other users, as one written in place would be"() {
        given: "the indexer that reads it may run as another user, and a temporary file is only readable by its owner"
        existingExport()
        stubRegistry { new ByteArrayInputStream(NEW_EXPORT.getBytes("UTF-8")) }

        when:
        service.updateMiriamResources("http://registry.example.org/export")

        then:
        Files.getPosixFilePermissions(registryExport.toPath()).contains(PosixFilePermission.OTHERS_READ)
    }
}
