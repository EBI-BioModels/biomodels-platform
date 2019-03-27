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
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import grails.rest.*
import org.springframework.validation.FieldError

/**
 * @author carankalle on 08/10/2018.
 */

@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
@Resource(uri = '/parameterSearch')
class ParameterSearchController {

    def parameterSearchService
/**
 * The class logger.
 */
    static final Log log = LogFactory.getLog(ParameterSearchController.class)

    final String IOExceptionCustomMessage = "Unable to retrieve data from EBI Search due to some problem with the request " +
        "parameters, please use the suitable request parameters and try again"

    def index(ParameterSearchCommand command) {
        if(!command.validate()) {
            def msg = "Invalid request $command.query, $command.errors.allErrors"
            log.error(msg)
            return['message': msg,"command":command]
        }
        render(view: "index",model:[command: command])
    }

    def search(ParameterSearchCommand command) {
        String commandErrorMessage
        def errorObject = ['recordsTotal': 0, 'recordsFiltered': 0, 'entries': []]
        final def NoMatchesFoundMessage = "No matches found"
        String format = "xml"
        try {
            withFormat {
                json {
                    format = "json"
                    if (!validateCommandObject(command, format)) {
                        errorObject['message'] = commandErrorMessage
                        renderErrorMessage(errorObject, format, 400)
                        return
                    }
                    ParameterSearchResults resultJSON = parameterSearchService.getJSONData(command)
                    if(resultJSON.hasProperty('recordsTotal') && resultJSON['recordsTotal'] == 0) {
                        errorObject['message'] = NoMatchesFoundMessage
                        renderErrorMessage(errorObject,format,200)
                        return
                    }
                    response.setContentType("application/json")
                    render(resultJSON as JSON)
                }
                xml {
                    format = "xml"
                    if(!validateCommandObject(command, format)) {
                        return
                    }
                    ParameterSearchResults resultJSON = parameterSearchService.getJSONData(command)
                    if(resultJSON.hasProperty('recordsTotal') && resultJSON['recordsTotal'] == 0) {
                        renderErrorMessage(NoMatchesFoundMessage,format,200)
                        return
                    }
                    response.setContentType("text/xml")
                    render(resultJSON as XML)

                }
                csv {
                    format = "csv"
                    if(!validateCommandObject(command, format)) {
                        return
                    }
                    String resultCSV = parameterSearchService.getCSVData(command)
                    if(null == resultCSV || resultCSV.isEmpty()) {
                        renderErrorMessage(NoMatchesFoundMessage,format,200)
                        return
                    }
                    response.setContentType("text/csv")
                    render(resultCSV)
                }
                '*' {
                    response.status = 415
                   render (['message': "Invalid format, please choose the format from JSON,XML and CSV"] as JSON)
                }
            }
        } catch (IllegalArgumentException ie) {
            log.error(ie.message, ie)
            renderErrorMessage(ie.getMessage(), format,400)
        }catch(IOException ioe) {
            log.error(ioe.message,ioe)
            renderErrorMessage(IOExceptionCustomMessage,format,400)

        } catch (Exception ex) {
            String msg = "Error encountered while processing $command, No Matches found"
            log.error(ex.message, ex)
            renderErrorMessage(msg,format,500)
        }
    }

    def export(ParameterSearchCommand command) {

        String query = command.query
        final String NoMatchesFoundMessage = "No matches found"
        String format = "csv"
        if(!validateCommandObject(command, format)){
            return
        }
        try {
            String resultCSV = parameterSearchService.exportData(command)
            if (null == resultCSV || resultCSV.isEmpty()) {
                renderErrorMessage(NoMatchesFoundMessage, format, 200)
                return
            }
            String filename = "BioModels_Parameters_Export-${new Date().format("yyyy-MM-dd")}.csv"
            response.setContentType("application/octet-stream")
            response.setHeader("Content-Disposition", "attachment;filename=${filename}")
            render(resultCSV)

        } catch (IllegalArgumentException ie) {
            log.error(ie.message, ie)
            renderErrorMessage(ie.getMessage(), format, 400)
        } catch (IOException ioe) {
            log.error(ioe.message, ioe)
            renderErrorMessage(IOExceptionCustomMessage, format, 400)
        } catch (Exception ex) {
            String msg = "Error encountered while processing $command, No Matches found"
            log.error(ex.message, ex)
            renderErrorMessage(msg, format, 500)
        }

    }

    private boolean validateCommandObject(ParameterSearchCommand command, String format) {
        if (!command.validate()) {
            response.status = 400
            String errorString = parseErrors(command.errors.fieldErrors)
            String commandErrorMessage = "Invalid request parameter. $errorString "
            log.error(commandErrorMessage)
            renderErrorMessage(commandErrorMessage, format, 400)
            return false
        }
        return true
    }

    private void renderErrorMessage(def msg, String format, int statusCode) {
        def responseContent
        response.status = statusCode
        if(format == "json") {
            render(msg as JSON)
        }else if(format == "xml") {
            responseContent = "<errors><message>${msg}</message></errors>"
            response.setContentType("text/xml")
            render(responseContent)
        }else if(format == "csv") {
            response.setContentType("text/plain")
            render(msg)
        }
    }

    private static String parseErrors (List<FieldError> fieldErrors) {
        String errorString = ""
        for(int i=0; i<fieldErrors.size(); i++) {
            String rejectedField = fieldErrors.get(i).field + "=" + fieldErrors.get(i).rejectedValue
            if(!errorString.isEmpty()) {
                errorString += ", " + rejectedField
            }else {
                errorString = "Rejected parameters: " + rejectedField
            }
        }
        return  errorString
    }
}

