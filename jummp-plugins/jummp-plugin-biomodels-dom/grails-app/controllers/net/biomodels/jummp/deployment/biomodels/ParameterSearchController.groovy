/**
 * Copyright (C) 2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import grails.rest.*

/**
* @author carankalle on 08/10/2018.
*/
@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
@Resource(uri = '/parameterSearch')
class ParameterSearchController {

    def parameterSearchService
    def grailsApplication
/**
 * The class logger.
 */
    static final Log log = LogFactory.getLog(ParameterSearchController.class)

    def index() {
        render(view: "index");
    }

    def search(ParameterSearchCommand command) {
        if (!command.validate()) {
            def msg = "Invalid request $command.query, $command.errors.allErrors"
            response.status = 400
            log.error(msg)
            render(['message': "Invalid request object"] as JSON)
            return
        }
        try {
            def result = parameterSearchService.getData(command)
            String responseformat = command.responseformat.toLowerCase()
            if (result.toString().length() == 0 || result.hasProperty('recordsTotal') && result['recordsTotal'] == 0) {
                String msg = "No matches found"
                render(['message': msg] as JSON)
            }
            if (responseformat == "json") {
                render(result as JSON)
            } else if (responseformat == "csv") {
                render(result)
            }
        } catch (IllegalArgumentException ie) {
            response.status = 400
            String msg = "Error encountered while processing $command: ${ie.message}"
            log.error(msg)
            render(['message': ie.getMessage()] as JSON)
        } catch (Exception ex) {
            response.status = 500
            String msg = "Error encountered while processing $command: ${ex.message}"
            log.error(msg, ex)
            render(['message': msg] as JSON)
        }
    }
}

