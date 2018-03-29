/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Grails (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails used as well as
* that of the covered work.}
**/


import grails.persistence.Entity
import grails.util.Environment
import grails.util.Holders
import net.biomodels.jummp.core.WebflowAclBeanDefinitionProcessor
import net.biomodels.jummp.core.model.identifier.generator.AbstractModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGeneratorRegistryService
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.filter.AnnotationTypeFilter

// Place your Spring DSL code here
beans = {
    xmlns aop: "http://www.springframework.org/schema/aop"
    def grailsApp = Holders.grailsApplication

    aop.config {
        // intercept all methods annotated with PostLogging annotation
        // and pass it to PostLoggingAdvice
        pointcut(id: "postLoggingPointcut", expression: "@annotation(net.biomodels.jummp.core.events.PostLogging)")
        advisor('pointcut-ref': "postLoggingPointcut", 'advice-ref': "postLogging")
    }
    postLogging(net.biomodels.jummp.core.events.PostLoggingAdvice)

    referenceTracker(net.biomodels.jummp.core.ReferenceTracker) { bean ->
        bean.autowire = "byName"
        bean.singleton = true
    }
    maintenanceMode(net.biomodels.jummp.core.MaintenanceBean) { bean ->
        bean.autowire = "byName"
        bean.singleton = true
    }
    modelFileFormatConfig(net.biomodels.jummp.core.ModelFileFormatConfig) { bean ->
        bean.autowire = "byName"
        bean.singleton = true
    }

    if (Environment.getCurrent() == Environment.DEVELOPMENT) {
        timingAspect(org.perf4j.log4j.aop.TimingAspect)
    }

    println("INFO\tUsing $grailsApp.config.jummp.search.strategy as current model search strategy")
    if (grailsApp.config.jummp.search.strategy == "solr") {
        solrServerHolder(net.biomodels.jummp.search.SolrServerHolder) { bean ->
            bean.scope = "singleton"
            bean.autowire = "byName"
            bean.initMethod = "init"
            bean.destroyMethod = "destroy"
        }
        solrBasedSearch(net.biomodels.jummp.search.SolrBasedSearch) { bean ->
            bean.scope = "singleton"
            bean.autowire = "byName"
            bean.singleton = true
            producerTemplate = ref("producerTemplate")
            solrServerHolder = ref("solrServerHolder")
            modelService = ref("modelService")
            springSecurityService = ref("springSecurityService")
            grailsApplication = ref("grailsApplication")
            configurationService = ref("configurationService")
            miriamService = ref("miriamService")
            aclUtilService = ref("aclUtilService")
        }
    } else {
        omicsdiBasedSearch(net.biomodels.jummp.search.OmicsdiBasedSearch) { bean ->
            bean.scope = "singleton"
            bean.autowire = "byName"
            bean.singleton = true
            producerTemplate = ref("producerTemplate")
            modelService = ref("modelService")
            springSecurityService = ref("springSecurityService")
            grailsApplication = ref("grailsApplication")
            configurationService = ref("configurationService")
            miriamService = ref("miriamService")
            aclUtilService = ref("aclUtilService")
            ebeyeWsConfig("uk.ac.ebi.ddi.ebe.ws.dao.config.EbeyeWsConfigDev")
        }
    }

    revisionCreatedListener(net.biomodels.jummp.plugins.bives.RevisionCreatedListener) { bean ->
        bean.autowire = "byName"
        bean.singleton = true
    }

    webflowAclBeanDefinitionProcessor(WebflowAclBeanDefinitionProcessor) {
        it.initMethod = "init"
    }

    //myBeanPostProcessor(net.biomodels.jummp.core.NosyBeanPostProcessor)

    Map R = grailsApp.config.jummp.id.generators
    identifierGeneratorRegistry(ModelIdentifierGeneratorRegistryService) {
        registry = R
    }

    R.each { name, generator ->
        def clazz = generator.getClass()
        if (generator instanceof AbstractModelIdentifierGenerator) {
            "$name"(clazz, generator.DECORATOR_REGISTRY)
        } else {
            "$name"(clazz)
        }
    }
    grailsApp.config.jummp.id.clear()

    //Add annotation store domain classes (defined externally) to the domain model
    //following: https://github.com/pongasoft/external-domain-classes-grails-plugin/blob/master/ExternalDomainClassesGrailsPlugin.groovy#L84
    BeanDefinitionRegistry simpleRegistry = new SimpleBeanDefinitionRegistry()
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(simpleRegistry, false)
    scanner.includeAnnotationConfig = false
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class))
    scanner.scan("net.biomodels.jummp")
    simpleRegistry?.beanDefinitionNames?.each { String beanName ->
        BeanDefinition bean = simpleRegistry.getBeanDefinition(beanName)
        String beanClassName = bean.beanClassName
        grailsApp.addArtefact(DomainClassArtefactHandler.TYPE,
                Class.forName(beanClassName, true, Thread.currentThread().contextClassLoader))
    }

    importBeans('classpath:/metadatalib-spring-config.xml')
    // override definition to use the one from the annotation-source-ddmore plugin
    springConfig.addAlias("metadataInfoService", "metadataInformationService")

    jf(com.fasterxml.jackson.core.JsonFactory)

    objectMapper(com.fasterxml.jackson.databind.ObjectMapper, jf) {
        visibility(com.fasterxml.jackson.annotation.PropertyAccessor.ALL, com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
        configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
