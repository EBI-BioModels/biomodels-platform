/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.filters

import grails.converters.JSON
import grails.converters.XML
import grails.util.Holders
import net.biomodels.jummp.deployment.biomodels.P2MMapping
import net.biomodels.jummp.deployment.biomodels.UhlenModelMapping
import net.biomodels.jummp.deployment.biomodels.AutoGenModelMapping

/**
 * @short Filter to redirect users to ModelController for Path2Models models
 *
 * About 140K models in Path2Models project are bringing to BioModels under hundreds of representative models. These
 * representative models are the main ones after grouping models into small categories. Each category will have a
 * delegate which is submitted into BioModels as the main file along with annotations and metadata information. The
 * other members of the group are archived into a zip file as an additional file and submitted together the main file
 * . If a user wants to access these member models, we will redirect them to the display page of the representative
 * model.
 *
 * The same approach will be applied for downloading these models.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date 2019-06-14
 */
class ModelFilters {
    final Set SUPPORTED_FORMAT = ['json', 'xml'] as HashSet
    final String serverURL = Holders.grailsApplication.config.grails.serverURL
    def filters = {
        showP2MModel(controller: "model", action: "show") {
            before = {
                String modelId = params.id
                if (modelId.contains("BMID") || modelId.contains("MODEL170711")) {
                    AutoGenModelMapping modelMap
                    if (modelId.contains("MODEL170711")) {
                        modelMap = UhlenModelMapping.findByMember(modelId)
                    } else if (modelId.contains("BMID")) {
                        modelMap = P2MMapping.findByMember(modelId)
                    }
                    if (modelMap) {
                        String representative = modelMap.representative
                        String format = params.format
                        if (format in SUPPORTED_FORMAT) {
                            response.setContentType(format == 'json' ? "application/json" : "application/xml")
                            response.status = 301
                            String newLocation = "${serverURL}/${representative}"
                            response.setHeader("Location", newLocation)
                            def result = [status: 301,
                                          message: "Resources have been moved permanently",
                                          Location: newLocation]
                            def returned = format == 'json' ? result as JSON : result as XML
                            render(returned)
                            return false
                        } else {
                            forward(controller: "model", action: "show", id: representative)
                        }
                    }
                }
            }
        }

        downloadP2MModel(controller: "model", action: "download") {
            before = {
                String modelId = params.id
                if (modelId.contains("BMID") || modelId.contains("MODEL170711")) {
                    AutoGenModelMapping modelMap
                    if (modelId.contains("MODEL170711")) {
                        modelMap = UhlenModelMapping.findByMember(modelId)
                    } else if (modelId.contains("BMID")) {
                        modelMap = P2MMapping.findByMember(modelId)
                    }
                    if (modelMap) {
                        String representative = modelMap.representative
                        forward(controller: "model", action: "download", id: representative)
                    }
                } 
            }
        }

        filesP2MModel(controller: "model", action: "files") {
            before = {
                String modelId = params.id
                if (modelId.contains("BMID") || modelId.contains("MODEL170711")) {
                    AutoGenModelMapping modelMap
                    if (modelId.contains("MODEL170711")) {
                        modelMap = UhlenModelMapping.findByMember(modelId)
                    } else if (modelId.contains("BMID")) {
                        modelMap = P2MMapping.findByMember(modelId)
                    }
                    if (modelMap) {
                        String representative = modelMap.representative
                        String format = params.format
                        if (format in SUPPORTED_FORMAT) {
                            response.setContentType(format == 'json' ? "application/json" : "application/xml")
                            response.status = 301
                            String newLocation = "${serverURL}/${representative}"
                            response.setHeader("Location", newLocation)
                            def result = [status: 301,
                                          message: "Resources have been moved permanently",
                                          Location: newLocation]
                            def returned = format == 'json' ? result as JSON : result as XML
                            render(returned)
                            return false
                        } else {
                            forward(controller: "model", action: "files", id: representative)
                        }
                    }
                }
            }
        }
    }
}
