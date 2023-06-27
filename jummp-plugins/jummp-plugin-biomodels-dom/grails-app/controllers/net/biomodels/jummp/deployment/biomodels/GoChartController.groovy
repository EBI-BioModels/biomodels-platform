/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.ModelDetails
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @short: The controller responsible for retrieving GoChart
 *
 * @author: Vu Tu <tvu@ebi.ac.uk>
 */
@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
class GoChartController {

    /**
     * Dependency Injection of ModelService
     */
    def modelService

    /**
     * Dependency Injection of ModelClassifierService
     */
    def modelClassifierService

    private static final Logger LOGGER = LoggerFactory.getLogger(GoChartController.class)

    def index() {
        try {
            List data = modelService.getAllModelWithDetails(false)
            List<ModelDetails> models = data.collect {
                new ModelDetails(it[0] as Model, it[1] as String, it[2] as Date)
            }
            ['classifiedModels': modelClassifierService.classify(models)]
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e)
            forward(controller: "errors", action: "error500", plugin: "jummp-plugin-web-application")
        }
    }
}
