/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
import grails.util.Holders
import groovy.time.TimeCategory
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.scms.CmsContent
import net.biomodels.jummp.statistic.OrganismData
import net.biomodels.jummp.statistic.RecentlyPublishedModel
import net.biomodels.jummp.utils.redis.RedisService
import org.perf4j.aop.Profiled
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import redis.clients.jedis.Jedis

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
class DecorationService implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DecorationService.class)

    def grailsApplication
    def configurationService
    RedisService redisService = Holders.grailsApplication.mainContext.getBean("redisService") as RedisService

    /*static String REDIS_SRV_HOST //= grailsApplication.config.jummp.redis.host
    static int REDIS_SRV_PORT //= grailsApplication.config.jummp.redis.host.port
    static int REDIS_SRV_TIMEOUT //= grailsApplication.config.jummp.redis.timeout
    */

    static String HP_STAT_TOTAL_FIGURE = "hp-statistics-total-figures"
    static int ACCESSED_MAX_RECORDS
    static int PUBLISHED_MAX_RECORDS
    static String SVR_URL

    @Override
    void afterPropertiesSet() throws Exception {
        HP_STAT_TOTAL_FIGURE = "hp-statistics-total-figures"
        ACCESSED_MAX_RECORDS = grailsApplication.config.biomodels.homepage.recently.accessed.models.maxRecords as int
        PUBLISHED_MAX_RECORDS = grailsApplication.config.biomodels.homepage.recently.published.models.maxRecords as int
        SVR_URL = grailsApplication.config.grails.serverURL
        LOGGER.info("Finished the bean initialisation")
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
        def matchedModels = Model.executeQuery(query,
            [then: then, now: now, max: ACCESSED_MAX_RECORDS]) as List<List>
        Map<String, String> returnedModels = new LinkedHashMap<>()
        matchedModels.each { row ->
            String id = row[0]
            String name = row[1]
            returnedModels.put(id, name)
        }
        LOGGER.debug("Extracting the list of recently ACCESSED models from the database")
        returnedModels
    }

    /**
     * <p><strong>gets 10 of the most recently published models</strong></p>
     * <br/>
     * <p>Due to needing more fields to be shown on the widget, this service will have to include
     * some fields like revision owner as submitter, publication title, publication journal and year.</p>
     * @return A {@link Map} of {@link net.biomodels.jummp.statistic.RecentlyPublishedModel} objects
     */
    @Profiled(tag = 'decorationService.buildListOfRecentlyPublishedModels')
    private Map<String, RecentlyPublishedModel> buildListOfRecentlyPublishedModels() {
        String query = '''
SELECT
    coalesce(model.publicationId, model.submissionId) as modelId,
    model.firstPublished,
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
  AND rev.revisionNumber = (SELECT MAX(revisionNumber) FROM Revision r2
                            WHERE r2.model.id=rev.model.id AND r2.state='PUBLISHED')
ORDER BY model.firstPublished DESC'''
        def matchedModels = Model.executeQuery(query, [max: PUBLISHED_MAX_RECORDS])
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
            LOGGER.debug("Extracting the list of recently PUBLISHED models from the database")
        } else {
            LOGGER.error("Could not extract the list of recently published models")
        }
        returnedModels
    }

    Map<String, String> refreshRecentlyAccessedModelsRedisCache() {
        Map<String, String> mapModels = buildListOfRecentlyAccessedModels()
        addListOfRecentlyAccessedModelsToRedis(mapModels)
        mapModels
    }

    private void addListOfRecentlyAccessedModelsToRedis(Map mapModels) {
        LOGGER.debug("Caching the list of recently ACCESSED models to Redis Server")
        redisService.doRedisHSet("hp-recently-accessed-models", mapModels)
    }

    Map<String, String> refreshRecentlyPublishedModelsRedisCache() {
        Map<String, RecentlyPublishedModel> mapModels = buildListOfRecentlyPublishedModels()
        Map<String, String> models = addRecentlyPublishedModelsToRedis(mapModels)
        models
    }

    private Map<String, String> addRecentlyPublishedModelsToRedis(Map<String, RecentlyPublishedModel> mapModels) {
        LOGGER.debug("Caching the list of recently PUBLISHED models to Redis Server")
        Jedis jedis = null
        Map<String, String> models = [:]
        try {
            String key = "hp-recently-published-models"
            jedis = redisService.jedisPool.getResource()
            clearRedisCacheOfRecentlyPublishedModels(jedis, key)

            for (Map.Entry<String, RecentlyPublishedModel> entry : mapModels) {
                RecentlyPublishedModel m = entry.value
                Map value = ["id": m.id, "title": m.title, "submitter": m.submitter,
                             "lastPublished": m.lastPublished, "pubTitle": m.pubTitle,
                             "pubJournal": m.pubJournal, "pubYear": m.pubYear ?: "Unpublished"]
                redisService.doRedisHSet("$key-${m.id}" as String, value)
                models.put(m.id, """${m.title}<br/>Submitted by: ${m.submitter}; \
Last published date: ${m.lastPublished};<br/>\
Publication: ${m.pubTitle};<br/>Published in ${m.pubYear} at ${m.pubJournal}.""" as String)
            }
            redisService.doRedisHSet(key, models)
        } catch (Exception ignored) {
            throw ignored
        } finally {
            if (jedis) { jedis.close() }
        }
        models
    }

    void refreshModelOfTheMonthEntryRedisCache() {
        Map momEntry = buildModelOfTheMonthEntry()
        if (momEntry) {
            LOGGER.debug("Adding or updating this entry on Redis Cache!")
            addModelOfTheMonthEntryToRedis(momEntry)
        } else {
            LOGGER.debug("Cannot load and update Redis Cache for the current entry of Model of the Month!")
        }
    }

    private void addModelOfTheMonthEntryToRedis(final Map momEntry) {
        LOGGER.debug("Caching the Model of the Month entry to Redis")
        final String MOM_ENTRY_KEY = "the-latest-mom-entry"
        redisService.doRedisHSet(MOM_ENTRY_KEY, momEntry)
    }

    String fetchAnnouncements() {
        String content = ""
        def queryStr = """\
from CmsContent where parent.aliasURI = :aliasURI and publishedTo >= :now \
and publishedFrom is not null and publishedTo is not null order by createdOn desc"""
        def announcements = CmsContent.executeQuery(queryStr, [aliasURI: 'announcements', now: new Date()], [max: 10])
        for (def entry : announcements) {
            content += entry.content
        }
        content
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
            LOGGER.debug("Falling back to build the Statistics for Organisms")
            returnedMap = buildStatisticsOrganisms()
            addStatisticsOrganismsToRedis(returnedMap)
        } else {
            // rebuild the map which @see buildStatisticsOrganisms() returns
            returnedMap["children"] = organismsMap.collect { entry ->
                String[] parts = entry.value.split(";")
                int count = parts[0] as int
                String taxonomy = parts[1] as String
                int normalisedCount = parts[2] as int
                [Name: entry.key, Count: count, Taxonomy: taxonomy, NormalisedCount: normalisedCount]
            }
        }
        return returnedMap
    }

    Map<String, Integer> fetchStatisticsJournals() {
        Map<String, Integer> journalsMap = fetchStatisticsDataFromRedisCache("hp-statistics-journals")
        return journalsMap
    }

    Map<String, String> fetchRecentlyAccessedModels() {
        Map models = redisService.doRedisHGetAll("hp-recently-accessed-models")
        if (!models) {
            // call the fallback
            LOGGER.debug("Falling back to build the list of Recently Accessed Models")
            models = buildListOfRecentlyAccessedModels()
            addListOfRecentlyAccessedModelsToRedis(models)
        }
        return models
    }

    Map<String, RecentlyPublishedModel> fetchRecentlyPublishedModels() {
        final String key = "hp-recently-published-models"
        Map models = redisService.doRedisHGetAll(key)
        Map<String, RecentlyPublishedModel> returnedMap = [:]
        if (!models) {
            // call the fallback
            LOGGER.debug("Falling back to build the list of Recently Published Models")
            returnedMap = buildListOfRecentlyPublishedModels()
            addRecentlyPublishedModelsToRedis(returnedMap)
        } else {
            for (String modelId in models.keySet()) {
                Map rpm = redisService.doRedisHGetAll("$key-$modelId" as String)
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
        Map<String, String> momEntryMap = redisService.doRedisHGetAll("the-latest-mom-entry")
        if (!momEntryMap) {
            // call the fallback
            LOGGER.debug("Falling back to build the Model of the Month entry")
            momEntryMap = buildModelOfTheMonthEntry()
            addModelOfTheMonthEntryToRedis(momEntryMap)
        }
        return momEntryMap
    }

    Map<String, String> fetchDataNewsWidget() {
        Map<String, String> news = redisService.doRedisHGetAll("hp-news-widget")
        if (!news) {
            // call the fallback
            LOGGER.debug("Falling back to build the News entry")
            news = buildDataForNewsWidget()
            // cache the data to Redis server
            LOGGER.debug("Caching the News entry to Redis server")
            redisService.doRedisHSet("hp-news-widget", news)
        } else {
            Map sortedNews = new LinkedHashMap()
            sortedNews = news.sort { n1, n2 ->
                String strDate1 = n1.value.take(10)
                String strDate2 = n2.value.take(10)
                Date date1 = new Date().parse("dd/MM/yyyy", strDate1)
                Date date2 = new Date().parse("dd/MM/yyyy", strDate2)
                return date2 <=> date1
            } as Map<String, String>
            news = sortedNews as Map<String, String>
        }
        return news
    }

    Map<String, Integer> buildStatisticsModellingApproaches() {
        // As of changing this line, we have less than 200 modelling approaches while 1000 is the allowed value
        // in the EBI Search to make sure we don't trap in NPEs.
        String query = "biomodels?query=*:*&size=0&facetfields=modellingapproach&facetcount=1000&format=json"
        def response = hitRemoteService(BioModels.EBI_SEARCH_RESTFUL_WS_URL, query)
        // the total hit count is always greater than the sum of these below values
        // because it includes private models.
        def totalFacets = response.json.facets[0].total
        def facetValues = response.json.facets[0].facetValues
        Map<String, Integer> approachesMap = [:]
        int total = totalFacets <= facetValues?.size() ? totalFacets : facetValues?.size()
        for (int i = 0; i < total; i++) {
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
    and m.submissionId NOT LIKE 'MODEL170711%'
    and m.submissionId NOT LIKE 'BMID%'
GROUP BY p.id, p.journal
'''
        def matchedModels = Model.executeQuery(query)
        Map<String, Integer> publications = new HashMap<>()
        matchedModels.each {
            publications.put(it[1] as String, it[2] as Integer)
        }
        publications
    }

    Map buildDataForNewsWidget() {
        // only select the published News items and ignore ones under the other statuses
        def newsQuery = """\
from CmsContent where parent.aliasURI = :aliasuri order by createdOn desc"""
        def newsEntries = CmsContent.executeQuery(newsQuery, [aliasuri: 'news'], [max: 10])
        Map<String, String> data = [:]
        for (def entry : newsEntries) {
            data.put(entry.aliasURI,
                "${entry.createdOn.format('dd/MM/yyyy')}: ${entry.title}" as String)
        }
        data
    }

    void refreshDataForNewsWidgetRedisCache() {
        Map data = buildDataForNewsWidget()
        redisService.doRedisHSet("hp-news-widget", data)
    }

    void refreshStatisticsModellingApproachesRedisCache() {
        Map modellingApproachesMap = buildStatisticsModellingApproaches()
        addStatisticsModellingApproachesToRedis(modellingApproachesMap)
    }

    private addStatisticsModellingApproachesToRedis(Map modellingApproachesMap) {
        LOGGER.debug("Caching the statistics modelling approaches to Redis Server")
        Map approachesMap = convert2RedisMap(modellingApproachesMap)
        redisService.doRedisHSet("hp-statistics-modelling-approaches", approachesMap)
    }

    void refreshStatisticsOrganismsRedisCache() {
        Map organismsMap = buildStatisticsOrganisms()
        addStatisticsOrganismsToRedis(organismsMap)
    }

    private void addStatisticsOrganismsToRedis(Map organismsMap) {
        def organisms = organismsMap["children"]
        Map<String, String> taxons = new HashMap<>()
        organisms.each {
            String value = "${it['Count']};${it['Taxonomy']};${it['NormalisedCount']}" as String
            taxons.put(it["Name"] as String, value)
        }
        LOGGER.info("Caching the statistics organism to Redis Server")
        redisService.doRedisHSet("hp-statistics-organisms", taxons)
    }

    void refreshStatisticsJournalsRedisCache() {
        Map pubsMap = buildStatisticsJournals()
        addStatisticsJournalsToRedis(pubsMap)
    }

    private void addStatisticsJournalsToRedis(Map pubsMap) {
        LOGGER.debug("Caching the statistics journals to Redis Server")
        Map pubsRedisMap = convert2RedisMap(pubsMap)
        redisService.doRedisHSet("hp-statistics-journals", pubsRedisMap)
    }

    void refreshStatisticsDataForFeatures() {
        long totalSubmissions = retrieveTotalSubmissionsFromEBISearchServer()
        long totalIndividualModels = retrieveTotalIndividualModelsFromEBISearchServer()
        long totalCuratedModels = retrieveTotalCuratedModelsFromEBISearchServer()
        long totalNonCuratedModels = retrieveTotalNonCuratedModelsFromEBISearchServer()
        long totalAutoGenModels = retrieveTotalAutoGeneratedModelsFromEBISearchServer()
        long totalGoClasses = retrieveTotalGOClassesFromBioModels()
        long totalParametersEntries = retrieveTotalParametersEntriesFromEBISearchServer()
        Map<String, String> figuresRedisMap = [:]
        figuresRedisMap.put("total-submissions", totalSubmissions.toString())
        figuresRedisMap.put("total-individual-models", totalIndividualModels.toString())
        figuresRedisMap.put("total-curated-models", totalCuratedModels.toString())
        figuresRedisMap.put("total-non-curated-models", totalNonCuratedModels.toString())
        figuresRedisMap.put("total-auto-generated-models", totalAutoGenModels.toString())
        figuresRedisMap.put("total-go-classes", totalGoClasses.toString())
        figuresRedisMap.put("total-parameters-entries", totalParametersEntries.toString())
        redisService.doRedisHSet("hp-statistics-total-figures", figuresRedisMap)
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
        refreshStatisticsModellingApproachesRedisCache()
        refreshStatisticsOrganismsRedisCache()
        refreshStatisticsJournalsRedisCache()
    }

    private clearRedisCacheOfRecentlyPublishedModels(final Jedis jedis, final String key) {
        // Use redis-cli: redis-cli KEYS "hp-recently-published-models*" | xargs redis-cli DEL
        // Get all recently published models
        Map rpm  = redisService.doRedisHGetAll(key)
        // Iterate on the models to remove each of them (i.e. these caches look hp-recently-published-models-BIOMD...)
        for (String m in rpm.keySet()) {
            redisService.deleteAllByPattern(jedis, "$key-$m")
        }
        // Delete the cache named as the key (i.e. hp-recently-published-models)
        redisService.deleteAllByPattern(jedis, key)
    }

    private Map<String, String> buildModelOfTheMonthEntry() {
        Date now = new Date()
        final String query = "from ModelOfTheMonth where publishedFrom <= :now and :now < publishedUntil order by publishedFrom asc"
        ModelOfTheMonth theLatestMoM = ModelOfTheMonth.find(query, [now: now])
        if (!theLatestMoM) {
            query = "from ModelOfTheMonth order by publicationDate desc"
            theLatestMoM = ModelOfTheMonth.find(query)
            if (!theLatestMoM) { return null }
        }
        String entryTitle = theLatestMoM.title
        String shortDescription = theLatestMoM.shortDescription
        String previewImage = Base64.encoder.encodeToString(theLatestMoM.previewImage)
        Date publishedFromDate = theLatestMoM.publishedFrom
        def monthNumStr = new SimpleDateFormat("MM").format(publishedFromDate)
        def monthString = new SimpleDateFormat("MMMMM").format(publishedFromDate)
        def yearString = new SimpleDateFormat("YYYY").format(publishedFromDate)
        final String prefixLink = "${SVR_URL}/content/model-of-the-month".toString()
        def link = "${prefixLink}?year=${yearString}&month=${monthNumStr}".toString()
        def linkAll = "${prefixLink}?all=yes".toString()
        String titlePreviewImage = "Model of the month: ${monthString} ${yearString}".toString()
        String lastUpdatedBy = theLatestMoM.authors
        Set models = theLatestMoM.models
        String modelIds = models.collect { it.publicationId ?: it.submissionId }.join(";")
        Map<String, String> momEntry = [:]
        momEntry.put("id", Long.toString(theLatestMoM.id))
        momEntry.put("entryTitle", entryTitle)
        momEntry.put("shortDescription", shortDescription)
        momEntry.put("previewImage", previewImage)
        momEntry.put("monthNumStr", monthNumStr)
        momEntry.put("monthString", monthString)
        momEntry.put("yearString", yearString)
        momEntry.put("momEntryLink", link)
        momEntry.put("momEntryLinkAll", linkAll)
        momEntry.put("titlePreviewImage", titlePreviewImage)
        momEntry.put("lastUpdatedBy", lastUpdatedBy)
        momEntry.put("models", modelIds)
        momEntry
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
            [Name: d.name, Count: d.count, Taxonomy: d.taxonomy, NormalisedCount: d.normalisedCount]
        }
        returned
    }

    /**
     * Makes a statistic about models with names of the publication journal
     *
     * @return a {@link List} of Organism objects
     */
    private List makeStatisticsOnOrganisms() {
        String query = "biomodels?query=*:*&facetcount=1000&facetfields=TAXONOMY&format=json"
        def response = hitRemoteService(BioModels.EBI_SEARCH_RESTFUL_WS_URL, query)
        def taxons = response.json.facets[0].facetValues
        List<OrganismData> organismData = new ArrayList<OrganismData>()
        for (tax in taxons) {
            // initialise the normalised count as the count
            int normalisedCount = tax['count'] as int
            OrganismData d = new OrganismData(name: "${tax['label']}",
                count: tax['count'] as int, normalisedCount: normalisedCount, taxonomy: tax['value'])
            organismData.add(d)
        }

        LOGGER.info("Sorting the organism list before normalising counts")
        Collections.sort(organismData, new Comparator<OrganismData>() {
            @Override
            int compare(OrganismData o1, OrganismData o2) {
                return o2.count <=> o1.count
            }
        })
        List returnedList = normaliseOrganismCount(organismData)

        return returnedList
    }

    private Map<String, Integer> fetchStatisticsDataFromRedisCache(final String key) {
        Map<String, String> redisMap = redisService.doRedisHGetAll(key)
        Map<String, Integer> returnedMap = new HashMap<>()
        if (!redisMap) {
            // call the fallback
            switch (key) {
                case "hp-statistics-modelling-approaches":
                    LOGGER.info("Falling back to build statistics for modelling approaches")
                    returnedMap = buildStatisticsModellingApproaches()
                    addStatisticsModellingApproachesToRedis(returnedMap)
                    break
                case "hp-statistics-journals":
                    LOGGER.info("Falling back to build statistics for journals")
                    returnedMap = buildStatisticsJournals()
                    addStatisticsJournalsToRedis(returnedMap)
                    break
            }
        } else {
            returnedMap = convertFromRedisMap(redisMap)
        }
        return returnedMap
    }

    private Map fetchStatisticsOrganismsFromRedisCache(final String key = "hp-statistics-organisms") {
        redisService.doRedisHGetAll(key)
    }

    private Map<String, String> convert2RedisMap(final Map<String, Integer> inputMap) {
        Map<String, String> returnedMap = new HashMap<>()
        for (entry in inputMap) {
            returnedMap.put(entry.key, Integer.toString(entry.value))
        }
        returnedMap
    }

    private Map<String, Integer> convertFromRedisMap(final Map<String, String> inputMap) {
        Map<String, Integer> returnedMap = new HashMap<>()
        for (entry in inputMap) {
            returnedMap.put(entry.key, Integer.parseInt(entry.value))
        }
        returnedMap
    }

    Long fetchStatisticsTotalSubmissions() {
        // fetch the figure from Redis cache
        Long total = redisService.doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-submissions") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalSubmissionsFromEBISearchServer()
        }
        total
    }

    Long fetchStatisticsTotalIndividualModels () {
        // fetch the figure from Redis cache
        Long total = redisService.doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-individual-models") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalIndividualModelsFromEBISearchServer()
        }
        total
    }

    Long fetchStatisticsTotalAutoGeneratedModels () {
        // fetch the figure from Redis cache
        Long total = redisService.doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-auto-generated-models") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalAutoGeneratedModelsFromEBISearchServer()
        }
        total
    }

    Long fetchStatisticsTotalGOClasses() {
        // fetch the figure from Redis cache
        /*Long total = redisService.doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-go-classes") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalGOClassesFromBioModels()
        }
        total*/
        0
    }

    Long fetchStatisticsTotalParametersEntries() {
        // fetch the figure from Redis cache
        Long total = redisService.doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-parameters-entries") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalParametersEntriesFromEBISearchServer()
        }
        total
    }

    /**
     * Hits EBI Search Server to get this figure
     *
     * @return a long number as the total models
     */
    private long retrieveTotalSubmissionsFromEBISearchServer() {
        String query = "biomodels_all?query=*:*&format=json"
        def response = hitRemoteService(BioModels.EBI_SEARCH_RESTFUL_WS_URL, query)
        return response.json.hitCount as Long
    }

    /**
     * Hits EBI Search Server to get this figure
     *
     * @return a long number as the total models
     */
    private long retrieveTotalIndividualModelsFromEBISearchServer() {
        String query = "biomodels?query=*:*&format=json"
        def response = hitRemoteService(BioModels.EBI_SEARCH_RESTFUL_WS_URL, query)
        return response.json.hitCount as Long
    }

    private long retrieveTotalCuratedModelsFromEBISearchServer() {
        String query = "biomodels?query=*:* AND curationstatus:'Manually curated'&format=json"
        def response = hitRemoteService(BioModels.EBI_SEARCH_RESTFUL_WS_URL, query)
        return response.json.hitCount as Long
    }

    private long retrieveTotalNonCuratedModelsFromEBISearchServer() {
        String query = "biomodels?query=*:* AND curationstatus:'Non-curated'&format=json"
        def response = hitRemoteService(BioModels.EBI_SEARCH_RESTFUL_WS_URL, query)
        return response.json.hitCount as Long
    }

    /**
     * Hits EBI Search Server to get this figure
     *
     * @return a long number as the total models
     */
    private long retrieveTotalAutoGeneratedModelsFromEBISearchServer() {
        String query = "biomodels_autogen?query=*:*&format=json"
        def response = hitRemoteService(BioModels.EBI_SEARCH_RESTFUL_WS_URL, query)
        return response.json.hitCount as Long
    }

    /**
     * Hits Model Classifier Service to get this figure
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
        def response = hitRemoteService(BioModels.EBI_SEARCH_RESTFUL_WS_URL, query)
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
        Proxy proxy = configurationService.verifyHttpProxy()
        LOGGER.debug("HTTP PROXY: ${proxy?.dump()}")
        String queryURL = "${serverURL}/${query}"
        LOGGER.debug("Connecting to the service at $queryURL")
        RestBuilder rest
        if (proxy) {
            rest = new RestBuilder(connectTimeout: 10000, readTimeout: 100000, proxy: proxy)
        } else {
            rest = new RestBuilder(connectTimeout: 10000, readTimeout: 100000)
        }

        def response = rest.get(queryURL) {
            accept("application/json")
            contentType("application/json;charset=UTF-8")
        }
        response
    }

    private List<OrganismData> normaliseOrganismCount(final List<OrganismData> organismData) {
        // Take into account the fact that the input list was sorted in descending order
        LOGGER.debug("Scaling the counts of Organisms")
        ArrayList<OrganismData> originalData = new ArrayList<OrganismData>(organismData)
        ArrayList<OrganismData> normalisedData = new ArrayList<OrganismData>()
        ArrayList<Float> delta = new ArrayList<Float>()
        for (int i = 0; i < originalData.size() - 1; i++) {
            normalisedData.add(originalData[i])
            Integer normalisedCount = originalData[i].count
            Float d = originalData[i].count / originalData[i+1].count
            delta.add(d)
            normalisedCount = scaleDelta(normalisedCount, d)
            normalisedData[i].normalisedCount = (int) normalisedCount
        }
        OrganismData lastElement = originalData[originalData.size() - 1]
        normalisedData.add(lastElement)
        LOGGER.debug("Nb. elements: ${originalData.size()} -- ${normalisedData.size()}")
        normalisedData.toList()
    }

    private Integer scaleDelta(Integer normalisedCount, final Float d) {
        if (d > 2.0) {
            // decrease the count the i_th element just time, for example, 80% - 90% of
            // the delta between it and the closest lower count
            normalisedCount = (Integer) (normalisedCount / (d * 0.50))
        } else if (normalisedCount == 1 || normalisedCount == 2) {
            normalisedCount = normalisedCount * 2
        }
        normalisedCount
    }

    long fetchStatisticsCuratedModels() {
        // fetch the figure from Redis cache
        Long total = redisService.doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-curated-models") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalIndividualModelsFromEBISearchServer()
        }
        total
    }

    long fetchStatisticsNonCuratedModels() {
        // fetch the figure from Redis cache
        Long total = redisService.doRedisHGet(HP_STAT_TOTAL_FIGURE, "total-non-curated-models") as Long
        if (!total) {
            // call the fall back
            total = retrieveTotalIndividualModelsFromEBISearchServer()
        }
        total
    }
}
