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

import net.biomodels.jummp.core.model.identifier.decorator.ChecksumAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.DateAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.FixedDigitAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.FixedLiteralAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.OrderedModelIdentifierDecorator
import net.biomodels.jummp.core.model.identifier.decorator.VariableDigitAppendingDecorator
import net.biomodels.jummp.core.model.identifier.support.ChecksumModelIdentifierPartition
import net.biomodels.jummp.core.model.identifier.support.DateModelIdentifierPartition
import net.biomodels.jummp.core.model.identifier.support.GeneratorDetails
import net.biomodels.jummp.core.model.identifier.support.LiteralModelIdentifierPartition
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierPartition
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierPartitionManager
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierPartitionRegexFactory
import net.biomodels.jummp.core.model.identifier.support.NumericalModelIdentifierPartition
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Helper class containing methods for interacting with model id scheme settings.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class ModelIdentifierUtils {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphores for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    /*
     * The suffix to use in the bean reference corresponding to a generator.
     * For instance, for the settings
     *      jummp.model.id.foo.part1.type=numerical
     *      jummp.model.id.foo.part1.width=12
     *      jummp.model.id.foo.part1.suffix=1
     *      jummp.model.id.foo.part1.isFixed=false
     * there would be a corresponding fooIdGenerator bean reference that would
     * generate identifiers like 000000000001, 000000000002 etc.
     */
    static final String GENERATOR_FIELD_SUFFIX = 'Id'
    static final String GENERATOR_BEAN_SUFFIX  = 'IdGenerator'
    static final String DEFAULT_GENERATOR_TYPE = 'submission'
    static final String DEFAULT_GENERATOR_BEAN = 'submissionIdGenerator'
    static final String DEFAULT_URL =
                "jdbc:h2:tempDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
    static final String DEFAULT_USERNAME = "sa"
    static final String DEFAULT_PASSWORD = ""
    static final String DEFAULT_DIALECT = ""
    static final String DEFAULT_DRIVER = "org.h2.Driver"
    static final String DEFAULT_PROTOCOL = "h2"
    /**
     * MySQL uses latin-1 charset by default, so it's essential to enable Unicode
     * characters support mode alongside the mandatory properties of database connection string
     */
    static final String UNICODE_OPTIONS = "useUnicode=yes&characterEncoding=UTF-8"


    /* hide constructor - all non-private methods are static. */
    protected ModelIdentifierUtils() {}

    static String simplifyDbConnStr(String dbConnStr) {
        int posUnicodeOptions = dbConnStr.indexOf(UNICODE_OPTIONS)
        if (posUnicodeOptions > 0) {
            dbConnStr = dbConnStr.substring(0, posUnicodeOptions-1) // take into account the ? symbol
        }
        dbConnStr
    }

    /* Builds map of arguments to construct dataSource from the given configuration. */
    private static Map extractDatabaseSettings(ConfigObject dbSettings) {
        if (!dbSettings) {
            log.warn "No database settings defined - using the defaults."
        }
        String protocol
        String type = dbSettings.type
        String server, port, db, username, password, url, driver
        switch(type) {
            case "POSTGRESQL":
                protocol = 'postgresql'
                driver = 'org.postgresql.Driver'
                break
            case 'MYSQL':
                protocol ='mysql'
                driver = 'com.mysql.jdbc.Driver'
                break
            case 'h2':
            default:
                log.warn "Using H2 database $DEFAULT_URL"
                protocol = DEFAULT_PROTOCOL
                driver = DEFAULT_DRIVER
                url = DEFAULT_URL
                username = DEFAULT_USERNAME
                password = DEFAULT_PASSWORD
                break
        }
        if (protocol != "h2") {
            server = dbSettings.server
            port = dbSettings.port
            db = dbSettings.database
            username = dbSettings.username
            password = dbSettings.password
            url = "jdbc:$type://$server:$port/$db"
        }
        if (protocol == 'mysql') {
            url = "$url?$UNICODE_OPTIONS"
        }
        def out = [ driver: driver, url: url, user: username, password: password ]
        if (IS_DEBUG_ENABLED) {
            log.debug "Extracted the following database settings: $out"
        }
        out
    }

    /*
     * Returns an ordered set of decorator instances given the settings for a generator.
     * For settings of the form
     *      jummp.model.id.foo.part1.type=date
     *      jummp.model.id.foo.part1.format=yyMMdd
     * the ConfigObject this method expects is
     *      part1 {
     *          type = 'date'
     *          format = 'yyMMdd'
     *      }
     * If @p mostRecentId is specified, the returned decorators will use it to adjust their
     * initial values.
     */
    static GeneratorDetails buildDecoratorsFromSettings(
                ConfigObject c, String mostRecentId = null, boolean shouldComputeRegexes = true) {
        ModelIdentifierPartitionManager partitionManager =
                    new ModelIdentifierPartitionManager(c, mostRecentId)
        SortedSet<? extends OrderedModelIdentifierDecorator> decorators = new TreeSet<>()
        List<ModelIdentifierPartition> partitions = partitionManager.partitions
        if (IS_DEBUG_ENABLED) {
            log.debug "Turned decorator settings ${c.inspect()} into ${partitions.inspect()}"
        }
        StringBuilder regexForThisIdentifier = new StringBuilder()
        String regex = null
        partitions.eachWithIndex { p, i ->
            OrderedModelIdentifierDecorator d
            boolean validPartition = p.validate()
            if (!validPartition) {
                log.warn "ModelIdentifierPartition ${p.dump()} is not valid!"
                throw new Exception("Incorrect model identifier settings: ${p.properties}")
            }

            String partitionRegex
            switch(p) {
                case DateModelIdentifierPartition:
                    // this decorator sets nextValue to today's date, which is sensible
                    String format = p.format
                    d = new DateAppendingDecorator(i, format)
                    // don't lose the last value used by this decorator
                    d.nextValue.set(p.value)
                    d.setPartition(p)
                    if (!mostRecentId) {
                        d.setInitialValue(p.value)
                    }
                    if (shouldComputeRegexes)
                        partitionRegex = ModelIdentifierPartitionRegexFactory.forDatePartition format
                    break
                case ChecksumModelIdentifierPartition:
                    char sep = ChecksumAppendingDecorator.DEFAULT_SEPARATOR
                    d = new ChecksumModelIdentifierPartition(i, sep)
                    if (shouldComputeRegexes)
                        partitionRegex = ModelIdentifierPartitionRegexFactory.forChecksumPartition(sep)
                    //no need to update the value of the checksum
                    break
                case LiteralModelIdentifierPartition:
                    String suffix = p.value
                    d = new FixedLiteralAppendingDecorator(i, suffix)
                    d.setPartition(p)
                    if (shouldComputeRegexes)
                        partitionRegex = ModelIdentifierPartitionRegexFactory.forLiteral suffix
                    // this is a fixed decorator, so nextValue does not need updating
                    if (!mostRecentId) {
                        d.initialValue = p.value
                    }
                    break
                case NumericalModelIdentifierPartition:
                    long suffix = Long.parseLong(p.value)
                    int width = p.width
                    if (p.fixed) {
                        d = new FixedDigitAppendingDecorator(i, suffix, width)
                    } else {
                        d = new VariableDigitAppendingDecorator(i, suffix, width)
                    }
                    d.setPartition(p)
                    if (!mostRecentId) {
                        d.initialValue = p.value
                    }
                    if (shouldComputeRegexes)
                        partitionRegex = ModelIdentifierPartitionRegexFactory.forNumericalPartition width
                    break
                default:
                    partitionRegex = null
                    String M = "Unknown model identifier setting type $p"
                    log.error M
                    throw new Exception(M)
            }
            if (IS_DEBUG_ENABLED) {
                log.debug "Created ${d.dump()} based on partition ${p.dump()}"
            }
            decorators.add d
            if (shouldComputeRegexes)
                regexForThisIdentifier.append partitionRegex
        }
        boolean haveVariableDecorator = decorators.find{ it.isFixed() == false } != null
        if (!haveVariableDecorator) {
            log.error "All Decorators in ${decorators} are fixed!"
            def err = """The model identifier settings would yield duplicates. \
Consider introducing variable digit patterns or dates into the identifier scheme. For example
    jummp.model.id.submission.partN.type=numerical
    jummp.model.id.submission.partN.fixed=false
    jummp.model.id.submission.partN.width=10"""
            throw new Exception(err)
        }
        if (shouldComputeRegexes) {
            regex = regexForThisIdentifier.toString()
        }

        if (IS_DEBUG_ENABLED) {
            log.debug "Identifier settings ${c.inspect()} converted to ${decorators.inspect()} and regex $regexForThisIdentifier"
        }
        new GeneratorDetails(decorators: decorators, regex: regex)
    }
}
