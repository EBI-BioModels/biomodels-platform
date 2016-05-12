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





import grails.util.Environment
import grails.util.Holders
import java.util.concurrent.Executors
import net.biomodels.jummp.core.model.identifier.generator.AbstractModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGeneratorRegistryService
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.filter.AnnotationTypeFilter
import grails.persistence.Entity
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory


// Place your Spring DSL code here
beans = {
    xmlns aop: "http://www.springframework.org/schema/aop"
    def grailsApplication = Holders.grailsApplication

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

    executorService(grails.plugin.executor.PersistenceContextExecutorWrapper) { bean ->
        bean.destroyMethod = 'destroy' //keep this destroy method so it can try and clean up nicely
        persistenceInterceptor = ref("persistenceInterceptor")
        executor = Executors.newFixedThreadPool(grailsApplication.config.jummp.threadPool.size)
    }

    solrServerHolder(net.biomodels.jummp.search.SolrServerHolder) { bean ->
        bean.scope = "singleton"
        bean.autowire = "byName"
        bean.initMethod = "init"
        bean.destroyMethod = "destroy"
    }

    Map R = grailsApplication.config.jummp.id.generators
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
    grailsApplication.config.jummp.id.clear()

    //Add annotation store domain classes (defined externally) to the domain model
    //following: https://github.com/pongasoft/external-domain-classes-grails-plugin/blob/master/ExternalDomainClassesGrailsPlugin.groovy#L84
    def packages = ["net.biomodels.jummp.annotationstore", "net.biomodels.jummp.core.model", "net.biomodels.jummp.model", "net.biomodels.jummp.plugins.security"] as String[]
    BeanDefinitionRegistry simpleRegistry = new SimpleBeanDefinitionRegistry()
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(simpleRegistry, false)
    scanner.includeAnnotationConfig = false
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class))
    scanner.scan(packages)
    simpleRegistry?.beanDefinitionNames?.each { String beanName ->
        BeanDefinition bean = simpleRegistry.getBeanDefinition(beanName)
        String beanClassName = bean.beanClassName
        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE,
                                      Class.forName(beanClassName,
                                      true,
                                      Thread.currentThread().contextClassLoader))
    }

    importBeans('classpath:/metadatalib-spring-config.xml')
}
