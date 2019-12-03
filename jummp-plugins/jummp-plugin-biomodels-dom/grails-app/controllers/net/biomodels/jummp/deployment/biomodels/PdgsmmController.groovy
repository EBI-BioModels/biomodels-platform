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
 */

package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import grails.rest.Resource
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * This controller defines logical routes to communication between UhlenModelService and corresponding views
 *
 * @author Tung Nguyen <nvntung@gmail.com>
 */
@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
@Resource(uri = "/pdgsmm")
class PdgsmmController {
    static final Log log = LogFactory.getLog(PdgsmmController.class)
    def uhlenModelService

    def index() {
        Map result = uhlenModelService.getModelCategoryMap(AutoGenModelIdentifierPrefix.PDGSMM.prefix)
        def diseases = result.keySet()
        render(view: "index", model: [title: "PDGSM models", diseases: diseases, categories: result])
    }

    def missing() {
        List<String> missingIds = uhlenModelService.findMissing()
        withFormat {
            html {
                render(view: "missing", model: [missing: missingIds])
            }
            json {
                response.setContentType("application/json")
                response.status = 200
                render([missing: missingIds] as JSON)
            }
            xml {
                response.setContentType("application/xml")
                response.status = 200
                render([missing: missingIds] as XML)
            }
        }
    }

    def representative() {
        String requestedModelId = params.model
        def status = 200
        def message = ""
        if (!requestedModelId) {
            status = 402
            message = "Missing required parameters"
            response.status = status
            render(view: "error", model: [status: status, message: message])
            return
        }
        Map<String, String> result = [:]
        result["requestedModelId"] = requestedModelId
        String representativeModelId = uhlenModelService.getRepresentativeId(requestedModelId)
        result["representativeModelId"] = representativeModelId
        withFormat {
            html {
                render(view: "representative", model: result) }
            json {
                response.setContentType("application/json")
                response.status = 200
                render(result as JSON)
            }
            xml {
                response.setContentType("application/xml")
                response.status = 200
                render(result as XML)
            }
        }
    }

    def representatives() {
        String paramsModelIds = params.get('modelIds')
        List modelIds = paramsModelIds?.split(",")
        Map reps = uhlenModelService.getRepresentatives(modelIds)

        withFormat {
            html {
                render(view: "representatives", model: [representatives: reps])
            }
            json {
                response.setContentType("application/json")
                response.status = 200
                render(reps as JSON)
            }
            xml {
                response.setContentType("application/xml")
                response.status = 200
                render(reps as XML)
            }
        }
    }
}
