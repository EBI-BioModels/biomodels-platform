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
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import spock.lang.Specification

import java.text.SimpleDateFormat

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestMixin(ServiceUnitTestMixin)
@Mock([Person, User, Tag])
@TestFor(TagService)
class TagServiceSpec extends Specification {

    def setup() {
        Person person = new Person(userRealName: "Elvis Nguyen")
        assert person.save()
        User user = new User(person: person, username: "elvis", email: 'elvis@itersdesktop.com', enabled: true, password:
            's3cr3t', accountExpired: false, accountLocked: false, passwordExpired: false)
        user.save()
    }

    void "test the service of creating a tag"() {
        given: "a name tag and creator's info"
        String username = "elvis"
        User user = User.findByUsername(username)
        String name = "Reproducible"

        when: "call creation service to create the tag"
        Tag tag = service.create(name, user)
        assert tag

        then: "the tag can be looked up"
        Tag.findByName(name)

        when: "call creation service to create another tag with the same property values"
        service.create(name, user)

        then: "the number of records is still 1"
        1 == Tag.count()
    }

    void "test the service of updating a tag"() {
        given: "a name tag and creator's info"
        String username = "elvis"
        User user = User.findByUsername(username)
        String name = "Reproducible"

        when: "call creation service to create the tag"
        Tag tag = service.create(name, user)
        assert tag

        and: "try to change tag name and date modified"
        String newName = "Annotated partially"
        Date dateModified = new Date()

        TagTransportCommand cmd = tag.toCommandObject()
        cmd.name = newName
        String dateFormat= "yyyy-MM-dd'T'HH:mm:ss"
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat)
        cmd.dateModified = sdf.format(dateModified)

        and: "call update service"
        Tag updated = service.update(cmd)

        then: "the tag will be updated successfully"
        updated.name == newName
        updated.id == tag.id
    }
}
