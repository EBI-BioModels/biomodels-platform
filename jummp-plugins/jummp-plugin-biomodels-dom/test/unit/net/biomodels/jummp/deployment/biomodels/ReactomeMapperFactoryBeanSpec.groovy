package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.models.DefaultReactomeMapper
import net.biomodels.jummp.models.ReactomeMapperFactoryBean
import spock.lang.Specification

/**
 * @author carankalle on 26/11/2019.
 */
@TestMixin(GrailsUnitTestMixin)
class ReactomeMapperFactoryBeanSpec extends Specification {

    def "test extraction of reactome pathway mapped to models" () {
        defineBeans {
            defaultReactomeMapper(DefaultReactomeMapper) { bean ->
                bean.scope = 'prototype'
            }
        }
        ReactomeMapperFactoryBean reactomeFactory = new ReactomeMapperFactoryBean()
        reactomeFactory.setApplicationContext(applicationContext)

        when: 'we request reactome mapper bean'
        def reactomeMapper = reactomeFactory.getObject()
        then: 'reactome data extracted correctly'
        reactomeMapper.getModelPathwayMap().size() > 0
    }
}
