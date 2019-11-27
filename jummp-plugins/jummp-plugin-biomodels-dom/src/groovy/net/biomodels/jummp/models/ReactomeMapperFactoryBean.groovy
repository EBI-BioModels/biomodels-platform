package net.biomodels.jummp.models

import groovy.transform.CompileStatic
import org.springframework.beans.BeansException
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * @author carankalle on 26/11/2019.
 */

@CompileStatic
class ReactomeMapperFactoryBean implements FactoryBean<ReactomeMapper>, ApplicationContextAware {

    public ApplicationContext applicationContext

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext
    }

    ReactomeMapperFactoryBean() {
    }

    @Override
    ReactomeMapper getObject() throws Exception {
        return getReactomeMapperObject()
    }

    @Override
    Class<?> getObjectType() {
        return ReactomeMapper.class
    }

    @Override
    boolean isSingleton() {
        false // prototype beans
    }

    ReactomeMapper getReactomeMapperObject() throws BeansException {
        Objects.requireNonNull(applicationContext)
            .getBean("defaultReactomeMapper", ReactomeMapper.class)
    }
}
