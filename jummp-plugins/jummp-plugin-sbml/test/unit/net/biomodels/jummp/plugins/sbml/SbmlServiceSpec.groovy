package net.biomodels.jummp.plugins.sbml

import grails.test.mixin.TestFor
import net.biomodels.jummp.core.model.CurationState
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.sbml.jsbml.CVTerm
import org.sbml.jsbml.SBMLDocument
import org.sbml.jsbml.SBMLReader
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.LocalTime

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

    void profileConsistencyChecks() {
        when: "we have a large model"
        File f = new File("test/files/MODEL1707110056.xml")
        SBMLDocument d = new SBMLReader().readSBML(f)
        Map offlineResponse = timeIt { d.checkConsistencyOffline() }
        println "offline $offlineResponse"

        then: "the model's offline consistency checks complete within 10 seconds"
        0 == offlineResponse['result']
        10_000L > offlineResponse['time']

        when: "we perform the consistency checks through http://sbml.org/Facilities/Validator/"
        Map remoteResponse = timeIt { d.checkConsistency() }
        println "online $remoteResponse"

        then: "we get the same result, but it takes longer"
        0 == remoteResponse['result']
        30_000 > remoteResponse['time']
        offlineResponse['time'] < remoteResponse['time']
    }

    private static Map timeIt(Closure c) {
        LocalTime start = LocalTime.now()
        def response = [:]
        try {
            def result = c.call()
            response['result'] = result
        } finally {
            Duration time = Duration.between(start, LocalTime.now())
            response['time'] = time.toMillis()
        }
        response
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

    @Unroll("""\
run this test with arguments #targetQualifier, #identifiersToBeInserted, #origIdentifiers, #updated and #nbOrigQuals 
against the document #documentPath""")
    void "test model annotations get inserted"(String documentPath,
                                               CVTerm.Qualifier targetQualifier,
                                               String accessionPattern,
                                               List origIdentifiers,
                                               List updated,
                                               int nbOrigQuals,
                                               String... identifiersToBeInserted) {
        when:
        SBMLDocument orig = new SBMLReader().readSBML(documentPath)
        List<CVTerm> origQualifierList = orig.model.annotation.filterCVTerms(targetQualifier)
        List<String> origAnnotations = origQualifierList*.resources.flatten()

        then:
        origQualifierList.size() == nbOrigQuals
        origAnnotations == origIdentifiers

        when:
        def rf = new RepositoryFileTransportCommand(path: documentPath, mainFile: true, description: "model file")
        def model = new ModelTransportCommand(submissionId: "MODEL0123456789")
        def format = new ModelFormatTransportCommand(identifier: "SBML")
        RevisionTransportCommand rev = new RevisionTransportCommand(model: model, state: ModelState.UNPUBLISHED,
            revisionNumber: 1, owner: "me", minorRevision: false, validated: true, name: "my model", format: format,
            description: "desc", uploadDate: new Date(), files: [rf], curationState: CurationState.CURATED)

        SBMLDocument doc = new SBMLReader().readSBML(documentPath)
        boolean isOk = service.addAnnotationsIfNeeded(rev, doc, targetQualifier,
            accessionPattern, identifiersToBeInserted)
        def modifiedQualifiers = doc.model.annotation.filterCVTerms(targetQualifier)
        then:
        isOk
        modifiedQualifiers.size() == 1
        modifiedQualifiers.resources.flatten() == updated

        where: "samples"
        documentPath << ["test/files/BIOMD0000000272.xml",
                         "test/files/BIOMD0000000654.xml"]
        targetQualifier << [CVTerm.Qualifier.BQM_IS, CVTerm.Qualifier.BQB_HAS_PROPERTY]
        accessionPattern << ["biomodels.db[/:](BIOMD|MODEL)[0-9]{10}",
                             "mamo[/:]MAMO_[0-9]{7}"]
        origIdentifiers << [["http://identifiers.org/biomodels.db/MODEL1005260001",
                             "http://identifiers.org/biomodels.db/BIOMD0000000272"],
                            ["http://identifiers.org/mamo/MAMO_0000046",
                             "http://identifiers.org/teddy/TEDDY456",
                             "http://identifiers.org/teddy/TEDDY123",
                             "http://identifiers.org/mamo/MAMO_0000007",]]
        updated << [["http://identifiers.org/biomodels.db/MODEL1005260001",
                     "http://identifiers.org/biomodels.db/BIOMD0000000272"],
                    ["http://identifiers.org/teddy/TEDDY456",
                     "http://identifiers.org/teddy/TEDDY123",
                     "http://identifiers.org/mamo/MAMO_0000009"]]
        nbOrigQuals << [2, 3]
        identifiersToBeInserted << [["http://identifiers.org/biomodels.db/MODEL1005260001",
                                     "http://identifiers.org/biomodels.db/BIOMD0000000272"] as String[],
                                    ["http://identifiers.org/mamo/MAMO_0000009"] as String[]]
    }

    def "can preserve UTF-8 characters when reading SBML documents"() {
        when:
        def modelFile = new File("test/files/MODEL1707110056.xml")
        def name = service.extractName([modelFile])

        then:
        name == 'Uhlén2017 - TCGA-06-0139-01A - Glioblastoma Multiforme (male, 41 years)'
    }

    def "can preserve UTF-8 characters when updating SBML documents"() {
        when: "a revision of a model whose name contains non-ASCII characters"
        String documentPath = "test/files/MODEL1707110056.xml"
        String originalName = "Uhlén2017 - TCGA-06-0139-01A - Glioblastoma Multiforme (male, 41 years)"
        String newName = "'\u03A3\u03A3 Uhlén2017 \u03A3\u03A3''" // also include greek symbols alongside latin-1

        def rf = new RepositoryFileTransportCommand(path: documentPath, mainFile: true,
            description: "model file")
        def model = new ModelTransportCommand(submissionId: "MODEL0123456789")
        def format = new ModelFormatTransportCommand(identifier: "SBML")
        RevisionTransportCommand rev = new RevisionTransportCommand(model: model,
            state: ModelState.UNPUBLISHED, revisionNumber: 1, owner: "me", minorRevision: false,
            validated: true, name: originalName, format: format, description: "desc",
            uploadDate: new Date(), files: [rf], curationState: CurationState.CURATED)

        then: "we update the name"
        service.updateName(rev, newName)

        and:
        new SBMLReader().readSBML(documentPath).model.name == newName
        service.updateName(rev, originalName)
    }

}
