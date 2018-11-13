package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.core.IModelService
import net.biomodels.jummp.model.Model
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ModelOfTheMonthController)
@Mock([Model, ModelOfTheMonth])
class ModelOfTheMonthControllerSpec extends Specification {

    void "test index action"() {
        given:
        def modelOfTheMonthService = mockFor(ModelOfTheMonthService)
        final Date pubDate = new Date()
        final Date mDate = new Date()

        modelOfTheMonthService.demand.list { ->
            def result = new ModelOfTheMonth(title: 'Dummy MoM entry', models: [new Model()])
            result.publicationDate = pubDate
            result.lastUpdated = mDate
            result.id = 1
            [result]
        }
        controller.modelOfTheMonthService = modelOfTheMonthService.createMock()

        when:
        def result = controller.index()
        then:
        result.entries.size() == 1
        def firstResult = result.entries.first()
        firstResult.id == 1
        firstResult.title == 'Dummy MoM entry'
        firstResult.publicationDate == pubDate
        firstResult.lastUpdated == mDate
    }

    void "test save action"() {
        given: "a params map and an initialised controller"
        params.title = 'a MoM for a random month'
        params.authors = 'Tung'
        params.formattedEntryDate = '2000-01-02'
        params.publicationDate = '2008-04-01T10:05:05'
        params.lastUpdated = '2008-04-01T11:00:00'
        params.models = "BIOMD0000000639, BIOMD0000000636"

        defineBeans {
            // trying to mock ModelDelegateService, which is outside of this plugin prevents
            // data binding for Date instance variables, using a dummy bean implementation instead
            modelDelegateService MockModelDelegateService
        }

        def modelOfTheMonthService = mockFor(ModelOfTheMonthService)
        modelOfTheMonthService.demand.doCreateOrUpdate { ModelOfTheMonthTransportCommand cmd ->
            def result = new ModelOfTheMonth(title: cmd.title, authors: cmd.authors,
                publicationDate: cmd.publicationDate,
                lastUpdated: cmd.lastUpdated,
                models: cmd.models)
            result.id = 45
            result
        }
        controller.modelOfTheMonthService = modelOfTheMonthService.createMock()

        /*def cmd = mockCommandObject(ModelOfTheMonthTransportCommand)
        cmd.modelDelegateService = mockModelDelegateService
        mockForConstraintsTests(ModelOfTheMonthTransportCommand, [cmd])
        controller.bindData(cmd, params)
        cmd.validate()*/

        /* def cmd = new ModelOfTheMonthTransportCommand(params)
        mockForConstraintsTests(ModelOfTheMonthTransportCommand, [cmd])
        cmd.modelDelegateService = mockModelDelegateService
        cmd.validate()*/

        when: "invoke save action"
        controller.save()

        then: "expect that the save action can save data successfully"
        response.json.message ==  "The record has been updated successfully"
        !response.json.entity.errors
        45 == response.json.id
    }
}

class MockModelDelegateService {
    Map<Long, String> findModelsByPerennialId(List<String> identifiers) {
        [1: "BIOMD0000000636", 2: "BIOMD0000000639"]
    }
}
