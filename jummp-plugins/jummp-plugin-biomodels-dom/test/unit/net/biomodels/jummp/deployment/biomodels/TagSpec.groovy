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
 **/

package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.Person
import spock.lang.Specification

@TestFor(Tag)
@Mock([User, Person])
class TagSpec extends Specification {

    User user
    Person person

    def setup() {
        person = new Person(userRealName: 'Mary')
        user = new User(person: person, email: 'mary@example.com', enabled: true,
            password: 'changeme', accountExpired: false, accountLocked: false, passwordExpired: false)
    }

    def "test breaking nullable constraints"() {
        given: "a plain object"
        Tag tag = new Tag()
        when: "perform to validate this object"
        boolean valid = tag.validate()
        then: "the validation cannot be passed"
        !valid
        when: "assign a tag name to that object, then validate it again"
        tag.name = "Annotated partially"
        valid = tag.validate()
        then: "the validation is still stuck"
        !valid
        when: "update the instance's userCreated and dateCreated property, then do validate the object again"
        tag.dateCreated = new Date()
        tag.userCreated = user
        then: "the validation should be passed"
        tag.validate()

    }
    void "test creating a tag successfully"() {
        given: "an initialised instance of Tag class"
        Tag tag = new Tag()
        tag.name = "Annotated"
        tag.userCreated = user
        tag.dateCreated = new Date()
        expect: "the instance passes the validation"
        tag.validate()
    }

    void "test saving a record into database"() {
        given: "an initialised instance of Tag class"
        Tag tag = new Tag()
        tag.name = "Annotated"
        tag.userCreated = user
        tag.dateCreated = new Date()
        when: "save the instance into database"
        tag.save(flush: true)
        then: "the instance is saved into database successfully"
        1 == Tag.count()
    }
}
