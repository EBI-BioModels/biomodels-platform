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
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired

/**
 * FactoryBean for creating {@link ModelIdentifierGeneratorRegistryService} instances.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@CompileStatic
class ModelIdentifierGeneratorRegistryFactory implements
        FactoryBean<ModelIdentifierGeneratorRegistryService> {

    /**
     * The GrailsApplication reference
     */
    GrailsApplication application

    /**
     * The types of id generators that are defined in the runtime settings.
     * For instance submission or publication.
     */
    Set<String> types

    /**
     * Flag to indicate that the setting indicating the regex for all model identifiers is present.
     */
    boolean haveExplicitRegexSetting

    /**
     * The regex for all generated model identifiers.
     */
    String explicitRegexValue

    /**
     * Constructs an instance of this class.
     *
     * @param application the grailsApplication bean to use
     * @param types a non-empty set of types of model identifiers types
     */
    @Autowired
    ModelIdentifierGeneratorRegistryFactory(GrailsApplication application, Set<String> types) {
        this.application = application
        this.types = types
    }

    /**
     * Factory method for creating ModelIdentifierGeneratorRegistryService instances.
     *
     * @return a registry instance aware of the instance variables of this class
     * @throws Exception if there is not at least a submission id generator registered or if
     *          the grailsApplication bean is undefined.
     */
    @Override
    ModelIdentifierGeneratorRegistryService getObject() throws Exception {
        if (null == types || !types.contains(ModelIdentifierUtils.DEFAULT_GENERATOR_TYPE)) {
            throw new IllegalStateException("At least a submission identifier is required.")
        }
        if (haveExplicitRegexSetting &&
                (null == explicitRegexValue || explicitRegexValue.isEmpty())) {
            throw new IllegalStateException("The regexSetting has not been initialised.")
        }

        Set<String> generatorNames = new LinkedHashSet<>()
        for (String t : types) {
            if (null != t && !t.trim().isEmpty())
            generatorNames << "$t${ModelIdentifierUtils.GENERATOR_BEAN_SUFFIX}".toString()
        }

        def result
        if (haveExplicitRegexSetting) {
            result = new ModelIdentifierGeneratorRegistryService(application, generatorNames,
                explicitRegexValue)
        } else {
            result = new ModelIdentifierGeneratorRegistryService(application, generatorNames)
        }

        result
    }

    /**
     * Returns the class of objects created by {@link #getObject()}.
     *
     * @return {@linkplain ModelIdentifierGeneratorRegistryService}
     */
    @Override
    Class<?> getObjectType() {
        ModelIdentifierGeneratorRegistryService.class
    }

    /**
     * Returns false to indicate that ModelIdentifierGeneratorRegistryService is prototype-scoped.
     *
     * @return false
     */
    @Override
    boolean isSingleton() {
        return false
    }
}
