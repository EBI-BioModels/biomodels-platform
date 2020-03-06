package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ParameterSearchService)
@TestMixin(ServiceUnitTestMixin)

class ParameterSearchServiceSpec extends Specification {


    private static ParameterSearchCommand prepareCommandObject(Map bindingMap) {
        return new ParameterSearchCommand(bindingMap)

    }
    private static isDataExists(String data) {
        String[] responseArray =  data.split("\n")
        return responseArray.length  > 1

    }

    void "test ParameterSearchService positively"() {

        given: "A parameter search command object is defined with basic criteria"
        ParameterSearchCommand command = prepareCommandObject([query: "BIOMD0000000292", size: 10, start: 0, sort: "entity:ascending", is_curated: true])

        when: "The service method's get data is called and parameter search command is valid"
        command.validate()

        ParameterSearchResults results = service.getJSONData(command)

        then: "it should return correct number of records"
        results.recordsTotal > 0

        and: "it should return correct records"


        String expectedReaction = "([3-phospho-D-glyceric acid; D-ribulose 1,5-bisphosphate; 3-Phospho-D-glycerate; D-Ribulose " +
            "1,5-bisphosphate] + [NADPH; C00005] + [ATP; C00002]) => ([dihydroxyacetone phosphate; aldehydo-D-ribose 5-phosphate; " +
            "D-ribulose 5-phosphate; keto-D-fructose 1,6-bisphosphate; keto-D-fructose 6-phosphate; D-erythrose 4-phosphate; sedoheptulose 1,7-bisphosphate; sedoheptulose 7-phosphate; " +
            "D-glyceraldehyde 3-phosphate; C00111; D-Ribose 5-phosphate;; D-Ribulose " +
            "5-phosphate; Sedoheptulose 7-phosphate;; Sedoheptulose 1,7-bisphosphate; D-Erythrose 4-phosphate; C00085; " +
            "C00354; C00118] + [C00008; ADP] + [NADP(+); C00006])"
        String expectedEntityId = "Y"
        String expectedModel = "BIOMD0000000292"
        String expectedOrganism = "Viridiplantae"

        results.entries.find { value ->
            value.fields.reaction == expectedReaction &&
                value.fields.entity_id == expectedEntityId &&
                value.fields.model == expectedModel &&
                value.fields.organism == expectedOrganism
        }
        and: "Number of records per page should be as per size value"
        10 == results.entries.size()

    }

    void "test ParameterSearchService Negatively"() {

        given: "A parameter search command object is defined with basic criteria"
        ParameterSearchCommand command = prepareCommandObject([query: "NON_MATCHING_QUERY", size: 10, start: 0, sort: "entity:ascending", is_curated: true])

        when: "The service method's get data is called and parameter search command is valid"

        command.validate()
        ParameterSearchResults results = service.getJSONData(command)

        then: "it should return correct number of records"
        0 == results.recordsTotal

    }
    void "test ParameterSearchService with non-curated query"() {

        given: "A parameter search command object is defined with basic criteria"
        ParameterSearchCommand command = prepareCommandObject([query: "Mus musculus", size: 10, start: 0, sort: "entity:ascending", is_curated:false])

        when: "The service method's get data is called and parameter search command is valid"

        command.validate()
        ParameterSearchResults results = service.getJSONData(command)

        then: "it should return correct number of records"
        String expectedModel = "MODEL1410060000"
        results.entries.find{value ->
            expectedModel == value.fields.model

        }
    }

    void "test ParameterSearchService With CSV data"() {

        given: "A parameter search command object is defined with basic criteria"
        ParameterSearchCommand command = prepareCommandObject([query: "BIOMD0000000292", size: 10, start: 0, sort: "entity:ascending", is_curated: true])

        when: "The service method's get data is called and parameter search command is valid"

        command.validate()
        String results = service.getCSVData(command)

        then: "it should return correct number of records"
        results.indexOf("[C00008; ADP]") != -1
        results.indexOf("entity_RAW") == -1
        results.indexOf("reaction_RAW") == -1
    }

     @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void "concurrent requests to fetch CSV data complete successfully"() {
        given:
        final int WORKER_COUNT = 4
        CountDownLatch start = new CountDownLatch(1)
        CountDownLatch end  = new CountDownLatch(WORKER_COUNT)
        final List queries = [ "*:*", "E4P*", "acetyl", "yeast" ]
        final int queryCount = queries.size()
        List<SimpleWorker> workers = []
        List<Thread> threads = []

        for (int i = 0; i < WORKER_COUNT; ++i) {
            int queryIndex = ThreadLocalRandom.current().nextInt(queryCount)
            String q = queries.get(queryIndex)
            def worker = new SimpleWorker(start, end, q, service)
            workers.add worker
            def thread = new Thread(worker)
            thread.setName(worker.query)
            thread.start()
        }

        when:
        // start querying the EBI Search concurrently
        start.countDown()
        end.await()

        then:
        def exceptions = workers.collect { w ->
            w.throwable
        }
        !exceptions.find { it != null }
        null == workers.find { w ->
            w.result.length() < 3
        }

        for (Thread t : threads) {
            try {
                t.join()
            } catch (Throwable err) {
                System.err.println("Could not stop thread ${t.name}: ${err.toString()}")
            }
        }
    }

    void "test export with csv format for all the records"() {
        given: "ParameterSearchService's exportData is called"
        ParameterSearchCommand command = prepareCommandObject([query: "*:*", size: 10, start: 0, sort: "model:ascending", is_curated: true])
        when : "Controller export method is invoked"
        String result = service.exportData(command)
        then: "Result should contain correct results"
        isDataExists(result)

    }

    void "test export with non-curated models"() {
        given: "ParameterSearchService's exportData is called"
        ParameterSearchCommand command = prepareCommandObject([query: "mus", size: 10, start: 0, sort: "model:ascending", is_curated: false])
        when : "Controller export method is invoked"
        String result = service.exportData(command)
        then: "Result should contain correct results"
        isDataExists(result)

    }
    void "test export with csv format for non matching query"() {
        given: "ParameterSearchService's exportData is called"
        ParameterSearchCommand command = prepareCommandObject([query: "NON_MATCHING_QUERY", size: 10, start: 0, sort: "model:ascending", is_curated: true])
        when : "Controller export method is invoked"
        then: "Result should contain empty results"
        service.exportData(command)?.isEmpty()
    }

    void "test export with is_curated flag and non_matching query"() {
        given: "ParameterSearchService's exportData is called"
        ParameterSearchCommand command = prepareCommandObject([query: "BIOMD0000000292", size: 10, start: 0, sort: "model:ascending", is_curated: false])
        when : "Controller export method is invoked"
        then: "Result should contain empty results"
        service.exportData(command)?.isEmpty()
    }
}
