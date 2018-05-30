package net.biomodels.jummp.plugins.matlab

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(MatlabController)
class MatlabControllerSpec extends Specification {

    def setup() {
        def mockMetadataService = mockFor("metadataDelegateService")
        mockMetadataService.demand.fetchGenericAnnotations {
            []
        }
        controller.metadataDelegateService = mockMetadataService.createMock()

        def matlabService = mockFor "matlabService"
        matlabService.demand.getMdlFilesFromRevision = {
            new File(".")
        }
    }

    def cleanup() {
    }

    void "test something"() {
        expect:
        controller
        flash
    }
}
