package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class ParameterSearchService {
    static transactional = false

    static final Log log = LogFactory.getLog(ParameterSearchService.class)


    private static String getData(ParameterSearchCommand command, String format) {
        if (!command) {
            throw new IllegalArgumentException("Couldn't read the request parameters");
        }
        def url = command.getSearchUrl(format)
        String searchResults = url.text
        String modifiedData = replaceFieldNames(searchResults)
        return modifiedData
    }
    static String replaceFieldNames(String data) {

        data = data.replaceAll("entity_RAW","entity")
        data = data.replaceAll("initial_data_RAW","initial_data")
        data = data.replaceAll("reaction_RAW","reaction")
        data = data.replaceAll("rate_RAW","rate")
        data = data.replaceAll("parameters_RAW","parameters")
        return data

    }

    ParameterSearchResults getJSONData(ParameterSearchCommand command) {
        String searchResults = getData(command, "JSON")
        return ParameterSearchResults.fromJson(JSON.parse(searchResults))
    }

    String getCSVData(ParameterSearchCommand command) {
        return getData(command, "CSV")
    }



}
