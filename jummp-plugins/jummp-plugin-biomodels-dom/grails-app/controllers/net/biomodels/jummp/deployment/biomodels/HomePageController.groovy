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

import grails.converters.JSON
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.webapp.CommonController

@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class HomePageController extends CommonController {
    def decorationService
    private static PRE_TITLE

    @Override
    void setConfiguration(ConfigObject co) {
        super.setConfiguration(co)
        PRE_TITLE = "Report of updating"
    }

    def index() {
        String hpStatisticsDataForFeatures = createLink(controller: "homePage", action: "updateStatisticsDataForFeatures")
        String hpStatisticsDataForCharts = createLink(controller: "homePage", action: "updateStatisticsDataForCharts")
        String hpStatisticsModellingApproaches = createLink(controller: "homePage", action: "updateStatisticsModellingApproaches")
        String hpStatisticsOrganisms = createLink(controller: "homePage", action: "updateStatisticsOrganisms")
        String hpStatisticsJournals = createLink(controller: "homePage", action: "updateStatisticsJournals")
        String recentlyAccessedModels = createLink(controller: "homePage", action: "updateRecentlyAccessedModels")
        String recentlyPublishedModels = createLink(controller: "homePage", action: "updateRecentlyPublishedModels")
        String hpDataNewsWidget = createLink(controller: "homePage", action: "updateDataNewsWidget")
        String latestMomEntry = createLink(controller: "homePage", action: "updateMoMEntryOnRedisCache")
        String title = "Admin Board to update data on Home Page | BioModels"
        Map links = ["layout": layout, "title": title]
        links.put("hpStatisticsDataForFeatures", hpStatisticsDataForFeatures)
        links.put("hpStatisticsDataForCharts", hpStatisticsDataForCharts)
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
        String title = "${PRE_TITLE} Statistics For Features | BioModels"
        String message = "Updated statistics data for the feature widgets successfully"
        render(view: "report", model: [message: message, title: title, layout: layout])
    }
    /**
     * Updates the list of recently accessed models on Redis Cache
     */
    def updateRecentlyAccessedModels() {
        decorationService.refreshRecentlyAccessedModelsRedisCache()
        Map<String, String> models = decorationService.doRedisHGetAll("hp-recently-accessed-models")
        String title = "${PRE_TITLE} recently accessed models | BioModels"
        render(view: "update-recently-accessed-models",
            model: [models: models, title: title, layout: layout])
    }

    /**
     * Updates the list of recently published models on Redis Cache
     */
    def updateRecentlyPublishedModels() {
        decorationService.refreshRecentlyPublishedModelsRedisCache()
        Map<String, String> models = decorationService.doRedisHGetAll("hp-recently-published-models")
        String title = "${PRE_TITLE} recently published models | BioModels"
        render(view: "update-recently-published-models",
            model: [models: models, title: title, layout: layout])
    }

    /**
     * Updates the latest entry of Model Of The Month
     */
    def updateMoMEntryOnRedisCache() {
        decorationService.refreshModelOfTheMonthEntryRedisCache()
        String title = "${PRE_TITLE} Statistics For MoM Entry | BioModels"
        String message = "Updated statistics data for the MoM widget successfully"
        render(view: "report", model: [message: message, title: title, layout: layout])
    }
    /**
     * Updates statistical data for the widgets shown on the home page
     */
    def updateStatisticsDataForCharts() {
        decorationService.updateDataForChartsOnHomePage()
        String title = "${PRE_TITLE} Statistics For Charts | BioModels"
        String message = "Updated statistics data for the charts successfully"
        render(view: "report", model: [message: message, title: title, layout: layout])
    }

    /**
     * Updates statistic of models replied on modelling approach
     */
    def updateStatisticsModellingApproaches() {
        decorationService.refreshStatisticsModellingApproachesRedisCache()
        String title = "${PRE_TITLE} Statistics For Modelling Approaches Chart | BioModels"
        String message = "Updated statistics data for the modelling approach chart successfully"
        render(view: "report", model: [message: message, title: title, layout: layout])
    }

    /**
     * Updates statistic of models replied on organism
     */
    def updateStatisticsOrganisms() {
        decorationService.refreshStatisticsOrganismsRedisCache()
        String title = "${PRE_TITLE} Statistics For Organisms | BioModels"
        String message = "Updated statistics data for the organisms successfully"
        render(view: "report", model: [message: message, title: title, layout: layout])
    }

    /**
     * Updates statistic of models replied journal
     */
    def updateStatisticsJournals() {
        decorationService.refreshStatisticsJournalsRedisCache()
        String title = "${PRE_TITLE} Statistics For Journals | BioModels"
        String message = "Updated statistics data for the journals successfully"
        render(view: "report", model: [message: message, title: title, layout: layout])
    }

    /**
     * Updates data for News widget
     */
    def updateDataNewsWidget() {
        decorationService.refreshDataForNewsWidgetRedisCache()
        String title = "${PRE_TITLE} Statistics For News Widget | BioModels"
        String message = "Updated statistics data for the News widgets successfully"
        render(view: "report", model: [message: message, title: title, layout: layout])
    }

    /**
     * Updates the Redis Cache
     */
    def updateRedisCache() {
        String title = "${PRE_TITLE} data for widgets | BioModels"
        decorationService.updateDataForWidgetsOnHomePage()
        String message = "Updated data for widgets on the Home Page successfully"
        Map output = [message: message, title: title, layout: layout]
        withFormat {
            html {
                render(view: "report", model: output)
            }
            json {
                render(output as JSON)
            }
            xml {
                render(output as XML)
            }
            '*' {
                render view: '/errors/error415', status: 415
            }
        }
    }
}
