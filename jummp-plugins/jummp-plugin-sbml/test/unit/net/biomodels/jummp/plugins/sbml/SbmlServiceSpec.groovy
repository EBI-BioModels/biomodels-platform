package net.biomodels.jummp.plugins.sbml

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(SbmlService)
class SbmlServiceSpec extends Specification {

    @Unroll("run this method areFilesThisFormat() with file #sbmlFile: #expected")
    void "test areFilesThisFormat() with different sbmlFile"(String sbmlFile, boolean expected) {
        expect: "sbmlService is fired up"
        service

        when: "feed the file #sbmlFile to the method"
        def f = new File("test/files/$sbmlFile".toString())
        f.exists()
        boolean result = service.areFilesThisFormat([f])

        then: "the method returns an expected result"
        result == expected

        where: "the data table"
        sbmlFile                 | expected
        "BIOMD0000000272.xml"    | true
        "fbc_example1.xml"       | true
        "Koch2017.xml"           | true    // <sbml tag and xmlns attribute are not the same line
        "NonSBML.xml"            | false   // this is a dummy xml file
    }

    void "valid SBML models do not throw errors"(String fileName, def result) {
        expect:
        service != null

        when:
        def f = new File("test/files/$fileName".toString())
        def err = []
        def doc = service.getFileAsValidatedSBMLDocument(f, err)

        then:
        f.exists()
        err == []
        doc != null

        where:
        fileName                | result
        "BIOMD0000000272.xml"   | _
        "fbc_example1.xml"      | _
    }

    void "capture error messages when invalid SBML models throw"(String sbmlFileName, String expectedErrors) {
        when:
        def f = new File("test/files/$sbmlFileName".toString())
        def actualErrors = []
        def sbmlDoc = service.getFileAsValidatedSBMLDocument(f, actualErrors)

        then:
        !sbmlDoc
        actualErrors
        1 == actualErrors.size()
        actualErrors[0].contains(expectedErrors)

        where:
        sbmlFileName                    | expectedErrors
        "Phan2017.xml"                  | 'Undeclared namespace prefix "bqbio"'
        "tiny_example_12.xml"           | 'Unexpected close tag </bqmodel:are>; expected </bqmodel:is>'
    }

    void "we can extract FBC-related stuff without errors"() {
        when:
        def f = new File("test/files/fbc_example1.xml")
        def doc = service.getFileAsValidatedSBMLDocument(f, [])
        def m = doc.getModel()

        then:
        m.isSetPlugin("fbc")

        when:
        def plugin = m.getPlugin("fbc")

        then:
        plugin != null

        when:
        def fluxBounds = plugin.getListOfFluxBounds()
        def fb = fluxBounds.get(0)

        then:
        1 == fluxBounds.size()
        "bound1" == fb.getId()
        "J0" == fb.getReaction()
        "EQUAL" == fb.getOperation().name()
        10 == fb.getValue()
    }
}
