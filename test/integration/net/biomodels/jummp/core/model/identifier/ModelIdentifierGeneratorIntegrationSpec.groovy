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
import net.biomodels.jummp.utils.redis.KeyCollection
import net.biomodels.jummp.utils.redis.RedisService
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext

class ModelIdentifierGeneratorIntegrationSpec extends IntegrationSpec {
    def grailsApplication
    GrailsApplicationContext ctx
    def redisService

    // date formats used by generators in this test
    final String y = new Date().format('yyyy')
    def ymd = new Date().format('yyMMdd')

    // The keys Redis holds for a generator type are shared by every generator of that type, so the generators of this
    // spec have types of their own: with the ones of the real generators ("submission", "publication") the seed values
    // below would replace the identifiers those generators, and the application under test, continue from.
    static final String SUBMISSION_TYPE  = "jbm776Submission"
    static final String PUBLICATION_TYPE = "jbm776Publication"
    static final String FOO_TYPE         = "jbm776Foo"
    static final String BAR_TYPE         = "jbm776Bar"

    /**
     * generate() continues from the identifier held in Redis under KeyCollection.getLastUsedIdValueKey(type), and from
     * the count under getLastUsedIdCountKey(type), where "0000" means the counter has just been reset.
     * getDefaultIdentifier() is no substitute here because the initializers below always supply a most recent
     * identifier. The tests work on a Redis database of their own (JBM-776), so seed both keys there, every time,
     * instead of relying on whatever another application or an earlier run left behind.
     */
    private static void seedLastUsedIdentifiers(final String year) {
        assert RedisService.REDIS_SRV_DATABASE != 0 :
            "Refusing to write model identifiers to Redis database 0, which an application may be using"
        [
            (SUBMISSION_TYPE) : ["MODEL1203044321", "4321"],
            (PUBLICATION_TYPE): ["PUBL1234554321", "1234554321"],
            (BAR_TYPE)        : ["YEARLY${year}123456".toString(), "123456"]
        ].each { type, lastUsed ->
            RedisService.doRedisSet(KeyCollection.getLastUsedIdValueKey(type), lastUsed[0])
            RedisService.doRedisSet(KeyCollection.getLastUsedIdCountKey(type), lastUsed[1])
        }
    }

    def setup() {
        seedLastUsedIdentifiers(y)
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
                'submissionIdInitializer', false, SUBMISSION_TYPE)
            publicationIdGenerator(ModelIdentifierGeneratorFactoryBean, pubCfg,
                'publicationIdInitializer', true, PUBLICATION_TYPE)
            fooIdGenerator(ModelIdentifierGeneratorFactoryBean, null, 'fooIdInitializer', false, FOO_TYPE)
            barIdGenerator(ModelIdentifierGeneratorFactoryBean, barCfg, 'barIdInitializer', true, BAR_TYPE)
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
        submissionFactory.generatorType  == SUBMISSION_TYPE
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
