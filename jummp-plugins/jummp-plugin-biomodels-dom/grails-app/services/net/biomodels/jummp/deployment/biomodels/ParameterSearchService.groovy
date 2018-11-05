package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class ParameterSearchService {
    static transactional = false

/*    function preProcessEbiSearchParams(urlParams) {
        // Global Filtering
        var query;

        urlParams.query = query;

        // Sorting
        urlParams.order.forEach(function (obj) {
            var column = urlParams.columns[obj.column];
            var columnName = column.data.replace("fields.", "");
            var sortDirection = obj.dir;
            urlParams.sortfield = columnName;
            if (sortDirection === "desc") {
                urlParams.order = "descending";
            }
            else if (sortDirection === "asc") {
                urlParams.order = "ascending";
            }
        });
        urlParams.size = urlParams.length;
        delete urlParams[search];
        delete urlParams[length];
        return urlParams;
    }*/
    static final Log log = LogFactory.getLog(ParameterSearchService.class)


    ParameterSearchResults getData(ParameterSearchCommand command) {
        if (!command) {
            throw new IllegalArgumentException("Couldn't read the request parameters");
        }
        def url = command.getSearchUrl()
        log.info (url)
        def searchResults = url.text
        return ParameterSearchResults.fromJson(JSON.parse(searchResults))
    }
}
