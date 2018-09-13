/**
 * Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core.model.identifier

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.core.ModelService
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.support.NullModelIdentifierGeneratorInitializer
import org.apache.tomcat.jdbc.pool.DataSource
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class ModelIdentifierGeneratorFactoryBeanSpec extends Specification {

    def "test injection of model identifier generator factory bean"() {
        defineBeans {
            // there's no dataSource bean defined in unit tests
            dataSource(DataSource) {
                driverClassName = "org.h2.Driver"
                url = "jdbc:h2:mem:testDb"
                username = "sa"
                password = ""
            }
            initializer(DummyModelIdentifierInitializer, 'MoDeL01') {
                it.scope = 'prototype'
            }
            nullModelIdGeneratorInitializer(NullModelIdentifierGeneratorInitializer) {
                it.scope = 'prototype'
            }

            def idSettings = new ConfigSlurper().parse('''
            part1 {
                type = "literal"
                suffix = "MoDeL"
            }
            part2 {
                type = 'numerical'
                fixed = 'false'
                width = '2'
            }''')

            sig(ModelIdentifierGeneratorFactoryBean, idSettings, "initializer", true) {
                it.scope = 'prototype'
            }
            pig(ModelIdentifierGeneratorFactoryBean) {
                it.scope = 'prototype'
            }
            modelService(ModelService)
            springConfig.addAlias('submissionIdGenerator', 'sig')
            springConfig.addAlias('publicationIdGenerator', 'pig')
        }

        when: 'we request id generator beans'
        def sig  = applicationContext.getBean('sig', ModelIdentifierGenerator.class)
        def sig2 = applicationContext.getBean('sig', ModelIdentifierGenerator.class)
        def pig  = applicationContext.getBean('pig', ModelIdentifierGenerator.class)
        def sigF = applicationContext.getBean('&sig', ModelIdentifierGeneratorFactoryBean.class)
        def pigF = applicationContext.getBean('&pig', ModelIdentifierGeneratorFactoryBean.class)
        ModelService modelService = applicationContext.modelService

        then: 'submission identifiers get generated correctly'
        sig.generate() == 'MoDeL02'

        and: 'there are no publication identifiers'
        !pig.generate()
        pig.class == NullModelIdentifierGenerator.class

        and: 'ModelIdentifierGeneratorBeans are prototype beans'
        sig2 != sig
        sig2 != pig
        sig  != pig

        and: "ModelIdentifierGeneratorFactorBeans' initializers are prototype-scoped"
        sigF.initializer != applicationContext.'&sig'.initializer
        pigF.initializer != applicationContext.'&pig'.initializer

        and: 'ModelService id generators are also prototype-scoped'
        // getSubmissionIdGenerator() and getPublicationIdGenerator() return new beans every time
        sig != modelService.submissionIdGenerator
        pig != modelService.publicationIdGenerator

        and: "the factory beans' shouldComputeRegex variable is initialised correctly"
        sigF.shouldComputeRegex
        !pigF.shouldComputeRegex
    }
}
