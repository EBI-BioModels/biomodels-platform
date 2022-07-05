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

import grails.transaction.Transactional
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.scms.CmsContentTransportCommand as CCTC
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Transactional
class CmsContentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmsContentService.class)

    def springSecurityService
    def userService

    Map fromCommandObject(CCTC cmd) {
        cmd.content = cmd.content.decodeHTML()
        User createdBy = User.findByUsername(cmd.createdBy)
        User lastChangedBy = User.findByUsername(cmd.lastChangedBy)
        CmsContent content = findByTransportCommand(cmd)
        if (content) {
            // save an existing content
            content.aliasURI = cmd.aliasURI
            content.title = cmd.title
            content.description = cmd.description
            content.content = cmd.content

            // TODO: correct this property later when building the contents browser
            content.parent = content

            content.createdBy = createdBy
            content.createdOn = cmd.createdOn
            content.lastChangedBy = lastChangedBy
            content.lastChangedOn = cmd.lastChangedOn
        } else {
            // create a new content which the parent property is set null as a default
            content = new CmsContent(aliasURI: cmd.aliasURI,
                title: cmd.title, description: cmd.description, content: cmd.content, parent: null,
                createdBy: createdBy, createdOn: cmd.createdOn,
                lastChangedBy: lastChangedBy, lastChangedOn: cmd.lastChangedOn)
        }
        Map result = [:]
        if (content.save(flush: true)) {
            result.put("message", "Success")
        } else {
            result.put("message", "Failure")
        }
        result.put("content", content)
        result
    }

    CmsContent findByTransportCommand(CCTC cmd) {
        CmsContent content = null
        if (cmd?.id || cmd?.id != -1) {
            content = CmsContent.get(cmd.id)
        } else if (cmd?.aliasURI) {
            content = CmsContent.executeQuery("from CmsContent as c where c.aliasURI = :aliasURI",
                [aliasURI: cmd.aliasURI])?.first()
        }
        content
    }

    // allows only admin and curators to edit and create contents
    boolean canEdit() {
        boolean isLoggedIn = springSecurityService.isLoggedIn()
        if (!isLoggedIn) { return false }
        boolean hasAdminOrCuratorRole = userService.isLoggedInUserACurator() || userService.isLoggedInUserAAdmin()
        hasAdminOrCuratorRole
    }
}
