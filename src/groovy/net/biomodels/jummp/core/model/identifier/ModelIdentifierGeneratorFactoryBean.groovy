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

import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator as DMIG
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.support.GeneratorDetails
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierGeneratorInitializer as MIGI
import net.biomodels.jummp.utils.redis.Operations
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * FactoryBean for creating model identifier generators of a given type.
 *
 * When Spring notices a bean definition with type FactoryBean it invokes its {@code getObject()}
 * method during instantiation, so that any other bean which depends on that bean definition will
 * have injected the object created by the factory, rather than the factory itself.
 *
 * Instances of this factory bean will parse the supplied id generator settings and use them to
 * construct an equivalent {@link ModelIdentifierGenerator}. Passing null idSettings in the
 * constructor will make this factory bean create a {@link NullModelIdentifierGenerator}. This can
 * be used when no new identifiers need to be generated when a model is published.
 *
 * A {@link MIGI} can be used to indicate the last used value for
 * identifiers of this type if consecutive identifiers are needed. A no-op
 * {@link net.biomodels.jummp.core.model.identifier.support.NullModelIdentifierGeneratorInitializer}
 * can be used when this is not required.
 *
 * A bean of this type is defined for every model id type (e.g. submission, publication, ...).
 * Model id generators have state, therefore they cannot be singletons, so they use
 * {@code prototype} scope instead.
 *
 * @see {@link org.springframework.beans.factory.FactoryBean}
 */
@CompileStatic
class ModelIdentifierGeneratorFactoryBean implements FactoryBean<ModelIdentifierGenerator>,
        ApplicationContextAware {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass().name)
    protected final String defaultInitializerBeanName = "nullModelIdGeneratorInitializer"
    ApplicationContext applicationContext
    String initializerBeanName
    boolean shouldComputeRegex

    /**
     * The configuration settings for this model identifier generator
     */
    ConfigObject idSettings

    /**
     * The type of model id generator produced by this factory bean instance (e.g. submission)
     */
    String generatorType

    @SuppressWarnings("GroovyUnusedDeclaration")
    ModelIdentifierGeneratorFactoryBean() {
        this(null, null, false, null)
    }

    ModelIdentifierGeneratorFactoryBean(ConfigObject config,
            String initializerBeanName,
            boolean computeRegex, String generatorType) {
        idSettings = config
        this.initializerBeanName = Optional.ofNullable(initializerBeanName)
            .orElse(defaultInitializerBeanName)
        shouldComputeRegex = computeRegex
        this.generatorType = generatorType
    }

    /**
     * Builds a {@code ModelIdentifierGenerator}.
     *
     * @return the initialised id generator for the given settings
     * @throws Exception If the initialiser bean was not defined or if there was a problem with
     *          the supplied idSettings (i.e. runtime configuration). These failures are normally
     *          critical and therefore fail application startup.
     * @see {@link ModelIdentifierGeneratorFactoryBean#getInitializer()}
     */
    @Override
    ModelIdentifierGenerator getObject() throws Exception {
        synchronized(this) {
            if (!idSettings || idSettings.isEmpty()) {
                return new NullModelIdentifierGenerator()
            }
            // get the last used value from the database
            String seed = Objects.requireNonNull(initializer).lastUsedValue
            String cachedSeed = Operations.doRedisGet(initializer.redisKeyForLastUsedValue)
            String type
            if (!cachedSeed && seed) {
                Operations.doRedisSet(initializer.redisKeyForLastUsedValue, seed)
                type = "from database"
            } else if (cachedSeed) {
                seed = cachedSeed
                type = "from redis cache"
            } else {
                seed = ""
                type = "from the initial value because the database is fresh"
            }
            LOGGER.debug("Seed: $seed --- type: $type")

            GeneratorDetails details = ModelIdentifierUtils.buildDecoratorsFromSettings(generatorType,
                idSettings, seed, shouldComputeRegex)

            new DMIG(details)
        }
    }

    @Override
    Class<?> getObjectType() {
        return ModelIdentifierGenerator.class
    }

    @Override
    boolean isSingleton() {
        false // prototype beans
    }

    /**
     * Retrieves the initializer bean associated with the generators created by this class.
     *
     * <p>This factory bean is implicitly defined as a singleton. There's one shared class instance
     * per bean definition (i.e. for every type of model identifier generator).</p>
     *
     * <p>The initializer is prototype-scoped, meaning every invocation of the bean should return a
     * new instance.</p>
     *
     * <p>This leads to the scoped bean injection problem: we'd like to ensure that every time we
     * get a ModelIdentifierGenerator created by this class we use a different initializer instance
     * which has the correct latest used value.</p>
     *
     * <p>Bean definitions of this class are instantiated only once, so the initializer
     * used by this class is never updated. {@code getInitializer()} solves the problem by telling
     * Spring to always fetch the initializer from the application context. We need to know the
     * name of the initializer bean name because normally several initializer beans are defined.</p>
     *
     * @return a (prototype-scoped) {@link MIGI}
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException
     *              if there is no such bean definition
     * @throws org.springframework.beans.factory.BeanNotOfRequiredTypeException
     *              if the bean is not of the required type
     * @throws org.springframework.beans.BeansException
     *              if the bean could not be created
     * @throws NullPointerException
     *              if the applicationContext is not defined
     */
    MIGI getInitializer() throws BeansException {
        Objects
            .requireNonNull(applicationContext)
            .getBean(initializerBeanName, MIGI.class)
    }
}
