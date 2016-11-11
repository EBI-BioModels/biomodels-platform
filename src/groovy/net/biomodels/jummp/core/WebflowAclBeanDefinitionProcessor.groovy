package net.biomodels.jummp.core

import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.config.RuntimeBeanReference
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.Ordered
// the acl plugin's bean class name
import org.springframework.expression.spel.standard.SpelExpressionParser
// the webflow plugin's bean class name
import org.springframework.webflow.expression.spel.WebFlowSpringELExpressionParser

/**
 * Custom BeanDefinitionRegistryPostProcessor to fix webflow and spring-security-acl integration.
 *
 * Once bean definitions are read from jummp and all plugins (resources.groovy, XML files,
 * doWithSpring), we reach the phase of the bean lifecycle where Spring knows what it should
 * create, but it hasn't instantiated any beans yet. The information is stored in a
 * BeanDefinitionRegistry, hence the name of the interface BeanDefinitionRegistryPostProcessor.
 *
 * At this stage, this bean kicks in [alongside others], and is free to make any modifications
 * it pleases: modify or remove existing bean definitions, or even add new ones.
 *
 * The crux of our problem is that both the acl plugin and the webflow plugin declare a bean
 * named 'expressionParser', but backed by different bean classes. The webflow plugin goes
 * further and uses the expressionParser bean from another bean called flowBuilderServices.
 *
 * Our fix is to simply alter webflow's definition so that the WebFlowSpringELExpressionParser
 * bean is named webflowExpressionParser rather than expressionParser. We also modify the
 * flowBuilderServices bean definition so that it uses the new bean definition.
 *
 * @author Mihai Glont
 * @date 20160519
 */
class WebflowAclBeanDefinitionProcessor implements Ordered, BeanDefinitionRegistryPostProcessor,
        ApplicationContextAware {
    /**
     * Dependency injection for the application context.
     */
    ApplicationContext applicationContext
    /**
     * It's advisable to have ordered postProcessors.
     */
    int order = Integer.MAX_VALUE - 1
    /**
     * Only run these changes if both the ACL and the webflow are present.
     * This flag is set by the init() method.
     */
    boolean isEnabled = false

    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        if (!isEnabled) {
            log.debug "Nothing to do"
            return
        } else {
            log.debug "Starting custom bean definition post-processor..."
        }
        log.debug "Registry has expressionParser bean defined: ${registry.containsBeanDefinition('expressionParser')}"
        if (registry.containsBeanDefinition("expressionParser")) {
            def defn = registry.getBeanDefinition "expressionParser"
            if (defn.beanClassName == WebFlowSpringELExpressionParser.class.name) {
                registry.removeBeanDefinition "expressionParser"
                registry.registerBeanDefinition "webflowExpressionParser", defn
                // just to be sure
                def newDefn = registry.getBeanDefinition "webflowExpressionParser"
                log.debug "Found webflowExpressionParser bean definition $newDefn"

                def clone = defn.cloneBeanDefinition()
                clone.beanClass = SpelExpressionParser.class
                clone.beanClassName = SpelExpressionParser.class.name
                clone.constructorArgumentValues = new ConstructorArgumentValues()
                registry.registerBeanDefinition "expressionParser", clone
                log.debug "Do this"
            } else {
                // expressionParser declared by Acl -- create webflowExpressionParser from scratch
                def webFlowExpressionParserDefinition = new GenericBeanDefinition()
                    webFlowExpressionParserDefinition.beanClass = WebFlowSpringELExpressionParser.class
                    webFlowExpressionParserDefinition.beanClassName =
                    WebFlowSpringELExpressionParser.class.name

                    // now have to set the properties of the bean
                    def ctorArgs = new ConstructorArgumentValues()
                    ctorArgs.addIndexedArgumentValue(0, new RuntimeBeanReference("sep"))
                    ctorArgs.addIndexedArgumentValue(1, new RuntimeBeanReference("conversationService"))
                    webFlowExpressionParserDefinition.constructorArgumentValues = ctorArgs

                    webFlowExpressionParserDefinition.setAutowireCandidate(true)

                    registry.registerBeanDefinition "webflowExpressionParser",
                    webFlowExpressionParserDefinition

                log.debug "webFlowEP: ${webFlowExpressionParserDefinition.properties}"
                log.debug "Do that"
            }
        } else {
            log.error "expected to find a bean called expressionParser..."
        }
        if (registry.containsBeanDefinition("flowBuilderServices")) {
            def defn = registry.getBeanDefinition "flowBuilderServices"
            def wepDefn = registry.getBeanDefinition "webflowExpressionParser"
            String wepBeanName = wepDefn.beanClassName
            log.debug "Processing flowBuilderServices bean definition: ${wepDefn} -- ${wepBeanName}"
            /*
             * in BeanBuilder DSL syntax this would be
             * flowBuilderServices(FlowBuilderServices) {
             *   expressionParser = ref('webflowExpressionParser')
             * }
             */
            defn.propertyValues.add("expressionParser", new RuntimeBeanReference("webflowExpressionParser"))
        }
        if (registry.containsBeanDefinition("expressionParser")) {
            def bd = registry.getBeanDefinition("expressionParser")
            log.debug "ExpressionParser Bean Definition: ${bd}"
        }
        if (registry.containsBeanDefinition("webflowExpressionParser")) {
            def bd = registry.getBeanDefinition("webflowExpressionParser")
            log.debug "webflowExpressionParser Bean Definition: ${bd}"
        }
        if (registry.containsBeanDefinition("flowBuilderServices")) {
            def bd = registry.getBeanDefinition "flowBuilderServices"
            log.debug "fBS Bean Definition: $bd"
        }
    }

    /**
     * We could use this method to further modify bean definitions, but we don't need to.
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        if (factory.containsBeanDefinition("flowBuilderServices")) {
            def bd = factory.getBeanDefinition("flowBuilderServices")
            log.debug "FlowBuilderServices: $bd"
            def vals = bd.propertyValues.getPropertyValueList()
            log.debug "Vals: ${vals.size()}"
            vals.eachWithIndex { it, i ->
                log.debug "$i -- $it.name : $it.value"
            }
        }
    }

    /**
     * Initialisation that needs to happen after this bean instance is constructed
     */
    void init() {
        GrailsPluginManager pluginManager =
                applicationContext.getBean("pluginManager") as GrailsPluginManager
        isEnabled = [true, true] == ["spring-security-core", "webflow"].collect {
            pluginManager.hasGrailsPlugin(it)
        }
        log.debug "Using custom bean definitions for SpelExpressionParser ${isEnabled ? 'yes' : 'no'}"
    }
}
