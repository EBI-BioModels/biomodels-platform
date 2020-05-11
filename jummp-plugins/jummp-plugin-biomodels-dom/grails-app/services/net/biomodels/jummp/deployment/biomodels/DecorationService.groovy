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
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.security.User
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
    static String EBI_SEARCH_URL = "https://www.ebi.ac.uk/ebisearch/ws/rest"
    static String FIXED_PARAMS = "biomodels?query=domain_source:biomodels&size=0&facetfields"
    static String EBI_SEARCH_BM_URL = "${EBI_SEARCH_URL}/${FIXED_PARAMS}"
    static String HP_STAT_TOTAL_FIGURE = "hp-statistics-total-figures"
    static String BM_SVR_URL //= grailsApplication.config.grails.serverURL
    static String CLASSIFIER_SVR_URL //= grailsApplication.config.jummp.classification.endpoint

    private String httpProxyHost
    private int httpProxyPort
    private Proxy proxy

    @Override
    void setConfiguration(ConfigObject co) {
        REDIS_SRV_HOST = co.jummp.redis.host
        REDIS_SRV_PORT = co.jummp.redis.port as int
        REDIS_SRV_TIMEOUT = co.jummp.redis.timeout as int
        BM_SVR_URL = co.grails.serverURL
        CLASSIFIER_SVR_URL = co.jummp.classification.endpoint
        EBI_SEARCH_URL = "https://www.ebi.ac.uk/ebisearch/ws/rest"
        FIXED_PARAMS = "biomodels?query=domain_source:biomodels&size=0&facetfields"
        EBI_SEARCH_BM_URL = "${EBI_SEARCH_URL}/${FIXED_PARAMS}"
        HP_STAT_TOTAL_FIGURE = "hp-statistics-total-figures"
        httpProxyHost = co.jummp.http.host
        httpProxyPort = co.jummp.http.port as int
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.httpProxyHost, this.httpProxyPort))
    }

    /**
     * gets 10 of the most accessed models from the last six months
     *
     * @return A {@link Map} constructed by model identifiers associating with their names
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
     * gets 10 of the most recently published models
     * Due to needing more fields to be shown on the widget, this service will have to include
     * some fields like revision owner as submitter, publication title, publication journal and year.
     * @return A {@link Map} of {@link RecentlyPublishedModel} objects
     */
    @Profiled(tag = 'decorationService.buildListOfRecentlyPublishedModels')
    private Map<String, RecentlyPublishedModel> buildListOfRecentlyPublishedModels() {
        String query = '''
SELECT
    coalesce(model.publicationId, model.submissionId) as modelId,
    max(model.firstPublished),
    rev.name, rev.owner, model.publication.title, model.publication.journal, model.publication.year
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
  AND model.firstPublished IS NOT NULL
GROUP BY rev.model
ORDER BY model.firstPublished DESC'''
        def matchedModels = Model.executeQuery(query, [max: 10])
        Map<String, RecentlyPublishedModel> returnedModels = new HashMap<String, RecentlyPublishedModel>()
        matchedModels.each {
            User owner = it[3] as User
            RecentlyPublishedModel rpm = new RecentlyPublishedModel(id: it[0],
                title: it[2],
                lastPublished: (it[1] as Date).format("yyyy-MM-dd"),
                submitter: owner.person.userRealName,
                pubTitle: it[4],
                pubJournal: it[5],
                pubYear: it[6])
            returnedModels.put(it[0], rpm)
        }
        if (returnedModels) {
            logger.debug("Extracting the list of recently PUBLISHED models from the database")
        }
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
            jedis.hmset("hp-recently-accessed-models", mapModels)
        } finally {
            if (jedis) { jedis.close() }
        }
        pool.close()
    }

    void refreshRecentlyPublishedModelsRedisCache() {
        Map<String, RecentlyPublishedModel> mapModels = buildListOfRecentlyPublishedModels()
        logger.debug("Populating the list of recently PUBLISHED models to Redis Server at ${new Date().toString()}")
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                                REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        Jedis jedis = null
        try {
            jedis = pool.getResource()
            String key = "hp-recently-published-models"
            deleteAllByPattern(jedis, key)
            Map models = [:]
            for (Map.Entry<String, RecentlyPublishedModel> entry : mapModels) {
                RecentlyPublishedModel m = entry.value
                Map value = ["id": m.id, "title": m.title, "submitter": m.submitter,
                             "lastPublished": m.lastPublished, "pubTitle": m.pubTitle,
                             "pubJournal": m.pubJournal, "pubYear": m.pubYear]
                jedis.hmset("$key-${m.id}" as String, value)
                models.put(m.id, m.title)
            }
            jedis.hmset(key, models)
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

    Map<String, RecentlyPublishedModel> fetchRecentlyPublishedModels() {
        final String key = "hp-recently-published-models"
        Map models = doRedisHGetAll(key)
        Map returnedMap = [:]
        if (!models) {
            // call the fallback
            returnedMap = buildListOfRecentlyPublishedModels()
        } else {
            for (String modelId in models.keySet()) {
                Map rpm = doRedisHGetAll("$key-$modelId" as String)
                RecentlyPublishedModel model =
                    new RecentlyPublishedModel(id: rpm.get("id"), submitter: rpm.get("submitter"),
                        lastPublished: rpm.get("lastPublished"),
                        title: rpm.get("title"), pubJournal: rpm.get("pubJournal"),
                        pubTitle: rpm.get("pubTitle"), pubYear: rpm.get("pubYear"))
                returnedMap.put(model.id, model)
            }
        }
        return returnedMap
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
        String query = "${FIXED_PARAMS}=curationstatus&facetcount=10&format=json"
        def response = hitRemoteService(EBI_SEARCH_URL, query)
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
        String query = "${FIXED_PARAMS}=modellingapproach&facetcount=10&format=json"
        def response = hitRemoteService(EBI_SEARCH_URL, query)
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

    void refreshStatisticsDataForFeatures() {
        long totalSubmissions = retrieveTotalSubmissionsFromEBISearchServer()
        long totalIndividualModels = retrieveTotalIndividualModelsFromEBISearchServer()
        long totalAutoGenModels = retrieveTotalAutoGeneratedModelsFromEBISearchServer()
        long totalGoClasses = retrieveTotalGOClassesFromBioModels()
        long totalParametersEntries = retrieveTotalParametersEntriesFromEBISearchServer()
        Map figuresRedisMap = [:]
        figuresRedisMap.put("total-submissions", totalSubmissions.toString())
        figuresRedisMap.put("total-individual-models", totalIndividualModels.toString())
        figuresRedisMap.put("total-auto-generated-models", totalAutoGenModels.toString())
        figuresRedisMap.put("total-go-classes", totalGoClasses.toString())
        figuresRedisMap.put("total-parameters-entries", totalParametersEntries.toString())
        doRedisHSet("hp-statistics-total-figures", figuresRedisMap)
    }

    /**
     * Updates cached data on Redis Server in batch mode. This update will refresh data for
     * <ul>
     *   <li>the figures shown in the features</li>
     *   <li>the charts on slideshow</li>
     *   <li>the recently accessed and published models</li>
     *   <li>the Model of the Month widget</li>
     *   <li>the news widget</li>
     * </ul>
     */
    void updateDataForWidgetsOnHomePage() {
        refreshStatisticsDataForFeatures()
        updateDataForChartsOnHomePage()
        refreshRecentlyAccessedModelsRedisCache()
        refreshRecentlyPublishedModelsRedisCache()
        refreshModelOfTheMonthEntryRedisCache()
        refreshDataForNewsWidgetRedisCache()
    }

    void updateDataForChartsOnHomePage() {
        refreshStatisticsCurationStateRedisCache()
        refreshStatisticsModellingApproachesRedisCache()
        refreshStatisticsOrganismsRedisCache()
        refreshStatisticsJournalsRedisCache()
    }

    void doRedisHSet(final String key, Map data) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                                REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        pool.getResource().withCloseable { Jedis jedis ->
            deleteAllByPattern(jedis, key)
            jedis.hmset(key, data)
        }
        pool.close()
    }

    String doRedisHGet(final String key, final String field) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        String cachedData
        pool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.hget(key, field)
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

    private deleteAllByPattern(final Jedis jedis, final String pattern) {
        Set<String> keys = jedis.keys(pattern)
        for (String key : keys) {
            jedis.del(key)
        }
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
        RestBuilder rest = new RestBuilder(connectTimeout: 10000, readTimeout: 100000, proxy: proxy)
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

    Long fetchStatisticsTotalSubmissions() {
        // fetch the figure from Redis cache
        Long total = doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-submissions") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalSubmissionsFromEBISearchServer()
        }
        total
    }

    Long fetchStatisticsTotalIndividualModels () {
        // fetch the figure from Redis cache
        Long total = doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-individual-models") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalIndividualModelsFromEBISearchServer()
        }
        total
    }

    Long fetchStatisticsTotalAutoGeneratedModels () {
        // fetch the figure from Redis cache
        Long total = doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-auto-generated-models") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalAutoGeneratedModelsFromEBISearchServer()
        }
        total
    }

    Long fetchStatisticsTotalGOClasses() {
        // fetch the figure from Redis cache
        Long total = doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-go-classes") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalGOClassesFromBioModels()
        }
        total
    }

    Long fetchStatisticsTotalParametersEntries() {
        // fetch the figure from Redis cache
        Long total = doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-parameters-entries") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalParametersEntriesFromEBISearchServer()
        }
        total
    }

    /**
     * Hits EBI Search Server via BioModels web service to get this figure
     *
     * @return a long number as the total models
     */
    private long retrieveTotalSubmissionsFromEBISearchServer() {
        String query = "search?domain=biomodels_all&query=*:*&format=json"
        def response = hitRemoteService(BM_SVR_URL, query)
        return response.json.matches as Long
    }

    /**
     * Hits EBI Search Server via BioModels web service to get this figure
     *
     * @return a long number as the total models
     */
    private long retrieveTotalIndividualModelsFromEBISearchServer() {
        String query = "search?domain=biomodels&query=*:*&format=json"
        def response = hitRemoteService(BM_SVR_URL, query)
        return response.json.matches as Long
    }

    /**
     * Hits EBI Search Server via BioModels web service to get this figure
     *
     * @return a long number as the total models
     */
    private long retrieveTotalAutoGeneratedModelsFromEBISearchServer() {
        String query = "search?domain=biomodels_autogen&query=*:*&format=json"
        def response = hitRemoteService(BM_SVR_URL, query)
        return response.json.matches as Long
    }

    /**
     * Hits Model Classifier Service via BioModels web service to get this figure
     *
     * @return a long number as the total classes
     */
    private long retrieveTotalGOClassesFromBioModels() {
        // TODO: will be implemented soon once this call is ready in Model Classifier Service
        /*String query = "query?nb_classes&format=json"
        def response = hitRemoteService(CLASSIFIER_SVR_URL, query)
        return response.total as Long
        */
        // due to the unavailability of this service in Model Classifier Service, we queried this value
        // directly from BioModels database
        1132L
    }

    /**
     * Hits EBI Search Service directly to get this figure
     *
     * @return a long number as the total entries
     */
    private long retrieveTotalParametersEntriesFromEBISearchServer() {
        String query = "biomodels_parameters?query=is_curated:false&size=1&fields=id&format=json"
        def response = hitRemoteService(EBI_SEARCH_URL, query)
        return response.json.hitCount as long
    }

    /**
     * Hits remote service via a given query URL
     *
     * @param serverURL a String as the server URL
     * @param query a String as the query
     * @return a JSON object
     */
    private def hitRemoteService(final String serverURL, final String query) {
        String queryURL = "${serverURL}/${query}"
        logger.debug("Connecting to the service at $queryURL")
        RestBuilder rest = new RestBuilder(connectTimeout: 10000, readTimeout: 100000, proxy: proxy)
        def response = rest.get(queryURL) {
            accept("application/json")
            contentType("application/json;charset=UTF-8")
        }
        response
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

class RecentlyPublishedModel {
    String id
    String title
    String submitter
    String lastPublished
    String pubTitle
    String pubJournal
    String pubYear

    String toString() {
        "$id: $title (${submitter}, ${lastPublished})\n${pubTitle}: ${pubJournal} (${pubYear})"
    }
}
