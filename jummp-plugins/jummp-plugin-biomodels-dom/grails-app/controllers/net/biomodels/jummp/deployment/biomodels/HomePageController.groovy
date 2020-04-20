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

/**
 * Handling routes from and to HomePage
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class HomePageController {
    def decorationService

    def index() {
        String hpStatisticsDataForFeatures = createLink(controller: "homePage", action: "updateStatisticsDataForFeatures")
        String hpStatisticsDataForCharts = createLink(controller: "homePage", action: "updateStatisticsDataForCharts")
        String hpStatisticsCurationState = createLink(controller: "homePage", action: "updateStatisticsCurationState")
        String hpStatisticsModellingApproaches = createLink(controller: "homePage", action: "updateStatisticsModellingApproaches")
        String hpStatisticsOrganisms = createLink(controller: "homePage", action: "updateStatisticsOrganisms")
        String hpStatisticsJournals = createLink(controller: "homePage", action: "updateStatisticsJournals")
        String recentlyAccessedModels = createLink(controller: "homePage", action: "updateRecentlyAccessedModels")
        String recentlyPublishedModels = createLink(controller: "homePage", action: "updateRecentlyPublishedModels")
        String hpDataNewsWidget = createLink(controller: "homePage", action: "updateDataNewsWidget")
        String latestMomEntry = createLink(controller: "homePage", action: "updateMoMEntryOnRedisCache")
        Map links = [:]
        links.put("hpStatisticsDataForFeatures", hpStatisticsDataForFeatures)
        links.put("hpStatisticsForCharts", hpStatisticsDataForCharts)
        links.put("hpStatisticsCurationState", hpStatisticsCurationState)
        links.put("hpStatisticsModellingApproaches", hpStatisticsModellingApproaches)
        links.put("hpStatisticsOrganisms", hpStatisticsOrganisms)
        links.put("hpStatisticsJournals", hpStatisticsJournals)
        links.put("recentlyAccessedModels", recentlyAccessedModels)
        links.put("recentlyPublishedModels", recentlyPublishedModels)
        links.put("hpDataNewsWidget", hpDataNewsWidget)
        links.put("latestMomEntry", latestMomEntry)
        links
    }

    /**
     * Updates statistical figures of the widgets in Features section of the home page
     */
    def updateStatisticsDataForFeatures() {
        decorationService.refreshStatisticsDataForFeatures()
        render "updated statistics data for the feature widgets"
    }
    /**
     * Updates the list of recently accessed models on Redis Cache
     */
    def updateRecentlyAccessedModels() {
        decorationService.refreshRecentlyAccessedModelsRedisCache()
        Map<String, String> models = decorationService.doRedisHGetAll("hp-recently-accessed-models")
        render(view: "update-recently-accessed-models", model: [models: models])
    }

    /**
     * Updates the list of recently published models on Redis Cache
     */
    def updateRecentlyPublishedModels() {
        decorationService.refreshRecentlyPublishedModelsRedisCache()
        Map<String, String> models = decorationService.doRedisHGetAll("hp-recently-published-models")
        render(view: "update-recently-published-models", model: [models: models])
    }

    /**
     * Updates the latest entry of Model Of The Month
     */
    def updateMoMEntryOnRedisCache() {
        decorationService.refreshModelOfTheMonthEntryRedisCache()
        render "OK"
    }
    /**
     * Updates statistical data for the widgets shown on the home page
     */
    def updateStatisticsDataForCharts() {
        decorationService.updateDataForChartsOnHomePage()
        render "updated statistics data for all charts"
    }

    /**
     * Updates statistic of models relied on curation state
     */
    def updateStatisticsCurationState() {
        decorationService.refreshStatisticsCurationStateRedisCache()
        render "updated statistics curation state"
    }
    /**
     * Updates statistic of models replied on modelling approach
     */
    def updateStatisticsModellingApproaches() {
        decorationService.refreshStatisticsModellingApproachesRedisCache()
        render "updated statistics modelling approaches"
    }

    /**
     * Updates statistic of models replied on organism
     */
    def updateStatisticsOrganisms() {
        decorationService.refreshStatisticsOrganismsRedisCache()
        render "updated statistics organisms"
    }

    /**
     * Updates statistic of models replied journal
     */
    def updateStatisticsJournals() {
        decorationService.refreshStatisticsJournalsRedisCache()
        render "updated statistics journals"
    }

    /**
     * Updates data for News widget
     */
    def updateDataNewsWidget() {
        decorationService.refreshDataForNewsWidgetRedisCache()
        render  "updated data for News Widget"
    }
}
