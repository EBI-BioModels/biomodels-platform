package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import grails.plugin.cache.Cacheable
import groovy.transform.CompileStatic
import groovyx.gpars.GParsPool
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import org.grails.async.factory.gpars.LoggingPoolFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ParameterSearchService {
    static transactional = false
    def static configurationService

    static final Logger LOGGER = LoggerFactory.getLogger(ParameterSearchService.class)
    static List<String> columnNames = ["entity", "entity_id", "initial concentration/amount", "reaction with entity labels", "reaction with entity ids",
                                        "reactants", "products", "modifiers",
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
        if (!searchResults) { return null }
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
                    LOGGER.error("Could not retrieve batch $page of $batchCount for query $query", t)
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
        HttpURLConnection conn
        Proxy proxy = configurationService.verifyHttpProxy()
        try {
            if (proxy) {
                conn = (HttpURLConnection) url.openConnection(proxy)
            } else {
                conn = (HttpURLConnection) url.openConnection()
            }
            /**
             * https://docs.oracle.com/javase/8/docs/api/java/net/URLConnection.html
             * https://stackoverflow.com/a/6830053/865603
             * https://www.baeldung.com/java-socket-connection-read-timeout
             *
             * The server often accepts the client connection, especially inter-connected services. So, we
             * don't need to pump up the specific time for the connection timeout property. Instead of
             * increasing the connection time out, is is recommended to increase the time for the read time out.
             *
             * From the client side, the "read timed out" error happens if the server is taking longer to
             * respond and send information. This could be due to a slow internet connection, or the host
             * could be offline. From the server side, it happens when the server takes a long time to
             * read data compared to the preset timeout.
             */
            conn.setConnectTimeout(15000)
            conn.setReadTimeout(30000)
            conn.connect()
            if (conn.responseCode < 400) {
                try {
                    String records = conn.getInputStream().text
                    return replaceFieldNames(records).replaceAll("\\\\","")
                } catch (IOException e) {
                    LOGGER.error("""Error while getting data from HttpUrlConnection ${conn.dump()} because of \
the error ${e.message}""")
                    return null
                }
            } else {
                LOGGER.error("""Couldn't fetch data from the resource ${url.dump()} because of the error \
caused by ${conn.getErrorStream().inspect()}""")
                return null
            }
        } catch (SocketTimeoutException ste) {
            LOGGER.error("""Error while retrieving records from EBI Search ${ste.getMessage()}, \
command - ${command}""".toString(), ste)
        } catch (IllegalArgumentException iae) {
            LOGGER.error("The proxy setting cannot be null or ${iae.getMessage()}")
        } finally {
            conn.getInputStream().close()
        }
        return null
    }
}
