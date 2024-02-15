/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.scms

import grails.converters.JSON
import net.biomodels.jummp.scms.CmsContentTransportCommand as CCTC
import net.biomodels.jummp.utils.Slug
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured

import java.text.SimpleDateFormat

@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class CmsContentController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmsContentController.class)

    def cmsContentService
    def userService

    def index() {
        Map items = cmsContentService.getAllItemsWithParentNode()
        [titlePage: "List of all CMS items | BioModels", items: items]
    }

    def create() {
        LOGGER.debug("Started creating a new content...")
        println("Started creating a new content...")
        // -1 is a fake id so that it can meet a minimal condition in the editor form.
        // it will be granted a valid value at persisting in the db.
        CCTC content = new CCTC(id: -1,
            createdBy: userService.username, createdOn: new Date(),
            lastChangedBy: userService.username, lastChangedOn: new Date())
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        [content: content, dateFormat: dateFormat]
    }

    def show() {
        LOGGER.debug("Started showing content...")
        println("Started showing content...")
        Long id = params.long("id")
        Map data = cmsContentService.findDataAndRenderView(id, "show")
        render(plugin: data.plugin, controller: data.controller, view: data.view, model: data.model)
    }

    def save(CCTC cmd) {
        Map result = [:]
        String action = cmd.id == -1 ? "creating" : "saving"
        LOGGER.debug("Started $action the following content into the database...\n${cmd.toString()} ")
        println("Started $action the following content into the database...\n${cmd.toString()}")

        String message = ""
        String status = ""
        Long id = -1
        if (cmd?.validate()) {
            Map contentMap = cmsContentService.fromCommandObject(cmd)
            CmsContent content = contentMap.get("content") as CmsContent
            message = contentMap.get("message")
            if (message == "Success") {
                status = "Succeeded"
                message = "Saved content successfully"
                id = content.id
            } else {
                status = "Failed"
                message = "Failed to save content"
            }
        } else {
            status = "Failed"
            message = "Data invalid: ${cmd.errors.toString()}"
        }
        result.put("status", status)
        result.put("message", message)
        result.put("id", id)
        LOGGER.debug(result.toString())
        render(result as JSON)
    }

    def generateSlug() {
        String title = params.title.decodeHTML()
        String slug = Slug.make(title)
        render([slug: slug] as JSON)
    }
}
