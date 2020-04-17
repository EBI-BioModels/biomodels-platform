/**
* Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
**/





package net.biomodels.jummp.deployment.biomodels

import grails.plugins.rest.client.RestBuilder
import grails.transaction.Transactional
import groovy.time.TimeCategory
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.model.Model
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware
import org.perf4j.aop.Profiled
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.weceem.content.WcmContent
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

import java.text.SimpleDateFormat

/**
 * @short Service responsible for retrieving necessary data to design front page and
 * other static pages.
 *
 * <p>This class  is used for accessing database and resulting required data aiming to
 * populate in home page. The example of this use is to get the recently published and accessed models.
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
@Transactional(readOnly = true)
class DecorationService implements GrailsConfigurationAware {
    private static final Logger logger = LoggerFactory.getLogger(DecorationService.class)
    def grailsApplication
    static String REDIS_SRV_HOST //= grailsApplication.config.jummp.redis.host
    static int REDIS_SRV_PORT //= grailsApplication.config.jummp.redis.host.port
    static int REDIS_SRV_TIMEOUT //= grailsApplication.config.jummp.redis.timeout

    final String EBI_BM_URL = "https://www.ebi.ac.uk/ebisearch/ws/rest/biomodels"
    final String SVC_URL_PREFIX = "${EBI_BM_URL}?query=domain_source:biomodels&size=0&facetfields"

    @Override
    void setConfiguration(ConfigObject co) {
        REDIS_SRV_HOST = co.jummp.redis.host
        REDIS_SRV_PORT = co.jummp.redis.port as int
        REDIS_SRV_TIMEOUT = co.jummp.redis.timeout as int
    }

    /**
     * gets 7 of the most accessed models from the last six months
     *
     * @return A {@link Map} of {@link ModelTransportCommand} associating with their hits
     */
    @Profiled(tag = 'decorationService.buildListOfRecentlyAccessedModels')
    private Map<String, String> buildListOfRecentlyAccessedModels() {
        String query ='''
SELECT
    coalesce(m.publicationId, m.submissionId) as modelId,
    rev.name
FROM
    ModelAudit AS ma
    JOIN ma.model AS m
    JOIN m.revisions AS rev
WHERE
  ma.dateCreated BETWEEN :then AND :now AND
  ma.success = 1 AND
  rev.id IN(
     SELECT aoi.objectId
        FROM
            AclEntry AS ace
            JOIN ace.aclObjectIdentity AS aoi
            JOIN aoi.aclClass AS aclClass
            JOIN ace.sid AS sid
        WHERE
            aclClass.className = 'net.biomodels.jummp.model.Revision'
            AND sid.sid = 'ROLE_ANONYMOUS'
            AND ace.mask = 1)
GROUP BY rev.model
'''
        def now = new Date()
        def then = null
        use(TimeCategory) {
            then = now - 6.months
        }
        def matchedModels = Model.executeQuery(query, [then: then, now: now, max: 10]) as List<List>
        Map<String, String> returnedModels = new LinkedHashMap<>()
        matchedModels.each { row ->
            String id = row[0]
            String name = row[1]
            returnedModels.put(id, name)
        }
        logger.debug("Extracting the list of recently ACCESSED models from the database")
        returnedModels
    }

    /**
     * gets 7 of the most recently published models
     *
     * @return A {@link Map} of {@link ModelTransportCommand} associating with latest published date
     */
    @Profiled(tag = 'decorationService.buildListOfRecentlyPublishedModels')
    private Map<String, String> buildListOfRecentlyPublishedModels() {
        String query = '''
SELECT
    coalesce(model.publicationId, model.submissionId) as modelId,
    max(model.firstPublished),
    rev.name
FROM Model AS model
JOIN model.revisions AS rev
WHERE
  rev.id IN(
     SELECT aoi.objectId
        FROM
			AclEntry AS ace
			JOIN ace.aclObjectIdentity AS aoi
            JOIN aoi.aclClass AS aclClass
            JOIN ace.sid AS sid
        WHERE
            aclClass.className = 'net.biomodels.jummp.model.Revision'
            AND sid.sid = 'ROLE_ANONYMOUS'
            AND ace.mask = 1)
GROUP BY rev.model
ORDER BY model.firstPublished DESC'''
        def matchedModels = Model.executeQuery(query, [max: 10])
        Map<String, String> returnedModels = new HashMap<String, String>()
        matchedModels.each {
            String modelId = it[0]
            String modelName = it[2]
            returnedModels.put(modelId, modelName)
        }
        logger.debug("Extracting the list of recently PUBLISHED models from the database")
        returnedModels
    }

    void refreshRecentlyAccessedModelsRedisCache() {
        Map<String, String> mapModels = buildListOfRecentlyAccessedModels()
        logger.debug("Populating the list of recently ACCESSED models to Redis Server at ${new Date().toString()}")
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                                REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        Jedis jedis = null
        try {
            jedis = pool.getResource()
            jedis.hset("hp-recently-accessed-models", mapModels)
        } finally {
            if (jedis) { jedis.close() }
        }
        pool.close()
    }

    void refreshRecentlyPublishedModelsRedisCache() {
        Map<String, String> mapModels = buildListOfRecentlyPublishedModels()
        logger.debug("Populating the list of recently PUBLISHED models to Redis Server at ${new Date().toString()}")
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                                REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        Jedis jedis = null
        try {
            jedis = pool.getResource()
            jedis.hset("hp-recently-published-models", mapModels)
        } finally {
            if (jedis) { jedis.close() }
        }
        pool.close()
    }

    void refreshModelOfTheMonthEntryRedisCache() {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                                REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        pool.getResource().withCloseable { Jedis jedis ->
            Map momEntry = buildModelOfTheMonthEntry()
            final String MOM_ENTRY_KEY = "the-latest-mom-entry"
            for (Map.Entry<String, String> entry : momEntry) {
                jedis.hset(MOM_ENTRY_KEY, entry.key, entry.value)
            }
        }
        pool.close()
    }

    /**
     * Fetches the statistical data on the chart of the curation states shown on Home Page
     *
     * <p>This service is being used in {@link BioModelsTagLib} for rendering the Curation State chart
     * in the slideshow/carousel.
     *
     * @return A {@link Map} holding curation states and their corresponding counts
     */
    Map<String, Integer> fetchStatisticsCurationState() {
        Map<String, Integer> curationStateMap = fetchStatisticsDataFromRedisCache("hp-statistics-curation-state")
        return curationStateMap
    }

    /**
     * Fetches the statistical data for the chart of the modelling approaches shown on Home Page.
     *
     * <p>This service is being used in {@link BioModelsTagLib} for rendering the Modelling Approaches
     * chart in the slideshow/carousel.
     *
     * @return A {@link Map} holding modelling approaches and their corresponding counts
     */
    Map<String, Integer>  fetchStatisticsModellingApproaches() {
        Map<String, Integer> approachesMap = fetchStatisticsDataFromRedisCache("hp-statistics-modelling-approaches")
        return approachesMap
    }

    Map fetchStatisticsOrganisms() {
        Map organismsMap = fetchStatisticsOrganismsFromRedisCache("hp-statistics-organisms")
        Map returnedMap = [:]
        if (!organismsMap) {
            // call the fallback
            returnedMap = buildStatisticsOrganisms()
        } else {
            // rebuild the map which @see buildStatisticsOrganisms() returns
            returnedMap["children"] = organismsMap.collect { entry ->
                String[] parts = entry.value.split(";")
                String count = parts[0] as String
                String taxonomy = parts[1] as String
                [Name: entry.key, Count: count, Taxonomy: taxonomy]
            }
        }
        return returnedMap
    }

    Map<String, Integer> fetchStatisticsJournals() {
        Map<String, Integer> journalsMap = fetchStatisticsDataFromRedisCache("hp-statistics-journals")
        return journalsMap
    }

    Map<String, String> fetchRecentlyAccessedModels() {
        Map models = doRedisHGetAll("hp-recently-accessed-models")
        if (!models) {
            // call the fallback
            models = buildListOfRecentlyAccessedModels()
        }
        return models
    }

    Map<String, String> fetchRecentlyPublishedModels() {
        Map models = doRedisHGetAll("hp-recently-published-models")
        if (!models) {
            // call the fallback
            models = buildListOfRecentlyPublishedModels()
        }
        return models
    }

    Map<String, String> fetchMomEntry() {
        Map momEntryMap = doRedisHGetAll("the-latest-mom-entry")
        if (!momEntryMap) {
            // call the fallback
            momEntryMap = buildModelOfTheMonthEntry()
        }
        return momEntryMap
    }

    Map<String, String> fetchDataNewsWidget() {
        Map news = doRedisHGetAll("hp-news-widget")
        if (!news) {
            // call the fallback
            news = buildDataForNewsWidget()
        }
        return news
    }

    Map<String, Integer> buildStatisticsCurationState() {
        String serviceURL = "${SVC_URL_PREFIX}=curationstatus&facetcount=10&format=json"
        RestBuilder restBuilder = new RestBuilder(connectTimeout: 10000, readTimeout: 100000, proxy: null)
        def response = restBuilder.get(serviceURL) {
            accept("application/json")
            contentType("application/json;charset=UTF-8")
        }
        def totalHitCount = response.json.hitCount as Integer
        // the total hit count is always greater than the sum of two below values
        // because it includes private models.
        Map<String, Integer> curationStateMap = ["totalHitCount": totalHitCount]
        def facetValues = response.json.facets[0].facetValues
        def facetCurated = facetValues[0]
        def facetNoncurated = facetValues[1]
        curationStateMap.put(facetCurated["label"] as String, facetCurated["count"] as Integer)
        curationStateMap.put(facetNoncurated["label"] as String, facetNoncurated["count"] as Integer)
        return curationStateMap
    }

    Map<String, Integer> buildStatisticsModellingApproaches() {
        String serviceURL = "${SVC_URL_PREFIX}=modellingapproach&facetcount=10&format=json"
        RestBuilder restBuilder = new RestBuilder(connectTimeout: 10000, readTimeout: 100000, proxy: null)
        def response = restBuilder.get(serviceURL) {
            accept("application/json")
            contentType("application/json;charset=UTF-8")
        }
        def totalHitCount = response.json.hitCount
        // the total hit count is always greater than the sum of these below values
        // because it includes private models.
        def totalFacets = response.json.facets[0].total
        def facetValues = response.json.facets[0].facetValues
        Map<String, Integer> approachesMap = [:] //["totalHitCount": totalHitCount]
        for (int i = 0; i < totalFacets; i++) {
            String key = facetValues[i]["label"] as String
            Integer value = facetValues[i]["count"] as Integer
            approachesMap.put(key, value)
        }
        return approachesMap
    }

    Map buildStatisticsJournals() {
        String query ='''
SELECT
    p.id, p.journal, count(m.submissionId)
FROM
    Model AS m
    JOIN m.publication AS p
WHERE
    m.deleted = 0
GROUP BY p.journal
'''
        def matchedModels = Model.executeQuery(query)
        Map<String, Integer> publications = new HashMap<>()
        matchedModels.each {
            publications.put(it[1] as String, it[2] as Integer)
        }
        publications
    }

    Map buildDataForNewsWidget() {
        def newsQuery = "from WcmContent where parent.aliasURI = :aliasuri"
        def newsEntries = WcmContent.executeQuery(newsQuery, [aliasuri: 'news'])
        Map<String, String> data = [:]
        for (def entry : newsEntries) {
            data.put(entry.aliasURI, entry.title)
        }
        data
    }

    void refreshDataForNewsWidgetRedisCache() {
        Map data = buildDataForNewsWidget()
        doRedisHSet("hp-news-widget", data)
    }

    void refreshDataForChartsRedisCache() {
        doRedisHSet("curation-state-based-statistic", ["curated": '900', "non-curated": '2500'])
    }

    void refreshStatisticsCurationStateRedisCache() {
        Map<String, Integer> curationState = buildStatisticsCurationState()
        Map<String, String> curationSateMap = convert2RedisMap(curationState)
        doRedisHSet("hp-statistics-curation-state", curationSateMap)
    }

    void refreshStatisticsModellingApproachesRedisCache() {
        Map modellingApproachesMap = buildStatisticsModellingApproaches()
        Map approachesMap = convert2RedisMap(modellingApproachesMap)
        doRedisHSet("hp-statistics-modelling-approaches", approachesMap)
    }

    void refreshStatisticsOrganismsRedisCache() {
        Map organismsMap = buildStatisticsOrganisms()
        def organisms = organismsMap["children"]
        Map<String, String> taxons = new HashMap<>()
        organisms.each {
            taxons.put(it["Name"] as String, "${it['Count']};${it['Taxonomy']}" as String)
        }
        doRedisHSet("hp-statistics-organisms", taxons)
    }

    void refreshStatisticsJournalsRedisCache() {
        Map pubsMap = buildStatisticsJournals()
        Map pubsRedisMap = convert2RedisMap(pubsMap)
        doRedisHSet("hp-statistics-journals", pubsRedisMap)
    }

    void updateDataForWidgetsOnHomePage() {
        refreshStatisticsCurationStateRedisCache()
        refreshStatisticsModellingApproachesRedisCache()
        refreshStatisticsOrganismsRedisCache()
        refreshStatisticsJournalsRedisCache()
        refreshRecentlyAccessedModelsRedisCache()
        refreshRecentlyPublishedModelsRedisCache()
        refreshModelOfTheMonthEntryRedisCache()
        refreshDataForNewsWidgetRedisCache()
    }

    void doRedisHSet(final String key, Map data) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                                REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        pool.getResource().withCloseable { Jedis jedis ->
            jedis.hset(key, data)
            jedis.close()
        }
        pool.close()
    }

    String doRedisHGet(final String key) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        String cachedData
        pool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.hget(key)
            jedis.close()
        }
        pool.close()
        cachedData
    }

    Map doRedisHGetAll(final String key) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        Jedis jedis = null
        Map returnedMap = new HashMap()
        try {
            jedis = pool.getResource()
            returnedMap = jedis.hgetAll(key)
        } finally {
            if (jedis) { jedis.close() }
        }
        pool.close()
        returnedMap
    }

    private Map buildModelOfTheMonthEntry() {
        final String query = "from ModelOfTheMonth order by publicationDate desc"
        ModelOfTheMonth theLatestMoM = ModelOfTheMonth.find(query)
        String shortDescription = theLatestMoM.shortDescription
        String previewImage = Base64.encoder.encodeToString(theLatestMoM.previewImage)
        Date theLatestPublicationDate = theLatestMoM.publicationDate
        def monthNumStr = new SimpleDateFormat("MM").format(theLatestPublicationDate)
        def monthString = new SimpleDateFormat("MMMMM").format(theLatestPublicationDate)
        def yearString = new SimpleDateFormat("YYYY").format(theLatestPublicationDate)
        final String prefixLink = "${grailsApplication.config.grails.serverURL}/content/model-of-the-month"
        def link = "${prefixLink}?year=${yearString}&month=${monthNumStr}"
        def linkAll = "${prefixLink}?all=yes"
        String titlePreviewImage = "Model of the month: ${monthString} ${yearString}"
        Map momEntry = [:]
        momEntry.put("shortDescription", shortDescription)
        momEntry.put("previewImage", previewImage)
        momEntry.put("monthNumStr", monthNumStr)
        momEntry.put("monthString", monthString)
        momEntry.put("yearString", yearString)
        momEntry.put("momEntryLink", link)
        momEntry.put("momEntryLinkAll", linkAll)
        momEntry.put("titlePreviewImage", titlePreviewImage)
        return momEntry
    }

    /**
     * Builds statistical data for Organisms chart
     *
     * @return a {@link Map} containing an element mapping "children" as the key and the value is a
     * {@link List} of customised map which the properties are Name, Count, and Taxonomy.
     */
    private Map buildStatisticsOrganisms() {
        List result = makeStatisticsOnOrganisms()
        Map returned = new HashMap()
        returned["children"] = result.collect { OrganismData d ->
            [Name: d.name, Count: d.count, Taxonomy: d.taxonomy]
        }
        returned
    }

    /**
     * Makes a statistic about models with names of the publication journal
     *
     * @return a {@link List} of Organism objects
     */
    private List makeStatisticsOnOrganisms() {
        String queryLink = "search?domain=biomodels&query=*:* AND NOT isprivate:true&format=json"
        String serverURL = grailsApplication.config.grails.serverURL
        String queryURL = "${serverURL}/${queryLink}"
        RestBuilder rest = new RestBuilder(connectTimeout: 10000, readTimeout: 100000, proxy: null)
        def response = rest.get(queryURL) {
            accept("application/json")
            contentType("application/json;charset=UTF-8")
        }

        def taxons = response.json.facets.findAll { it['id'] == 'TAXONOMY' }
        taxons = taxons.facetValues.flatten()
        StringBuilder result = new StringBuilder()
        List<OrganismData> organismData = new ArrayList<OrganismData>()
        for (tax in taxons) {
            OrganismData d = new OrganismData(name: "${tax['label']} (${tax['value']})",
                count: tax['count'] as int, taxonomy: tax['value'])
            organismData.add(d)
        }
        return organismData
    }

    private Map<String, Integer> fetchStatisticsDataFromRedisCache(final String key) {
        Map<String, String> redisMap = doRedisHGetAll(key)
        Map<String, Integer> returnedMap = new HashMap<>()
        if (!redisMap) {
            // call the fallback
            switch (key) {
                case "hp-statistics-curation-state":
                    returnedMap = buildStatisticsCurationState()
                    break
                case "hp-statistics-modelling-approaches":
                    returnedMap = buildStatisticsModellingApproaches()
                    break
                case "hp-statistics-journals":
                    returnedMap = buildStatisticsJournals()
                    break
            }
        } else {
            returnedMap = convertFromRedisMap(redisMap)
        }
        return returnedMap
    }

    private Map fetchStatisticsOrganismsFromRedisCache(final String key = "hp-statistics-organisms") {
        doRedisHGetAll(key)
    }

    private static Map<String, String> convert2RedisMap(final Map<String, Integer> inputMap) {
        Map<String, String> returnedMap = new HashMap<>()
        for (entry in inputMap) {
            returnedMap.put(entry.key, Integer.toString(entry.value))
        }
        returnedMap
    }

    private static Map<String, Integer> convertFromRedisMap(final Map<String, String> inputMap) {
        Map<String, Integer> returnedMap = new HashMap<>()
        for (entry in inputMap) {
            returnedMap.put(entry.key, Integer.parseInt(entry.value))
        }
        returnedMap
    }
}

class ModelLatestPublished {
    String modelName
    Date latestAccessedDate

    ModelLatestPublished(String modelName, Date latestAccessedDate) {
        this.modelName = modelName
        this.latestAccessedDate = latestAccessedDate
    }
}

class OrganismData {
    String name
    String taxonomy
    int count

    String toString() {
        "$name ($taxonomy): $count"
    }
}
