package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import grails.plugin.cache.Cacheable
import groovy.transform.CompileStatic
import groovyx.gpars.GParsPool
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand as ParamSC
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults as ParamSR
import net.biomodels.jummp.model.Model
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ParameterSearchService {
    static transactional = false
    def static configurationService
    def static redisService

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

    ParamSR getJSONData(ParamSC command, String modelId = null) {
        String searchResults = ""
        if (modelId) {
            searchResults = redisService.doRedisHGet("BP", modelId)
            LOGGER.debug("Retrieving parameters for ${modelId} from Redis cache.")
            println("Retrieving parameters for ${modelId} from Redis cache.")
        }
        if (!searchResults)  {
            // fall back to the live search on EBI Search Server
            LOGGER.debug("Falling back EBI Search Server to fetch parameters for the query: ${command}")
            println("Falling back EBI Search Server to fetch parameters for the query: ${command}")
            searchResults = getData(command, "JSON")
            // cache the search results on Redis
            doCacheSearchResultsOnRedis(searchResults, modelId)
        }
        if (!searchResults) { return null }
        return ParamSR.fromJson(JSON.parse(searchResults))
    }

    String getCSVData(ParamSC command) {
        return getData(command, "CSV")
    }

    @CompileStatic
    @Cacheable(value = "csvRecords", key = "#command.query.concat(#command.is_curated)")
    String exportData(ParamSC command) {
        int MAX_RECORDS = 100
        String csvRecords = null
        ParamSR parameterSearchResults = getJSONData(command)
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

    String assembleSearchResultsUsingGPars(ParamSC command, int total, int MAX_RECORDS) {
        final int batchCount = (int) Math.floor(total / MAX_RECORDS)
        def searchResults = null
        GParsPool.withPool(100) {
            searchResults = (0..batchCount).collectParallel { int page ->
                String query = command.query
                def thisCmd = new ParamSC(query: query, start: page * MAX_RECORDS,
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

    void updateRedisCache() throws SocketTimeoutException {
        // Notes: BP uses the public identifiers
        String query = """SELECT M.publicationId FROM Model AS M \
WHERE M.deleted = :deleted \
 AND M.firstPublished IS NOT NULL \
 AND M.publicationId IS NOT NULL \
 AND M.publicationId != '' \
 ORDER BY M.publicationId ASC"""
        List listOfModels = Model.executeQuery(query, [deleted: false])
        final int POOL_SIZE = 8
        GParsPool.withPool(POOL_SIZE) {
            listOfModels.eachParallel { String modelId ->
                ParamSC cmd = new ParamSC(size: 10, start: 0, sort: 'model:ascending')
                cmd.query = modelId
                String searchResults = getData(cmd, "JSON")
                doCacheSearchResultsOnRedis(searchResults, modelId)
            }
        }
    }

    private static String removeHeader(String csvData) {
        if (null == csvData) return null
        int indexOfNewLineChar = csvData.indexOf("\n")
        return csvData.substring(indexOfNewLineChar + 1)
    }

    private static String getData(ParamSC command, String format) {
        if (!command) {
            throw new IllegalArgumentException("Couldn't read the request parameters");
        }
        def url = command.getSearchUrl(format)
        HttpURLConnection conn
        Proxy proxy = configurationService.verifyHttpProxy()
        String result = null
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
             * increasing the connection time out, it is recommended to increase the time for the read time out.
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
                    result = replaceFieldNames(records).replaceAll("\\\\","")
                } catch (IOException e) {
                    LOGGER.error("""Error while getting data from HttpUrlConnection ${conn.dump()} because of \
the error ${e.message}""")
                } catch (SocketTimeoutException ste) {
                    String msg = """Error while trying to retrieve BioModels Paramters from EBI Search due to \
"${ste.getMessage()}" with the query info wrapped in the command: ${command}""".toString()
                    LOGGER.error(msg, ste)
                } finally {
                    return result
                }
            } else {
                LOGGER.error("""Couldn't fetch data from the resource ${url.dump()} because of the error \
caused by ${conn.getErrorStream().inspect()}""")
                return null
            }
        } catch (SocketTimeoutException ste) {
            String msg = """Error while trying to connect to EBI Search Server to retrieve BioModels Parameters \
due to "${ste.getMessage()}" with the query info wrapped in the command: ${command}""".toString()
            LOGGER.error(msg, ste)
        } catch (IllegalArgumentException iae) {
            LOGGER.error("The proxy setting cannot be null or ${iae.getMessage()}")
        } finally {
            conn.getInputStream().close()
            return result
        }
    }

    private static void doCacheSearchResultsOnRedis(String searchResults, String modelId) {
        if (searchResults && modelId) {
            Map map = [:]
            map.put(modelId, searchResults)
            redisService.doRedisHSet("BP", map)
            LOGGER.debug("Caching the parameters for ${modelId} on Redis cache.")
            println("Caching the parameters for ${modelId} on Redis cache.")
        }
    }
}
