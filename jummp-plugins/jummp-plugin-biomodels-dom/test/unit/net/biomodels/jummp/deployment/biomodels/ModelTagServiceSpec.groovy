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
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import spock.lang.Specification

@TestMixin(ServiceUnitTestMixin)
@Mock([Model, Revision, Person, User, Tag, ModelTag])
@TestFor(ModelTagService)
class ModelTagServiceSpec extends Specification {

    def setup() {
        /**
         * Create a person, a user, and a model format
         * Create two models and a few tags
         * Create a few associations between these models and tags
         */
        Person person = new Person(userRealName: "Elvis Nguyen")
        assert person.save()
        User user = new User(person: person, username: "elvis", email: 'elvis@itersdesktop.com', enabled: true, password:
            's3cr3t', accountExpired: false, accountLocked: false, passwordExpired: false)
        user.save()

        ModelFormat fmt = new ModelFormat(identifier: 'foo', name: 'foo', formatVersion: '1.0')
        fmt.save()

        Model m1 = new Model(vcsIdentifier: "aaa/123abc", name: "Model 1", submissionId: "M001")
        Model m2 = new Model(vcsIdentifier: "aaa/123def", name: "Model 2", submissionId: "M002")
        Model m3 = new Model(vcsIdentifier: "aaa/123def", name: "Model 2", submissionId: "M003")

        Revision r1 = new Revision(model: m1, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("elvis"),
            minorRevision: false, name:"sample model 1", description: "sample model 1",
            comment: "this is a sample model", uploadDate: new Date(),
            format: ModelFormat.findByIdentifierAndFormatVersion("foo", "1.0"))
        Revision r2 = new Revision(model: m2, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("elvis"),
            minorRevision: false, name:"sample model 2", description: "sample model 2",
            comment: "this is a sample model", uploadDate: new Date(),
            format: ModelFormat.findByIdentifierAndFormatVersion("foo", "1.0"))
        Revision r3 = new Revision(model: m2, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("elvis"),
            minorRevision: false, name:"sample model 3", description: "sample model 3",
            comment: "this is a sample model", uploadDate: new Date(),
            format: ModelFormat.findByIdentifierAndFormatVersion("foo", "1.0"))

        m1.revisions = [r1] as Set
        m2.revisions = [r2] as Set
        m3.revisions = [r3] as Set

        assert [m1,m2,m3]*.save(flush: true)

        // Create some tags
        Tag tag = new Tag()
        tag.name = "Annotated"
        tag.userCreated = User.findByUsername("elvis")
        tag.dateCreated = new Date()
        tag.dateModified = new Date()
        assert tag.save()

        tag = new Tag()
        tag.name = "Reproducible"
        tag.userCreated = User.findByUsername("elvis")
        tag.dateCreated = new Date()
        tag.dateModified = new Date()
        assert tag.save()

        tag = new Tag()
        tag.name = "Annotated Partially"
        tag.userCreated = User.findByUsername("elvis")
        tag.dateCreated = new Date()
        tag.dateModified = new Date()
        assert tag.save()

        assert 3 == Tag.count()

        // Map models and tags
        Tag tagAnnotated = Tag.findByName("Annotated")
        Tag tagAnnotatedPartially = Tag.findByName("Annotated Partially")
        Tag tagReproducible = Tag.findByName("Reproducible")

        ModelTag modelTag1 = new ModelTag(model: m1, tag: tagAnnotated)
        ModelTag modelTag2 = new ModelTag(model: m1, tag: tagReproducible)
        modelTag1.save()
        modelTag2.save()

        ModelTag modelTag3 = new ModelTag(model: m2, tag: tagAnnotatedPartially)
        ModelTag modelTag4 = new ModelTag(model: m2, tag: tagReproducible)
        modelTag3.save()
        modelTag4.save()

        assert ModelTag.count == 4
    }

    void "test the service of getting all tag names by model"() {
        given: "a model identifier"
        String modelId = "M001"

        when:
        List tags = service.getTagsByModelId("M001")
        then:
        tags.size() == 2

        when:
        tags = service.getTagsByModelId("M002")
        then:
        tags.contains("Annotated Partially") && tags.contains("Reproducible")
    }

    void "test the service of updating tags for a certain model"() {
        given:
        String modelId = "M001"
        User user = User.findByUsername("elvis")
        Set updatedTags = ["Sample Model", "Annotated", "Reproducible"] as Set

        when:
        Map result = service.update(updatedTags, modelId, user)
        then:
        result["status"] == 200
        result["message"].contains("Labels [${updatedTags.join(', ')}] applied successfully to the model")

        when:
        updatedTags = [] as Set
        result = service.update(updatedTags, modelId, user)
        then:
        result["status"] == 200
        result["message"] == "The model has no longer been tagged any label"

        when:
        modelId = "M003"
        updatedTags = [] as Set
        result = service.update(updatedTags, modelId, user)
        then:
        result["status"] == 422
        result["message"] == "Cannot save nothing for labels to the model"
    }
}
