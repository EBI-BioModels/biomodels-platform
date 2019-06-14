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

import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.plugins.security.User
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
@Mock([User, Tag])
class TagTransportCommandSpec extends Specification {
    void "test providing a full specification from a command object"() {
        given: "a Tag object"
        Date dateCreated = new Date()
        Date dateModified = new Date()
        User userCreated = new User(username: "elvis")
        Tag tag = new Tag(name: "Sample model", userCreated: userCreated)
        tag.description = "This is a sample model"
        tag.dateCreated = dateCreated
        tag.dateModified = dateModified
        assert tag.validate()

        when: "convert this Tag object to command object"
        TagTransportCommand cmd = tag.toCommandObject()

        then: "the command keeps original information of the initial Tag object"
        cmd.validate()
    }
}
