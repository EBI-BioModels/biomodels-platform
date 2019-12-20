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
import spock.lang.Specification

/**
 * @author carankalle on 26/11/2019.
 * @author mglont on 05/12/2019.
 */
@TestMixin(GrailsUnitTestMixin)
class ReactomeServiceFactoryBeanSpec extends Specification {

    static doWithSpring = {
        reactomeService(ReactomeServiceFactoryBean)
    }

    def "test extraction of reactome pathway mapped to models"() {
        when: 'we request reactome mapper factory bean'
        def factoryBean = grailsApplication.mainContext.getBean("&reactomeService", ReactomeServiceFactoryBean)
        def rs1 = factoryBean.object

        then: 'reactome service prototype instances are correctly created'
        rs1.modelPathwayMapping == factoryBean.modelPathwayMap
        !rs1.modelPathwayMapping.isEmpty()

        when: "we get a reactomeService bean reference"
        def service1 = grailsApplication.mainContext.reactomeService
        def service2 = grailsApplication.mainContext.reactomeService

        then: "it is different from the previous instances"
        service1 != service2
    }
}
