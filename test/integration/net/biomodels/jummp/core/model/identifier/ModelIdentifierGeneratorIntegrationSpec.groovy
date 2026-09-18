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

import grails.spring.BeanBuilder
import grails.test.runtime.DirtiesRuntime
import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.support.NullModelIdentifierGeneratorInitializer
import net.biomodels.jummp.utils.redis.RedisService
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext

class ModelIdentifierGeneratorIntegrationSpec extends IntegrationSpec {
    def grailsApplication
    GrailsApplicationContext ctx
    def redisService

    // date formats used by generators in this test
    final String y = new Date().format('yyyy')
    def ymd = new Date().format('yyMMdd')

    def setup() {
        ctx = grailsApplication.mainContext as GrailsApplicationContext

        // reset bean definitions and state relating to model id generation
        ["submissionIdGenerator", "publicationIdGenerator"].each { bean ->
            ctx.removeBeanDefinition(bean)
        }

        def subCfg = new ConfigSlurper().parse('''
            part1 {
                type   = 'literal'
                suffix = 'MODEL'
            }
            part2 {
                type   = 'date'
                format = 'yyMMdd'
            }
            part3 {
                type   = 'numerical'
                fixed  = 'false'
                suffix = '1'
                width  = '4'
            }
        ''')
        def pubCfg = new ConfigSlurper().parse('''
            part1 {
                type   = 'literal'
                suffix = 'PUBL'
            }
            part2 {
                type  = 'numerical'
                width = '10'
                fixed = 'false'
            }
        ''')
        def barCfg = new ConfigSlurper().parse('''
            part1 {
                type   = 'literal'
                suffix = 'YEARLY'
            }
            part2 {
                type   = 'date'
                format = 'yyyy'
            }
            part3 {
                type  = 'numerical'
                fixed = 'false'
                width = '6'
            }
        ''')

        BeanBuilder bb = new BeanBuilder()
        bb.beans {
            // declare id generator beans for the settings defined above
            submissionIdInitializer(DummyModelIdentifierInitializer, "MODEL1203044321")
            publicationIdInitializer(DummyModelIdentifierInitializer, "PUBL1234554321")
            fooIdInitializer(NullModelIdentifierGeneratorInitializer)
            barIdInitializer(DummyModelIdentifierInitializer, "YEARLY${y}123456")

            submissionIdGenerator(ModelIdentifierGeneratorFactoryBean, subCfg,
                'submissionIdInitializer', false, 'submission')
            publicationIdGenerator(ModelIdentifierGeneratorFactoryBean, pubCfg,
                'publicationIdInitializer', true, 'publication')
            fooIdGenerator(ModelIdentifierGeneratorFactoryBean, null, 'fooIdInitializer', false, 'foo')
            barIdGenerator(ModelIdentifierGeneratorFactoryBean, barCfg, 'barIdInitializer', true, 'bar')
        }
        bb.beanDefinitions.each { name, beanDef ->
            ctx.registerBeanDefinition(name, beanDef)
        }
    }

    def cleanup() {
    }

    @DirtiesRuntime
    void 'should cope with multiple types of id generators'() {
        given: 'the redis calls are bypassed without knock-on effects on model id generation'
        RedisService.metaClass.static.doRedisGet = { String key ->
            println "doRedisGet $key"
            key
        }
        when:
        def submissionIdGenerator  = ctx.submissionIdGenerator
        def publicationIdGenerator = ctx.publicationIdGenerator
        def fooIdGenerator         = ctx.fooIdGenerator
        def barIdGenerator         = ctx.barIdGenerator
        def submissionFactory      = ctx.'&submissionIdGenerator'
        def publicationFactory     = ctx.'&publicationIdGenerator'
        def fooFactory             = ctx.'&fooIdGenerator'
        def barFactory             = ctx.'&barIdGenerator'

        then:
        submissionIdGenerator.generate() == "MODEL${ymd}0001"
        !submissionIdGenerator.regex
        submissionFactory.generatorType  == 'submission'
        !submissionFactory.shouldComputeRegex

        publicationIdGenerator.generate() == "PUBL1234554322"
        publicationIdGenerator.regex == "\\QPUBL\\E\\d{10}?"
        publicationFactory.shouldComputeRegex

        fooIdGenerator.class == NullModelIdentifierGenerator.class
        !fooIdGenerator.generate()
        !fooIdGenerator.regex
        !fooFactory.shouldComputeRegex

        barIdGenerator.generate() == "YEARLY${y}123457"
        barIdGenerator.regex      == "\\QYEARLY\\E\\d{4}?\\d{6}?"
        barIdGenerator.class      == DefaultModelIdentifierGenerator.class
        barFactory.shouldComputeRegex
    }
}
