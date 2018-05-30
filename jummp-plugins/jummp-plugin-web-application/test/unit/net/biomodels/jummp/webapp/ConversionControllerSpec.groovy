package net.biomodels.jummp.webapp

import grails.test.mixin.TestFor
import net.biomodels.jummp.core.IModelConversionService
import net.biomodels.jummp.core.IModelService
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ConversionController)
class ConversionControllerSpec extends Specification {

    void "test convert"() {
        setup: "declare essential variables"
        def modelDelegateService = mockFor(IModelService)

        modelDelegateService.demand.getRevisionFromParams = { modelId, revisionId ->
            new RevisionTransportCommand(model: new ModelTransportCommand(
                submissionId: modelId
            ), revisionNumber: revisionId)
        }
        controller.modelDelegateService = modelDelegateService.createMock()
        IModelConversionService modelConversionService = Mock()
        controller.modelConversionService = modelConversionService
        when: "hit convert method"
        params.id = "MODEL0000000600"
        params.revisionId = "1"
        controller.convert()

        then: "response a text"
        log.info(response.text)
        response.text
    }
}
