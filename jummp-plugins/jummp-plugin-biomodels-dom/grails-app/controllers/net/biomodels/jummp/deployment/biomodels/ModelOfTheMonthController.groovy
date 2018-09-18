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
import groovy.json.JsonSlurper

import java.text.SimpleDateFormat

@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class ModelOfTheMonthController {
    def modelDelegateService
    def modelOfTheMonthService

    def index() {
        render(view: "index", model: [entries: list()])
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
        respond new ModelOfTheMonth(params)
    }

    List list() {
        List<ModelOfTheMonth> entries = modelOfTheMonthService.list()
        entries
    }

    def show() {
        ModelOfTheMonthTransportCommand command
        if (params?.id) {
            int id = params.int("id")
            command = modelOfTheMonthService.get(id)
            command.id = id
        } else {
            Date currentDate = new Date()
            String date = currentDate.format(ModelOfTheMonth.DATE_FORMAT_PATTERN)
            command = new ModelOfTheMonthTransportCommand(date: date)
            command.publicationDate = currentDate
            command.lastUpdated = currentDate
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        render(view: "show", model: [entry: command, dateFormat: dateFormat])
    }

    def save() {
        // retrieve data from the parameters
        def momEntryTC = params.momEntryTC
        def id = params.long("id")
        // sanitise the data feed by the end users
        // then convert them into the transport command object
        ModelOfTheMonthTransportCommand command = parseMoMEntryTC(momEntryTC, id)
        // get the latest timestamp
        command.lastUpdated = new Date()
        command.updated = command?.id ? true : false
        Map response = [:]
        if (command?.validate()) {
            ModelOfTheMonth updated = modelOfTheMonthService.doCreateOrUpdate(command)
            if (updated) {
                response['message'] = "The record has been updated successfully"
                response['id'] = updated.id
            } else {
                response['message'] = "There is an error while trying to persist the entry into the database"
            }
        } else {
            String defaultMessage = command.errors.getFieldError("title")?.defaultMessage
            if (defaultMessage?.contains("cannot be blank")) {
                response['message'] = "The title cannot be blank"
            } else {
                response['message'] = command.errors.allErrors.inspect()
            }
        }

        render(response as JSON)
    }

    ModelOfTheMonthTransportCommand parseMoMEntryTC(def momEntryTC, def id) {
        momEntryTC = new JsonSlurper().parseText(momEntryTC)
        String authors = momEntryTC["authors"].encodeAsHTML()
        String title = momEntryTC["title"].encodeAsHTML()
        String shortDescription = momEntryTC["shortDescription"].encodeAsHTML()
        def newPublicationDate = momEntryTC["publicationDate"]
        def newLastUpdated = momEntryTC["lastUpdated"]
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        Date publicationDate = dateFormat.parse(newPublicationDate)
        Date lastUpdated = dateFormat.parse(newLastUpdated)
        String date = publicationDate?.format(ModelOfTheMonth.DATE_FORMAT_PATTERN)
        String mimeType = momEntryTC["mimeType"]
        boolean updated = momEntryTC["updated"]
        def models = momEntryTC["models"]
        List<String> modelIds = models.split(", ")
        Map<Long, String> associatedModels = modelDelegateService.findModelsBySubmissionOrPublicationId(modelIds)
        def bindingMap = [id: id,
                          authors: authors,
                          title: title,
                          models: associatedModels,
                          shortDescription: shortDescription,
                          publicationDate: publicationDate,
                          lastUpdated: lastUpdated,
                          date: date,
                          mimeType: mimeType,
                          updated: updated]
        ModelOfTheMonthTransportCommand command = new ModelOfTheMonthTransportCommand(bindingMap)
        if (params?.id) {
            command.id = params.long("id")
        }
        if (momEntryTC["previewImage"]) {
            command.previewImage = Base64.decoder.decode(momEntryTC["previewImage"])
            command.mimeType = momEntryTC["mimeType"]
        }
        command
    }
}
