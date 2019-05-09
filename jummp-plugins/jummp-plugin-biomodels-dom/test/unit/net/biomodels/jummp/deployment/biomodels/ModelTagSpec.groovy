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
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import spock.lang.Specification

@Mock([Model, Tag])
@TestFor(ModelTag)
class ModelTagSpec extends Specification {

    Person person
    User user
    Model model
    Tag tag

    def setup() {
        person = new Person(userRealName: 'Mary')
        user = new User(person: person, email: 'mary@example.com', enabled: true,
            password: 'changeme', accountExpired: false, accountLocked: false, passwordExpired: false)
        model = new Model(vcsIdentifier: "test", name: "test")
        tag = new Tag()
        tag.name = "Annotated"
        tag.userCreated = user
        tag.dateCreated = new Date()
        tag.dateModified = new Date()
    }

    void "test breaking constraints applied to ModelTag"() {
        given: "a ModelTag instance"
        ModelTag modelTag = new ModelTag(model: model)
        expect: "the object is invalid due to missing tag property"
        !modelTag.validate()
    }

    void "test saving ModelTag object into database"() {
        given: "a ModelTag instance"
        ModelTag modelTag = new ModelTag(model: model, tag: tag)
        when: "perform to persist the object into database"
        assert true == modelTag.validate()
        modelTag.save()
        then: "the object is saved successfully"
        ModelTag.count() == 1
    }
}
