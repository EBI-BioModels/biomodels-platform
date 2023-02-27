/**
* Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Environment
import net.biomodels.jummp.core.WebflowAclBeanDefinitionProcessor
import net.biomodels.jummp.core.model.identifier.ModelIdentifierGeneratorFactoryBean
import net.biomodels.jummp.core.model.identifier.ModelIdentifierGeneratorRegistryFactory
import net.biomodels.jummp.core.model.identifier.ModelIdentifierUtils
import net.biomodels.jummp.core.model.identifier.support.NullModelIdentifierGeneratorInitializer
import net.biomodels.jummp.core.model.identifier.support.PublicationIdGeneratorInitializer
import net.biomodels.jummp.core.model.identifier.support.SubmissionIdGeneratorInitializer
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.filter.AnnotationTypeFilter

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

// Place your Spring DSL code here
beans = {
    xmlns aop: "http://www.springframework.org/schema/aop"

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

    if (Environment.isDevelopmentMode()) {
        timingAspect(org.perf4j.log4j.aop.TimingAspect)
    }

    def searchStrategy = application.config.jummp.search.strategy
    println("INFO\tUsing $searchStrategy as current model search strategy")
    if (searchStrategy == "solr") {
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

    authenticationSuccessHandler(net.biomodels.jummp.plugins.security.BioModelsAuthSuccessHandler) {
        /* Reusing the security configuration */
        def conf = SpringSecurityUtils.securityConfig
        /* Configuring the bean */
        requestCache = ref('requestCache')
        redirectStrategy = ref('redirectStrategy')
        defaultTargetUrl = conf.successHandler.defaultTargetUrl
        alwaysUseDefaultTargetUrl = conf.successHandler.alwaysUseDefault
        targetUrlParameter = conf.successHandler.targetUrlParameter
        ajaxSuccessUrl = conf.successHandler.ajaxSuccessUrl
        useReferer = conf.successHandler.useReferer
    }

    //myBeanPostProcessor(net.biomodels.jummp.core.NosyBeanPostProcessor)

    // This section defines model identifier related beans.
    // Relevant docs:
    //      https://grails.github.io/grails2-doc/2.5.5/guide/spring.html
    //      https://grails.github.io/grails2-doc/2.5.5/api/grails/spring/BeanBuilder.html

    // generator beans need a corresponding initializer
    // using request scope helps us ensure that we always fetch the latest value from the db
    submissionIdGeneratorInitializer(SubmissionIdGeneratorInitializer) { bean ->
        dataSource = ref('dataSource')
        bean.scope = 'prototype'
    }
    publicationIdGeneratorInitializer(PublicationIdGeneratorInitializer) { bean ->
        dataSource = ref('dataSource')
        bean.scope = 'prototype'
    }
    /*
     * Use this for id generators that don't need to know about the values they generated before
     * by convention, idGenerator foo's initializer is called fooIdGeneratorInitializer.
     *
     * To only define the initializer and let JUMMP create the bean definition for a generator
     * put
     *      springConfig.addAlias('fooIdGeneratorInitializer', 'nullModelIdGeneratorInitializer')
     * or
     *      fooIdGeneratorInitializer(NullModelIdentifierGeneratorInitializer) {
     *          it.scope = 'prototype'
     *      }
     * in a plugin's doWithSpring() or any other place where a BeanBuilder is used.
     */
    nullModelIdGeneratorInitializer(NullModelIdentifierGeneratorInitializer) { bean ->
        bean.scope = 'prototype' // for consistency, use the same scope for all initializers
    }

    // turn identifier generator settings into corresponding bean definitions
    ConfigObject idGeneratorSettings = application.config.jummp.model.id
    if (idGeneratorSettings?.isEmpty() || !idGeneratorSettings?.isSet('submission'))
        throw new IllegalStateException('Submission identifier settings not found in the config file')

    def regexSetting = idGeneratorSettings.get('regex')
    boolean regexPresent = regexSetting instanceof String && !regexSetting.trim().isEmpty()
    String regex = regexSetting as String
    if (regexPresent) {
        try {
            Pattern.compile(regex)
        } catch (PatternSyntaxException ignore) {
            throw new IllegalArgumentException("'$regexSetting' is not a valid Java regex pattern.")
        }
    }

    // this is the only mandatory identifier generator, all others are optional
    submissionIdGenerator(ModelIdentifierGeneratorFactoryBean) { bean ->
        bean.scope  = 'prototype'
        idSettings  = idGeneratorSettings.get('submission')
        initializerBeanName = "submissionIdGeneratorInitializer"
        shouldComputeRegex  = !regexPresent
        generatorType       = 'submission'
    }

    Map<String, ConfigObject> optionalGeneratorBeanDefs = [:]
    idGeneratorSettings.each { String name, def /*ConfigObject or String*/ cfg ->
        String beanName = name + ModelIdentifierUtils.GENERATOR_BEAN_SUFFIX
        if (name != 'submission' && name != 'regex')
            optionalGeneratorBeanDefs.put(beanName, cfg)
    }
    // the publicationIdGenerator bean must exist, but will be NullModelIdentifierGenerator if
    // jummp.model.id.publication.* settings are not defined
    optionalGeneratorBeanDefs.putIfAbsent('publicationIdGenerator', null)

    def parentContext = ((GrailsApplicationContext) getParentCtx())
    optionalGeneratorBeanDefs.each { String name, ConfigObject c ->
        // don't touch bean definitions from BeanDefinitionRegistryPostProcessors, doWithSpring etc
        if (null == parentContext || !parentContext.containsBeanDefinition(name)) {
            String initializerBean = "${name}Initializer"
            "$name"(ModelIdentifierGeneratorFactoryBean) { bean ->
                bean.scope = 'prototype'
                idSettings = c
                // the initializer bean should exist, even if it's a NullModelIdGeneratorInitializer
                initializerBeanName = initializerBean
                shouldComputeRegex  = !regexPresent
                generatorType       = name - ModelIdentifierUtils.GENERATOR_BEAN_SUFFIX
            }
        }
    }

    Set<String> generatorTypes = idGeneratorSettings.keySet().findAll {
        it != 'regex'
    }

    idGeneratorRegistry(ModelIdentifierGeneratorRegistryFactory,
            ref('grailsApplication'), generatorTypes) { bean ->
        bean.scope = 'prototype'
        haveExplicitRegexSetting = regexPresent
        if (regexPresent) {
            explicitRegexValue = regex
        }
    }

    /*
     * Allows a singleton to use a prototype-scoped dependency without having to explicitly
     * invoke applicationContext.getBean(). Instead, inject the factory into the singleton:
     * <pre>
     *     def idGeneratorRegistryFactoryBean // the factory itself is a singleton
     *     ....
     *     void doSomethingWithTheRegistry() {
     *          ModelIdentifierGeneratorRegistryService registry =
     *                  idGeneratorRegistryFactoryBean.getObject()
*               // now do something with that registry ...
     *     }
     * </pre>
     * @see org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean
     * @see net.biomodels.jummp.core.ModelDelegateService
     */
    idGeneratorRegistryFactoryBean(ObjectFactoryCreatingFactoryBean) {
        targetBeanName = 'idGeneratorRegistry'
    }

    // end of id generator beans

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
        application.addArtefact(DomainClassArtefactHandler.TYPE,
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
