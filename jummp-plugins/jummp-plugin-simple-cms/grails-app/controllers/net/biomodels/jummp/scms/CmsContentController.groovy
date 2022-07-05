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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured
import net.biomodels.jummp.plugins.security.User

import java.text.SimpleDateFormat


@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class CmsContentController {
    private static final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    def cmsContentService
    def userService

    def create() {
        // -1 is a fake id that will be granted a valid value
        CCTC content = new CCTC(id: -1,
            createdBy: userService.username, createdOn: new Date(),
            lastChangedBy: userService.username, lastChangedOn: new Date())
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        [content: content, dateFormat: dateFormat]
    }

    def save(CCTC cmd) {
        Map result = [:]
        LOGGER.debug(cmd.dump())

        String message = ""
        String status = ""
        if (cmd?.validate()) {
            Map contentMap = cmsContentService.fromCommandObject(cmd)
            CmsContent content = contentMap.get("content")
            message = contentMap.get("message")
            if (message == "Success") {
                status = "Succeeded"
                message = "Saved content successfully"
            } else {
                status = "Failed"
                message = "Failed to save content"
            }
        } else {
            status = "Succeeded"
            message = "Data invalid: ${cmd.errors.toString()}"
        }
        result.put("status", status)
        result.put("message", message)
        render(result as JSON)
    }
}
