package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class ParameterSearchService {
    static transactional = false

    static final Log log = LogFactory.getLog(ParameterSearchService.class)


    ParameterSearchResults getData(ParameterSearchCommand command) {
        if (!command) {
            throw new IllegalArgumentException("Couldn't read the request parameters");
        }
        def url = command.getSearchUrl()
        def searchResults = url.text
        return ParameterSearchResults.fromJson(JSON.parse(searchResults))
    }
}
