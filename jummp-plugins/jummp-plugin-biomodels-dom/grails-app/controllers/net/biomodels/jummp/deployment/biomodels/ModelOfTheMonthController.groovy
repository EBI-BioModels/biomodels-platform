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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Grails, Spring Security (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Grails, Spring Security used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import java.text.SimpleDateFormat

@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class ModelOfTheMonthController {
    def modelDelegateService
    def modelOfTheMonthService

    def index() {
        List<ModelOfTheMonthTransportCommand> entries = modelOfTheMonthService.list()
        [entries: entries]
    }

    def updatePreviewImageAndShortDescription() {
        List<ModelOfTheMonth> models = modelOfTheMonthService.updatePreviewImageAndShortDescription()
        String updateReport = ""
        models.each {ModelOfTheMonth model ->
            updateReport += "${model.publicationDate.toString()}: ${model.shortDescription}<br/>"
        }
        render updateReport
    }

    def create() {
        Date current = new Date()
        String yearDate = current.format(ModelOfTheMonth.DATE_FORMAT_PATTERN)
        ModelOfTheMonthTransportCommand entry = new ModelOfTheMonthTransportCommand(formattedEntryDate: yearDate, lastUpdated: current, publicationDate: current)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        [entry: entry, dateFormat: dateFormat]
    }

    def show(ModelOfTheMonth entry) {
        if (!entry) {
            // render out the error
        }
        ModelOfTheMonthTransportCommand command = entry.toCommandObject()
        command.formattedEntryDate = command.publicationDate.format(ModelOfTheMonth.DATE_FORMAT_PATTERN)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        [entry: command, dateFormat: dateFormat]
    }

    def save(ModelOfTheMonthTransportCommand command) {
        if (params?.id) {
            command.id = params.long("id")
        }
        if (params["previewImage"]) {
            command.previewImage = params["previewImage"]
            command.mimeType = params["mimeType"]
        }
        command.updated = command?.id ? true : false
        Map result = [:]
        if (command?.validate()) {
            ModelOfTheMonth updated = modelOfTheMonthService.doCreateOrUpdate(command)
            if (updated) {
                result.status = 200
                result['entity'] = updated
                result['id'] = updated.id
                result['message'] = "The record has been updated successfully"
            } else {
                result.status = 400
                result['message'] = "There is an error while trying to persist the entry into the database"
                result['errors'] = updated.errors.getFieldErrors()
            }
        } else {
            result.status = 422
            result['errors'] = command.errors.allErrors.inspect()
            result['message'] = "Sorry, but your form was not submitted because it is not valid. Please correct or enter valid values into the required fields if they are missing. Click Save button again when you finish it!"
        }
        response.status = result.status
        render(result as JSON)
    }
}
