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

import grails.transaction.Transactional
import net.biomodels.jummp.model.Tag
import net.biomodels.jummp.plugins.security.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service for handling CRUD operations on tags/labels being used in BioModels
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional
class TagService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagService.class)
    def springSecurityService

    List<TagTransportCommand> getAll() {
        List<Tag> tags = Tag.getAll()
        List<TagTransportCommand> commands = tags.collect {
            TagTransportCommand.fromTag(it)
        }
        commands
    }

    List<String> getAllTagNames() {
        List<Tag> tags = Tag.getAll()
        tags.collect { it.name }.asList()
    }

    List<String> search(String s) {
        List<Tag> matchedTags = Tag.findAllByNameLike("%${s}%")
        List<String> result = matchedTags*.name
        result
    }

    Tag create(String name, String description, User userCreated = null) {
        Tag tagObj = Tag.findOrCreateByName(name)
        if (!tagObj.id) {
            tagObj.dateCreated = new Date()
            tagObj.dateModified = new Date()
        }
        if (!userCreated) {
            userCreated = springSecurityService.currentUser
        }
        tagObj.description = description
        tagObj.userCreated = userCreated
        Tag returned = tagObj.save(flush: true)
        if (returned) {
            LOGGER.debug("""\
Tag ${returned.name} (${returned.description}) has been created or updated successfully \
by ${returned.userCreated.username}.""")
        } else {
            LOGGER.error("""\
An error occurred when creating or updating the tag $name ($description) by ${userCreated.username}.""")
        }
        returned
    }

    Tag createOrUpdate(TagTransportCommand command) {
        // this command object was validated in the controller before coming here
        Tag tag
        if (command?.id) {
            tag = Tag.get(command?.id)
            tag.dateModified = new Date()
        } else {
            tag = new Tag()
            Date current = new Date()
            tag.dateCreated = current
            tag.dateModified = current
        }
        tag.name = command.name
        tag.description = command.description
        tag.userCreated = User.findByUsername(command.userCreated)
        Tag saved = tag.save(flush: true)
        if (saved) {
            LOGGER.debug("""\
The tag (${tag.name}) was created or updated successfully""")
        } else {
            LOGGER.error("""\
There have been errors while persisting the tag (${tag.name}) into the database
because of ${tag.errors.allErrors.inspect()}""")
        }
        saved
    }
}
