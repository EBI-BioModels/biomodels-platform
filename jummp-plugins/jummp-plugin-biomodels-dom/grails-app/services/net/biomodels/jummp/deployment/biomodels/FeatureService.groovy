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
 */

package net.biomodels.jummp.deployment.biomodels

import grails.transaction.Transactional
import grails.util.Holders
import net.biomodels.jummp.scms.CmsContent
import net.biomodels.jummp.utils.redis.RedisService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @short Service responsible for creating and managing feature pages.
 *
 * <p>This service class is used for creating and managing some special pages using the simple CMS.</p>
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
@Transactional
class FeatureService {
    private final Logger LOGGER = LoggerFactory.getLogger(FeatureService.class)

    RedisService redisService = Holders.grailsApplication.mainContext.getBean("redisService") as RedisService
    def groovyPageRenderer

    String getContentForBioModelsTerms() {
        def newsQuery = """from CmsContent where aliasURI = :aliasuri order by createdOn desc"""
        def newsItem = CmsContent.executeQuery(newsQuery, [aliasuri: 'biomodels-terms'], [max: 1])
        newsItem[0]?.content
    }

    /**
     * Retrieves the content of the COVID-19 page under Browse menu
     *
     * <p>Temporarily, we store the content of this page as a News item. Using CmsContent domain class, it can be
     * retrieved by running the query. That News item has been set status Reviewed and had to keep the aliasuri as
     * covid-19 to make sure the related service still working.
     *
     * @return A String representing the content of the page
     */
    String getCovid19PageContent() {
        def newsQuery = """from CmsContent where aliasURI = :aliasuri order by createdOn desc"""
        def newsItem = CmsContent.executeQuery(newsQuery, [aliasuri: 'covid-19'], [max: 1])
        newsItem[0]?.content
    }

    String getContentForReproducibilityPage() {
        def newsQuery = """from CmsContent where aliasURI = :aliasuri \
order by createdOn desc"""
        def newsItem = CmsContent.executeQuery(newsQuery, [aliasuri: 'reproducibility'], [max: 1])
        newsItem[0]?.content
    }

    String getContentForFROGPage() {
        def newsQuery = """from CmsContent where aliasURI = :aliasuri \
order by createdOn desc"""
        def newsItem = CmsContent.executeQuery(newsQuery, [aliasuri: 'fbc'], [max: 1])
        newsItem[0]?.content
    }

    List getContentForModelOfTheYear2022CompetitionPage() {
        def newsQuery = """FROM CmsContent where aliasURI = :aliasuri \
order by createdOn desc"""
        def newsItem = CmsContent.executeQuery(newsQuery, [aliasuri: 'model-of-the-year-2022-competition'], [max: 1])
        [newsItem[0]?.id, newsItem[0]?.content]
    }

    List getContentForModelOfTheYear2023CompetitionPage() {
        def newsQuery = """FROM CmsContent where aliasURI = :aliasuri \
order by createdOn desc"""
        def newsItem = CmsContent.executeQuery(newsQuery, [aliasuri: 'model-of-the-year-2023-competition'], [max: 1])
        [newsItem[0]?.id, newsItem[0]?.content]
    }

    List getContentForModelOfTheYear2024CompetitionPage() {
        def newsQuery = """FROM CmsContent where aliasURI = :aliasuri \
order by createdOn desc"""
        def newsItem = CmsContent.executeQuery(newsQuery, [aliasuri: 'model-of-the-year-2024-competition'], [max: 1])
        [newsItem[0]?.id, newsItem[0]?.content]
    }

    String getSvgAgedBrain() {
        final String SVG_AGED_BRAIN = "svg-aged-brain"
        // load the SVG content from Redis cache
        String svgAgedBrain = redisService.doRedisGet(SVG_AGED_BRAIN)
        if (!svgAgedBrain) {
            println("Rendering the AgedBrain page directly")
            LOGGER.debug("Rendering the AgedBrain page directly")
            svgAgedBrain = groovyPageRenderer.render(template: "/templates/svgAgedBrain",
                plugin: "jummp-plugin-biomodels-dom")
            // cache the svgAgedBrain to Redis server
            if (svgAgedBrain) {
                println("Caching the AgedBrain page on Redis cache")
                LOGGER.debug("Caching the AgedBrain page on Redis cache")
                redisService.doRedisSet(SVG_AGED_BRAIN, svgAgedBrain)
            } else {
                svgAgedBrain = "There has been an error when trying to load the Model space in neurodegeneration - model landscape map."
            }
        } else {
            println("Retrieving the AgedBrain page from Redis cache")
            LOGGER.debug("Retrieving the AgedBrain page from Redis cache")
        }

        svgAgedBrain
    }
}
