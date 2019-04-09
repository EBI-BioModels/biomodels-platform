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
import net.biomodels.jummp.plugins.security.User
import java.text.SimpleDateFormat

/**
 * This controller defines logical routes to communication between Tag and view
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class TagController {
    def springSecurityService
    def userService
    def tagService

    def index() {
        List<TagTransportCommand> tags = tagService.getAll()
        [tags: tags]
    }

    def show() {
        Tag tag = Tag.get(params.int("id"))
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        [tag: tag, dateFormat: dateFormat]
    }

    def create(String name) {
        def user = springSecurityService.getCurrentUser()
        Tag tagObj = tagService.create(name, user)
        if (tagObj) {
            render(tagObj as JSON)
        } else {
            String errMsg = "There is an error while trying to persist the tag '$name' in the database."
            render([message: errMsg] as JSON)
        }
    }

    def add() {
        User user = springSecurityService.getCurrentUser()
        Date current = new Date()
        Tag tag = new Tag(name: "", userCreated: user)
        tag.dateCreated = current
        tag.dateModified = current
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        TagTransportCommand tagCmd = new TagTransportCommand(name: "", userCreated: user?.username)
        tagCmd.dateCreated = dateFormat.format(current)
        tagCmd.dateModified = dateFormat.format(current)
        [tag: tag, dateFormat: dateFormat, tagCmd: tagCmd]
    }

    def update(TagTransportCommand command) {
        Map result = [:]
        if (!command.validate()) {
            result["message"] = "There have been errors while making a data binding for the object ${command.dump()}"
        } else {
            Tag tag = tagService.update(command)
            if (tag) {
                result["message"] = "Updated tag '${tag.name}' successfully"
            } else {
                result["message"] = "There have been errors while saving the update on the tag ${command.name}"
            }
        }
        //render(result as JSON)
        render(view: "update", model: ['message': result["message"]])
    }
}
