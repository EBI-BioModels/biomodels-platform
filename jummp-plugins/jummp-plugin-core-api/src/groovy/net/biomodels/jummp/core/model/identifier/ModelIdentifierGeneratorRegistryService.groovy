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
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGenerator
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication

import java.util.regex.Pattern

/**
 * Service for registering and storing model identifier generator types.
 *
 * @see ModelIdentifierGeneratorRegistryFactory
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@CompileStatic
class ModelIdentifierGeneratorRegistryService {

    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    static final String REGEX_SEPARATOR = '|'

    /**
     * The set of generator bean names.
     */
    Set<String> generatorNames = null

    /**
     * The types of model id generators present. These types (e.g. submissionId, publicationId),
     * directly mapped to eponymous fields in {@link net.biomodels.jummp.model.Model}, reflect the
     * types of identifier generators declared in the runtime configuration. At least a submissionId
     * is required.
     */
    Set<String> generatorTypes = null

    /**
     * The pattern that matches identifiers produced by the registered generators.
     */
    Set<String> regexSet = null

    /**
     * Whether the regex pattern was indicated in the externalised configuration.
     */
    boolean haveExplicitRegexSetting

    /**
     * The GrailsApplication reference.
     */
    GrailsApplication grailsApplication

    /**
     * Constructor to use when the regex setting is not explicitly stated.
     *
     * @param grailsApplication the grailsApplication bean reference
     * @param generatorNames the generator bean names.
     */
    ModelIdentifierGeneratorRegistryService(
        GrailsApplication grailsApplication,
        Set<String> generatorNames, Set<String> generatorTypes
    ) {
        if (null == generatorNames || !generatorNames.contains(
            ModelIdentifierUtils.DEFAULT_GENERATOR_BEAN))
            throw new IllegalStateException('SubmissionIdGenerator is mandatory but was not found')
        if (!grailsApplication)
            throw new IllegalStateException('''The grailsApplication reference is not initialised. \
If you instantiate this class outside Grails, please manually pass the grailsApplication reference.
''')
        this.grailsApplication   = grailsApplication
        this.generatorNames      = generatorNames
        this.generatorTypes      = generatorTypes
        haveExplicitRegexSetting = false
    }

    /**
     * Constructor to use when the regex setting is explicitly stated.
     *
     * @param grailsApplication the grailsApplication bean reference
     * @param generatorNames the generator bean names.
     * @param explicitRegex the model id pattern defined in the externalised configuration.
     */
    ModelIdentifierGeneratorRegistryService(
        GrailsApplication grailsApplication,
        Set<String> generatorNames, Set<String> generatorTypes, String explicitRegex
    ) {
        this(grailsApplication, generatorNames, generatorTypes)
        if (null == explicitRegex || explicitRegex.isEmpty())
            throw new IllegalArgumentException('''The model id regex cannot be empty or null. \
Use ModelIdentifierGeneratorRegistryService(GrailsApplication, Set<String>) instead if the pattern \
has not been set in the properties file.
''')
        this.regexSet            = [explicitRegex] as Set
        haveExplicitRegexSetting = true
    }

    /**
     * Returns an instance of the id generator with @p beanName, or null if nothing was found.
     */
    ModelIdentifierGenerator lookup(String beanName) {
        if (null == beanName || beanName.isEmpty() || !generatorNames.contains(beanName))
            return null

        ModelIdentifierGenerator result = grailsApplication.mainContext.getBean(beanName,
                ModelIdentifierGenerator.class)
        if (IS_DEBUG_ENABLED) {
            log.debug("Searched for a generator named $beanName and found ${result ?: 'nothing'}.")
        }
        return result
    }

    /**
     * Returns the model identifier pattern.
     *
     * If this was explicitly defined in the externalised configuration, this method will return
     * the corresponding {@link Pattern}. Otherwise, each registered model id generator is asked
     * to provide the regex for the identifiers it generates and the resulting set is OR'ed together
     *
     * @return the pattern that matches the identifiers created by all registered generators.
     */
    Pattern getRegexForAllModelIdentifiers() {
        Set<String> patterns
        if (haveExplicitRegexSetting) {
            patterns = regexSet
        } else {
            patterns = new LinkedHashSet<>(4, 1.0f)
            for (String name: generatorNames) {
                ModelIdentifierGenerator generator = lookup(name)
                String pattern = generator.regex
                if (null != pattern) {
                    patterns.add(pattern)
                }
            }
        }
        if (null == patterns || patterns.isEmpty()) {
            throw new IllegalStateException("""Identifier regex setting not specified and none of
the id generators '$generatorNames' have computed the pattern for the identifiers they create.""")
        }
        Pattern.compile(patterns.join(REGEX_SEPARATOR))
    }

    /**
     * Maps generator names to corresponding beans.
     *
     * Every method invocation will fetch a new instance of every registered generator from the
     * application context.
     *
     * @return a map with keys being bean names and values denoting instances of those beans.
     */
    Map<String, ? extends ModelIdentifierGenerator> getGeneratorMap() {
        Map<String, ? extends ModelIdentifierGenerator> result = [:]
        generatorNames.each { name ->
            result.put(name, lookup(name))
        }
        result
    }
}
