package net.biomodels.jummp.core

import org.codehaus.groovy.grails.plugins.support.BeanPostProcessorAdapter
import org.springframework.beans.BeansException

/**
 * Created by tnguyen on 26/05/16.
 */
class NosyBeanPostProcessor extends BeanPostProcessorAdapter {
    final List<String> targetNames = ['flowBuilderServices', 'expressionParser', 'webflowExpressionParser',
            'ehCacheManagementService']

    @Override
    Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (targetNames.contains(beanName)) {
            println "\t\there's $beanName --- $bean.properties"
        }
        bean
    }
}
