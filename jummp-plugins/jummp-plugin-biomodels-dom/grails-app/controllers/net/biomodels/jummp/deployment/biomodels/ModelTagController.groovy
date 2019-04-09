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
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * This controller defines logical routes to communication between ModelTagService and Their Views
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class ModelTagController {
    /**
     * The class logger
     */
    private static final Log log = LogFactory.getLog(ModelTagController.class)

    def tagService
    def modelTagService
    def springSecurityService

    def fetchTagsForSelect2() {
        String term = params.get("search").encodeAsHTML()
        List<String> tags = tagService.search(term)
        List result = []
        tags.eachWithIndex { String value, Long index ->
            result.add(["id": index, "text": value])
        }
        def rt = ["results": result] as JSON
        render rt
    }

    def updateModelTag() {
        def tagParams = params.list("updatedTags")[0]
        Set<String> updatedTags = tagParams != "" ? tagParams.split(",") : [].toSet()
        def modelId = params.get("modelId")
        def user = springSecurityService.getCurrentUser()
        Map result = modelTagService.update(updatedTags, modelId, user)
        response.status = result["status"]
        render(result as JSON)
    }
}
