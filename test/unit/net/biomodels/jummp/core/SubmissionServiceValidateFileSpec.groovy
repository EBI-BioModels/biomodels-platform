package net.biomodels.jummp.core

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Covers JBM-798: SubmissionService.validateFile is what the web wizard's upload step calls for every file it gets, and
 * it shows the messages to the submitter.
 */
@TestFor(SubmissionService)
class SubmissionServiceValidateFileSpec extends Specification {
    private File dir

    void setup() {
        dir = File.createTempDir("submission-validate-file", "")
    }

    void cleanup() {
        dir.deleteDir()
    }

    void "a file with content has no errors"() {
        given:
        File file = new File(dir, "model.xml")
        file.text = "<sbml/>"

        expect:
        service.validateFile(file) == []
    }

    void "an empty file is reported"() {
        given:
        File file = new File(dir, "empty.xml")
        file.text = ""

        expect:
        service.validateFile(file) == ["The file is empty"]
    }

    void "a file that does not exist is not called empty"() {
        expect:
        service.validateFile(new File(dir, "missing.xml")) == ["File does not exist"]
    }

    void "a directory is not called empty"() {
        expect:
        service.validateFile(dir) == ["The model file cannot be a directory"]
    }
}
