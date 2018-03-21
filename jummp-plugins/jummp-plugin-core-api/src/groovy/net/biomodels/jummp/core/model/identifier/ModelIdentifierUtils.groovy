/**
 * Copyright (C) 2010-2017 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import net.biomodels.jummp.core.model.identifier.decorator.ChecksumAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.DateAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.FixedDigitAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.FixedLiteralAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.OrderedModelIdentifierDecorator
import net.biomodels.jummp.core.model.identifier.decorator.VariableDigitAppendingDecorator
import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.support.ChecksumModelIdentifierPartition
import net.biomodels.jummp.core.model.identifier.support.DateModelIdentifierPartition
import net.biomodels.jummp.core.model.identifier.support.LiteralModelIdentifierPartition
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierPartition
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierPartitionManager
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierPartitionRegexFactory
import net.biomodels.jummp.core.model.identifier.support.NumericalModelIdentifierPartition
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.tomcat.jdbc.pool.DataSource
import org.apache.tomcat.jdbc.pool.PoolProperties

import java.util.regex.Pattern

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
    // Regular expressions for each model identifier generator scheme (submission, publication, ...)
    static final Set<String> MODEL_ID_REGEXES = new LinkedHashSet<>()

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
    static final String GENERATOR_BEAN_SUFFIX = 'IdGenerator'
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
    /* stores the patterns that are used to generate a model identifier */
    static ConfigObject settings
    static TreeSet perennialFields

    /* hide constructor - all non-private methods are static. */
    protected ModelIdentifierUtils() {}

    static String simplifyDbConnStr(String dbConnStr) {
        int posUnicodeOptions = dbConnStr.indexOf(UNICODE_OPTIONS)
        if (posUnicodeOptions > 0) {
            dbConnStr = dbConnStr.substring(0, posUnicodeOptions-1) // take into account the ? symbol
        }
        dbConnStr
    }

    /** The starting point for wiring up model identifier generator beans. */
    static Map processGeneratorSettings(ConfigObject jummpConfig) {
        if (!jummpConfig || jummpConfig.isEmpty()) {
            def m ="""The settings for the model identification scheme are missing.
A sample configuration is
    jummp.model.id.submission.part1.type=literal
    jummp.model.id.submission.part1.suffix=MODEL
    jummp.model.id.submission.part2.type=date
    jummp.model.id.submission.part2.format=yyyyMMdd
    jummp.model.id.submission.part2.type=numerical
    jummp.model.id.submission.part2.fixed=false
    jummp.model.id.submission.part2.width=10"""
            log.error "No configuration settings provided - cannot build id generators."
            throw new Exception(m)
        }
        ConfigObject idSettings = jummpConfig.model.id
        settings = idSettings
        ConfigObject dbSettings = jummpConfig.database
        Map db = extractDatabaseSettings(dbSettings)
        boolean usingH2 = db['driver'] == DEFAULT_DRIVER
        GroovyRowResult mostRecentModelDetails
        Sql sql
        if (!usingH2) {
            // Due to GRAILS-10562 and GRAILS-10850, we cannot use
            // dS = Sql.newInstance(db.url, db.user, db.password, db.driver)
            // because of classloader issues, use Tomcat JDBC to create a dataSource
            PoolProperties pp = new PoolProperties()
            pp.url = db.url
            pp.driverClassName = db.driver
            pp.username = db.user
            pp.password = db.password
            DataSource ds = new DataSource()
            ds.poolProperties = pp
            sql = new Sql(ds)
            try {
                mostRecentModelDetails = sql.firstRow("""\
select model.id,
model.submission_id as submissionId,
model.perennialPublicationIdentifier as publicationId
from model
where id = (
    select model_id 
    from revision 
    where upload_date = (
        select max(upload_date) from revision where revision_number = 1
    )
    LIMIT 1
)
""")
                def lastPublished = sql.firstRow "select max(perennialPublicationIdentifier) as pId from model"
                if (lastPublished && mostRecentModelDetails) {
                    mostRecentModelDetails.publicationId = lastPublished.pId
                }
            } catch (Exception e) {
                final String W = """Unable to access the database - model IDs will be created \
using the default values."""
                log.warn (W, e)
            } finally {
                sql.close() // very important
            }
            if (IS_DEBUG_ENABLED) {
                log.debug "Most recent model in database is ${mostRecentModelDetails}"
            }
        }
        Map<String, ModelIdentifierGenerator> generatorBeans = [:]
        boolean submissionIdSettingsMissing = idSettings.submission.isEmpty()
        if (submissionIdSettingsMissing) {
            String e = """\
The configuration settings lack the rules for generating model identifiers!"""
            log.error e
            throw new Exception(e)
        }

        boolean shouldComputeRegexes = true
        def regexSetting = idSettings.remove("regex")
        if (regexSetting instanceof String) {
            if (!regexSetting?.trim()) {
                throw new IllegalArgumentException("Invalid configuration value for the model \
identifier regex. Remove it if you want it to be automatically generated from the settings.")
            }
            shouldComputeRegexes = false
            String modelIdentifierSchemePattern
            try {
                modelIdentifierSchemePattern = Pattern.compile regexSetting
            } catch (Exception ignore) {
                throw new IllegalArgumentException("w-t-f $regexSetting ?!!")
            }
            if (IS_DEBUG_ENABLED) {
                log.debug "Using model identifier regex $modelIdentifierSchemePattern"
            }
            MODEL_ID_REGEXES.add(modelIdentifierSchemePattern)
        }
        idSettings.each { name, cfg ->
            final String BEAN_NAME = "${name}${GENERATOR_BEAN_SUFFIX}"
            if (generatorBeans[BEAN_NAME]) {
                String err = "Duplicate settings for '$name' identifier."
                log.error(err)
                throw new Exception(err)
            }
            final String PROPERTY_NAME = "${name}Id"
            final String LAST_ID_FROM_THIS_GENERATOR = null
            if (mostRecentModelDetails?.containsKey(PROPERTY_NAME)) {
                LAST_ID_FROM_THIS_GENERATOR = mostRecentModelDetails[PROPERTY_NAME]
            }
            Set<OrderedModelIdentifierDecorator> decorators = buildDecoratorsFromSettings(
                    cfg, LAST_ID_FROM_THIS_GENERATOR, shouldComputeRegexes)
            ModelIdentifierGenerator generator = new DefaultModelIdentifierGenerator(decorators)
            generatorBeans[BEAN_NAME] = generator
        }
        perennialFields = idSettings.keySet()
        final String PUBLICATION_ID_BEAN_NAME = "publication$GENERATOR_BEAN_SUFFIX"
        boolean publicationIdBeanMissing = !generatorBeans[PUBLICATION_ID_BEAN_NAME]
        if (publicationIdBeanMissing) {
            generatorBeans[PUBLICATION_ID_BEAN_NAME] = new NullModelIdentifierGenerator()
        }
        if (IS_DEBUG_ENABLED) {
            String MSG = "Constructed the following objects: ${generatorBeans.inspect()}"
            log.debug MSG
        }
        return generatorBeans
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
    private static TreeSet<OrderedModelIdentifierDecorator> buildDecoratorsFromSettings(
                ConfigObject c, String mostRecentId = null, boolean shouldComputeRegexes = true) {
        ModelIdentifierPartitionManager partitionManager =
                    new ModelIdentifierPartitionManager(c, mostRecentId)
        TreeSet<OrderedModelIdentifierDecorator> decorators = new TreeSet()
        List<ModelIdentifierPartition> partitions = partitionManager.partitions
        if (IS_DEBUG_ENABLED) {
            log.debug "Turned decorator settings ${c.inspect()} into ${partitions.inspect()}"
        }
        StringBuilder regexForThisIdentifier = new StringBuilder()
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
                    if (shouldComputeRegexes)
                        partitionRegex = ModelIdentifierPartitionRegexFactory.forLiteral suffix
                    // this is a fixed decorator, so nextValue does not need updating
                    break
                case NumericalModelIdentifierPartition:
                    long suffix = Long.parseLong(p.value)
                    int width = p.width
                    if (p.fixed) {
                        d = new FixedDigitAppendingDecorator(i, suffix, width)
                    } else {
                        d = new VariableDigitAppendingDecorator(i, suffix, width)
                        // trigger decorator update
                        d.lastUsedSuffix.set(suffix)
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
            MODEL_ID_REGEXES.add regexForThisIdentifier.toString()
        }

        if (IS_DEBUG_ENABLED) {
            log.debug "Identifier settings ${c.inspect()} converted to ${decorators.inspect()} and regex $regexForThisIdentifier"
        }
        return decorators
    }
}
