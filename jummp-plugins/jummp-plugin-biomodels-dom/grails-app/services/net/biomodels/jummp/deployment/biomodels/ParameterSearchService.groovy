package net.biomodels.jummp.deployment.biomodels


import grails.converters.JSON
import grails.plugin.cache.Cacheable
import groovy.transform.CompileStatic
import groovyx.gpars.GParsPool
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.grails.async.factory.gpars.LoggingPoolFactory

class ParameterSearchService {
    static transactional = false

    static final Log log = LogFactory.getLog(ParameterSearchService.class)
    static List<String> columnNames = ["entity", "entity_id", "initial concentration/amount", "reaction with entity labels", "reaction with entity ids",
                                       "model", "organism", "publication",
                                       "rate with entity labels", "rate with entity ids","parameters", "entity access url", "reaction SBO link",
                                       "entity SBO link","external links"]

    static String replaceFieldNames(String data) {
        data = data.replaceAll("entity_RAW", "entity")
        data = data.replaceAll("initial_data_RAW", "initial_data")
        data = data.replaceAll("reaction_RAW", "reaction")
        data = data.replaceAll("rate_RAW", "rate")
        data = data.replaceAll("parameters_RAW", "parameters")
        return data
    }

    ParameterSearchResults getJSONData(ParameterSearchCommand command) {
        String searchResults = getData(command, "JSON")
        return ParameterSearchResults.fromJson(JSON.parse(searchResults))
    }

    String getCSVData(ParameterSearchCommand command) {
        return getData(command, "CSV")
    }

    @CompileStatic
    @Cacheable(value = "csvRecords", key = "#command.query.concat(#command.is_curated)")
    String exportData(ParameterSearchCommand command) {
        int MAX_RECORDS = 100
        LoggingPoolFactory
        String csvRecords = null
        ParameterSearchResults parameterSearchResults = getJSONData(command)
        command.size = MAX_RECORDS
        int recordsTotal = parameterSearchResults.recordsTotal

        if (recordsTotal > MAX_RECORDS) {
            csvRecords = assembleSearchResultsUsingGPars(command, recordsTotal, MAX_RECORDS)
        } else {
            csvRecords = removeHeader(getCSVData(command))
        }

        if(!csvRecords.isEmpty()) {
            csvRecords = "\"" + columnNames.join("\",\"") + "\"\n" +csvRecords;
        }

        return csvRecords
    }

    String assembleSearchResultsUsingGPars(ParameterSearchCommand command, int total, int MAX_RECORDS) {
        final int batchCount = Math.floor(total / MAX_RECORDS)
        def searchResults = null
        GParsPool.withPool(100) {
            searchResults = (0..batchCount).collectParallel { int page ->
                String query = command.query
                def thisCmd = new ParameterSearchCommand(query: query, start: page * MAX_RECORDS,
                    size: MAX_RECORDS, is_curated: command.is_curated)
                String result = ""
                try {
                    result = removeHeader(getCSVData(thisCmd))
                } catch (Throwable t) {
                    log.error("Could not retrieve batch $page of $batchCount for query $query", t)
                }

                return result
            }
        }

        return searchResults?.join("")
    }

    private static String removeHeader(String csvData) {
        if (null == csvData) return null
        int indexOfNewLineChar = csvData.indexOf("\n")
        return csvData.substring(indexOfNewLineChar + 1)
    }

    private static String getData(ParameterSearchCommand command, String format) {
        if (!command) {
            throw new IllegalArgumentException("Couldn't read the request parameters");
        }
        def url = command.getSearchUrl(format)
        String records = ""
        try {
            records = url.text
        } catch (SocketException se) {
            log.error("Error while retrieving records from EBI Search ${se.getMessage()}, command - ${command}")
        }
        return replaceFieldNames(records).replaceAll("\\\\","")
    }
}
