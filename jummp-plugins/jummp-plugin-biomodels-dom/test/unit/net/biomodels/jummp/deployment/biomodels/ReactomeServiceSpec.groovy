/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.models.ReactomeServiceFactoryBean

/**
 * @author carankalle on 26/11/2019.
 * @author mglont on 05/12/2019.
 */
@TestMixin(GrailsUnitTestMixin)
class ReactomeServiceSpec {
    private final String reactomeId = "R-HSA-1250196"
    private final String modelId = "BIOMD0000000255"


    def doWithSpring = {
        List<String> reactomeIds = new ArrayList<>()
        reactomeIds << reactomeId + "|" + "label"
        final Map<String, List<String>> reactomeMapping = [
            "BIOMD0000000255": reactomeIds
        ]
        reactomeService(DummyReactomeServiceFactoryBean, reactomeMapping) {
            it.scope = 'prototype'
        }
    }
    void "test getPathwayForModelId"() {

        when: "Asked for reactome Id for a known model"
        def service = grailsApplication.mainContext.reactomeService

        List<String> actualReactomeIds = service.getPathwaysForModelId(modelId)
        actualReactomeIds != null
        then: "reactomeId string shouldn't be empty"
        actualReactomeIds.contains(reactomeId);

        and: "asking for a pathway id for a model that does not exist should return null"
        service.getPathwaysForModelId("THIS-DOES-NOT-EXIST") == null
        service.getPathwaysForModelId("") == null
        service.getPathwaysForModelId(null) == null
    }
}

class DummyReactomeServiceFactoryBean extends ReactomeServiceFactoryBean {
    final Map<String, List<String>> mockModelPathwayMapping

    DummyReactomeServiceFactoryBean(Map<String, List<String>> mapping) {
        this.mockModelPathwayMapping = mapping
    }

    @Override
    ReactomeService getObject() throws Exception {
        new ReactomeService(mockModelPathwayMapping)
    }
}
