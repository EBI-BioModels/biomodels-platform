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
        ModelTagBootStrap.initialiseData()
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
        result["message"].contains("Labels [${updatedTags.join(', ')}] have been applied successfully to the model")

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
