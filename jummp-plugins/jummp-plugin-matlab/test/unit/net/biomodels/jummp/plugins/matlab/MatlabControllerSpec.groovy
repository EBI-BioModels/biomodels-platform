package net.biomodels.jummp.plugins.matlab

import grails.test.mixin.TestFor
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import spock.lang.Specification

@TestFor(MatlabController)
class MatlabControllerSpec extends Specification {

    def setup() {
        def mockMetadataService = mockFor(MetadataDelegateService)
        mockMetadataService.demand.fetchGenericAnnotations {
            []
        }
        controller.metadataDelegateService = mockMetadataService.createMock()

        def matlabService = mockFor MatlabService
        matlabService.demand.getMatlabFilesFromRevision = {
            new File(".")
        }
        controller.matlabService = matlabService.createMock()
    }

    def cleanup() {
    }

    void "test something"() {
        given:
        flash['genericModel'] = [revision: new RevisionTransportCommand(name: "Foo")]

        when:
        controller.show()

        then:
        response.text.contains("application.properties")
    }
}
