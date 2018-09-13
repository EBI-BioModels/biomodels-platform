package net.biomodels.jummp.core.model.identifier

import grails.spring.BeanBuilder
import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.support.NullModelIdentifierGeneratorInitializer
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext

class ModelIdentifierGeneratorIntegrationSpec extends IntegrationSpec {
    def grailsApplication
    GrailsApplicationContext ctx

    // date formats used by generators in this test
    final String y = new Date().format('yyyy')
    def ymd = new Date().format('yyMMdd')

    def setup() {
        ctx = grailsApplication.mainContext as GrailsApplicationContext

        // reset bean definitions and state relating to model id generation
        ["submissionIdGenerator", "publicationIdGenerator"].each { bean ->
            ctx.removeBeanDefinition(bean)
        }
        ModelIdentifierUtils.perennialFields = null
        ModelIdentifierUtils.MODEL_ID_REGEXES.clear()

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
                'submissionIdInitializer', false)
            publicationIdGenerator(ModelIdentifierGeneratorFactoryBean, pubCfg,
                'publicationIdInitializer', true)
            fooIdGenerator(ModelIdentifierGeneratorFactoryBean, null, 'fooIdInitializer', false)
            barIdGenerator(ModelIdentifierGeneratorFactoryBean, barCfg, 'barIdInitializer', true)
        }
        bb.beanDefinitions.each { name, beanDef ->
            ctx.registerBeanDefinition(name, beanDef)
        }
    }

    def cleanup() {
    }

    void 'should cope with multiple types of id generators'() {
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
