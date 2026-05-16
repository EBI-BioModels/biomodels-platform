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
**/


import grails.util.Environment
import net.biomodels.jummp.plugins.configuration.ConfigurationService
import org.apache.log4j.FileAppender
import org.apache.log4j.Level
import org.perf4j.log4j.AsyncCoalescingStatisticsAppender
import org.perf4j.log4j.GraphingStatisticsAppender
import java.lang.IllegalArgumentException as IAE
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.plugin.springsecurity.logout.postOnly = false
grails.plugin.springsecurity.password.algorithm = 'SHA-256'
grails.plugin.springsecurity.password.hash.iterations = 1
grails.plugin.springsecurity.useSessionFixationPrevention = false
grails.plugin.springsecurity.rejectIfNoRule = true
grails.plugin.springsecurity.fii.rejectPublicInvocations = false
grails.plugin.springsecurity.scr.disabled = true  // disable session creation for stateless paths
grails.plugin.springsecurity.sessionFixationPrevention.alwaysCreateSession = false

Properties jummpProperties = new Properties()
try {
    println "${new Date().format("yyyy-MM-dd HH:mm:ss")} ${this.getClass().name} LOADING THE EXTERNAL CONFIG FILE..."
	def service = new ConfigurationService()
    String pathToConfig = service.getConfigFilePath()
    if (pathToConfig) {
    	jummpProperties.load(new FileInputStream(pathToConfig))
    }
    else {
    	throw new Exception("No config file available, using defaults")
    }
} catch (Exception ignored) {
    jummpProperties.setProperty("jummp.security.ldap.enabled", "false")
    jummpProperties.setProperty("jummp.security.registration.email.send", "false")
    jummpProperties.setProperty("jummp.server.url", "http://localhost:8080/${appName}")
}
def jummpConfig = new ConfigSlurper().parse(jummpProperties)
List pluginsToExclude = []

cors.enabled=true
cors.url.pattern = '/api/*'
cors.headers=[
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Credentials': true,
    'Access-Control-Allow-Headers': 'origin, authorization, accept, content-type, x-requested-with',
    'Access-Control-Allow-Methods': 'GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS',
    'Access-Control-Max-Age': 3600
]

// The Accept header set by older versions of IE and Opera may be unreliable, ignore it
grails.mime.disable.accept.header.userAgents = ['Presto', 'Trident']
grails.mime.file.extensions = false // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]
//needed?
grails.web.disable.multipart = false

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// The default codec used to encode data with ${}
grails.views.default.codec = "html" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'
// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// whether to install the java.util.logging bridge for sl4j. Disable for AppEngine!
grails.logging.jul.usebridge = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'
// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false
// configure passing transaction's read-only attribute to Hibernate session, queries and criterias
// set "singleSession = false" OSIV mode in hibernate configuration after enabling
grails.hibernate.pass.readonly = false
// configure passing read-only to OSIV session by default, requires "singleSession = false" OSIV mode
grails.hibernate.osiv.readonly = false
grails.views.javascript.library="jquery"
// avoid ehcache duplicate CacheManager exception mess
beans {
    cacheManager {
        shared = true
    }
}

grails.cache.config.provider.name = "jummpCacheManager"
grails.cache.ehcache.cacheManagerName = "jummpCacheManager"

// set per-environment serverURL stem for creating absolute links
environments {
    production {
        grails.serverURL = jummpConfig.jummp.server.url
    }
    development {
        grails.serverURL = jummpConfig.jummp.server.url //"http://localhost:8080/${appName}"
    }
    test {
        grails.serverURL = "http://localhost:8080/${appName}"
    }
}

String catalinaBase = System.properties['catalina.base']
String defaultLogsDir = "$catalinaBase/logs"

if (Environment.isDevelopmentMode()) {
    defaultLogsDir = "${System.properties['user.dir']}/logs"
}

if (!(jummpConfig.jummp.logs.location instanceof ConfigObject)) {
    jummp.logs.location = jummpConfig.jummp.logs.location
} else {
    // default to solr
    jummp.logs.location = defaultLogsDir
}
String logsDir = jummp.logs.location
// enable/disable log security information based on DebugFilter class at the info level
// and add the implementation class name in Log4j configuration
environments {
   development {
      grails.logging.jul.usebridge = false
      grails.plugin.springsecurity.debug.useFilter = false
      // enable/disable web-based console plugin
      grails.plugin.console.enabled = true
   }
   production {
      grails.logging.jul.usebridge = false
      grails.plugin.console.enabled = true
   }
}

// database migrations
environments {
    development {
        grails.plugin.databasemigration.updateOnStart = false
        grails.plugin.databasemigration.updateOnStartFileNames = ['changelog.groovy']
        grails.plugin.databasemigration.changelogFileName = 'changelog.groovy'
    }
    production {
        grails.plugin.databasemigration.updateOnStart = false
        grails.plugin.databasemigration.updateOnStartFileNames = ['changelog.groovy']
        grails.plugin.databasemigration.changelogFileName = 'changelog.groovy'
    }
    test {
        /*
         * Due to GPDATABASEMIGRATION-160, migrations cannot be applied before
         * integration tests. The suggested workaround was to use
         *      grails.plugin.databasemigration.forceAutoMigrate = true
         * but that does not work in Grails 2.3.4. Hence, we set dbCreate to
         * create-drop in the test environment.
         */
        grails.plugin.databasemigration.updateOnStart = false
        grails.plugin.databasemigration.autoMigrateScripts = []
    }
}

environments {
    test {
        // need to disable the plugins or tests may fail
        // if needed in the tests, mockConfig should be used
        jummp.plugins.subversion.enabled = false
        jummp.plugins.git.enabled = false
        // disable registration mail sending
        jummp.security.registration.email.send = false
        jummp.security.resetPassword.email.send = false
    }
}

jummp.metadata.strategy = "biomodels" // "ddmore", "biomodels" or "default"
jummp.app.name=appName
//branding
// This property is used to select messages,
// and style if jummp.branding.style is not specified
jummp.branding.deployment = "biomodels" // "ddmore", "biomodels" or "default"
jummp.branding.style = "biomodels" // used to specify any other name for the css file
// log4j configuration
log4j.main = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
    appenders {
        // file appender that writes out the URLs of the Google Chart API graphs generated by the performanceGraphAppender
        def performanceGraphFileAppender = new FileAppender(
            fileName: "${logsDir}/perfGraphs.log",
            layout: pattern(conversionPattern: '%m%n')
        )
        appender name: 'performanceGraphFileAppender', performanceGraphFileAppender, additivity: false

        // this appender creates the Google Chart API graphs
        def performanceGraphAppender = new GraphingStatisticsAppender(
            graphType: 'Mean',      // possible options: Mean, Min, Max, StdDev, Count or TPS
            tagNamesToGraph: 'tag1,tag2,tag3',
            dataPointsPerGraph: 5
        )
        performanceGraphAppender.addAppender(performanceGraphFileAppender)
        appender name: 'performanceGraph', performanceGraphAppender


        // file appender that writes out the textual, aggregated performance stats generated by the performanceStatsAppender
        def performanceStatsFileAppender = new FileAppender(
            fileName: "${logsDir}/perfStats.log",
            layout: pattern(conversionPattern: '%m%n')  // alternatively use the StatisticsCsvLayout to generate CSV
        )
        appender name: 'performanceStatsFileAppender', performanceStatsFileAppender, additivity: false


        // this is the most important appender and first in the appender chain. it aggregates all profiling data withing a certain time frame.
        // the GraphingStatisticsAppender is attached as a child to this appender and uses its aggregated data.
        def performanceStatsAppender = new AsyncCoalescingStatisticsAppender(
            timeSlice: 30 * 60 * 1000    // 30 minutes in ms
        )
        performanceStatsAppender.addAppender(performanceStatsFileAppender)
        performanceStatsAppender.addAppender(performanceGraphAppender)
        appender name: 'performanceStatsAppender', performanceStatsAppender, additivity: false

        rollingFile name: "jummpAppender", file: "${logsDir}/jummp-core.log",
            threshold: Level.WARN, additivity: false
        rollingFile name: "eventsAppender",
            file: "${logsDir}/jummp-events.log",
            threshold: Level.DEBUG, additivity: false

        // change the threshold to DEBUG to have debug output in development mode
        console name: "stdout", threshold: Level.WARN, additivity: false

        rollingFile name: "stacktrace", maxFileSize: 1024,
                    file: "${logsDir}/stacktrace.log", additivity: false
    }

    // configure the performanceStatsAppender to log at INFO level
    info   performanceStatsAppender: 'org.perf4j.TimingLogger', additivity: false
    error  jummpAppender: [
        'org.codehaus.groovy.grails.web.servlet',  //  controllers
        'org.codehaus.groovy.grails.web.pages', //  GSP
        'org.codehaus.groovy.grails.web.sitemesh', //  layouts
        'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
        'org.codehaus.groovy.grails.web.mapping', // URL mapping
        'org.codehaus.groovy.grails.commons', // core / classloading
        'org.codehaus.groovy.grails.plugins', // plugins
        'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
        'org.springframework',
        'org.hibernate',
        'net.biomodels.jummp.plugins.configuration'
    ], additivity: false

    warn   jummpAppender: 'org.mortbay.log'
    // Simple Logging goes to its own file
    info   eventsAppender: [
        'net.biomodels.jummp.plugins.simplelogging',
        'net.biomodels.jummp.core.events',
        'net.biomodels.jummp.plugins.bives',
        'net.biomodels.jummp.webapp'
    ], additivity: false

    rollingFile name: "debugAppender", file: "${logsDir}/jummp-debug.log",
        threshold: Level.DEBUG, additivity: false
    rollingFile name: "irreproducibleAppender", file: "${logsDir}/jummp-irreproducible.log",
        threshold: Level.INFO, additivity: false
    rollingFile name: "hibernateAppender", file: "${logsDir}/jummp-hibernate.log",
        threshold: Level.WARN, additivity: false
    rollingFile name: "cronJobAppender", file: "${logsDir}/jummp-cronjob.log",
        threshold: Level.DEBUG, additivity: false
    rollingFile name: "apiFilterAppender", file: "${logsDir}/jummp-api-filter.log",
            threshold: Level.INFO, additivity: false
    rollingFile name: "searchAppender", file: "${logsDir}/jummp-search.log",
            threshold: Level.DEBUG, additivity: false
    debug debugAppender: [
        'net.biomodels.jummp',
        'net.biomodels.jummp.core',
        'net.biomodels.jummp.model',
        'net.biomodels.jummp.core.model',
        'net.biomodels.jummp.core.model.identifier',
        'net.biomodels.jummp.core.model.identifier.decorator',
        'net.biomodels.jummp.core.model.identifier.generator',
        'net.biomodels.jummp.core.model.identifier.support',
        'net.biomodels.jummp.core.events',
        'net.biomodels.jummp.core.subscribers',
        'net.biomodels.jummp.deployment.biomodels',
        'net.biomodels.jummp.plugins.pharmml',
        'net.biomodels.jummp.plugins.configuration',
        'net.biomodels.jummp.scms',
        'net.biomodels.jummp.security',
        'net.biomodels.jummp.utils.redis',
        'net.biomodels.jummp.webapp',
        'grails.app.conf.BootStrap'
        /*"grails.plugin.springsecurity",
        "org.springframework.security",
        "org.pac4j"*/
    ], additivity: false

    debug searchAppender: [
        'net.biomodels.jummp.core.SearchService',
        'net.biomodels.jummp.webapp.SearchController',
        'net.biomodels.jummp.search'
    ], additivity: false

    debug irreproducibleAppender: [
        'net.biomodels.jummp.core.adapters.RevisionAdapter',
        'net.biomodels.jummp.core.ModelDelegateService'

    ], additivity: false

    debug cronJobAppender: [
        'net.biomodels.jummp.core.AuthenticationOutdaterJob',
        'net.biomodels.jummp.core.GarbageCollectionBasedCleanerJob',
        'net.biomodels.jummp.core.HomePageUpdaterJob',
        'net.biomodels.jummp.core.MiriamRegistryExportUpdaterJob',
        'net.biomodels.jummp.core.ModelFileFormatConfig',
        'net.biomodels.jummp.core.OldFilesExchangeCleanerJob',
        'net.biomodels.jummp.core.RegistrationCheckerJob',
        'net.biomodels.jummp.core.SearchIndexOptimiserJob',
        'net.biomodels.jummp.core.UpdateCachedParametersOnRedisJob',
        'net.biomodels.jummp.core.RefreshModelIdentifiersOnRedisJob',
    ], additivity: false

    warn hibernateAppender: [
        'org.codehaus.groovy.grails.orm.hibernate',
        'org.codehaus.groovy.grails.orm.support',
        'org.hibernate.SQL',
        'org.springframework.orm.hibernate4.support'

    ], additivity: false

    info apiFilterAppender: [
        'PreProcessFilters', 'RequestLoggingFilters'
    ]
}


String healthCheckIpRestrictions
if (jummpConfig.jummp.healthcheck.ipRestrictions instanceof String) {
    healthCheckIpRestrictions = jummpConfig.jummp.healthcheck.ipRestrictions
} else {
    healthCheckIpRestrictions = "127.0.0.1"
}
println "INFO\tThe health check endpoint will only be available from '$healthCheckIpRestrictions'"

// IPv4 IP addresses and ranges allowed to access specific URLs
// requests from localhost are always allowed:
// http://grails-plugins.github.io/grails-spring-security-core/2.0.x/guide/ip.html
grails.plugin.springsecurity.ipRestrictions = [
//    '/healthCheck/**': healthCheckIpRestrictions
]

jummp.controllerAnnotations = [
    // /model/create and /model/create?execution=e.*s1 show the display the submission guidelines, which should be visible without logging in
    '/model/create': ["request.getParameter('execution') == null ? permitAll : (request.getParameter('execution').matches('^e.*?s1\$') ? permitAll: fullyAuthenticated)"],

    // request.getParameter('execution')) == null ? matches('^e.*?s1\$') ? permitAll: fullyAuthenticated
    "/":                        ["permitAll"],
    "/index":                   ["permitAll"],
    '/index.gsp':               ['permitAll'],
    // protect the spring security ui plugin controllers
    '/aclclass/**':             ['ROLE_ADMIN'],
    '/aclentry/**':             ['ROLE_ADMIN'],
    '/aclobjectidentity/**':    ['ROLE_ADMIN'],
    '/aclsid/**':               ['ROLE_ADMIN'],
    '/persistentlogin/**':      ['ROLE_ADMIN'],
    '/register/**':             ['ROLE_ADMIN'],
    '/registrationcode/**':     ['ROLE_ADMIN'],
    '/requestmap/**':           ['ROLE_ADMIN'],
    '/role/**':                 ['ROLE_ADMIN'],
    '/securityinfo/**':         ['ROLE_ADMIN'],
    '/user':                    ['isAuthenticated()'],
    '/user/**':                 ['ROLE_ADMIN'],
    "/css/**":                  ["permitAll"],
    "/images/**":               ["permitAll"],
    "/js/**":                   ["permitAll"],
    "/plugins/jquery*/**":      ["permitAll"],
    "/plugins/navigation*/**":  ["permitAll"],
    "/plugins/blueprint*/**":   ["permitAll"],
    "/api-docs/**":             ["permitAll"],
    "/console/**":              ["ROLE_ADMIN"],
    "/plugins/console*/**":     ['ROLE_ADMIN'],
    "/plugins/*/js/*":          ['permitAll'],
    "/plugins/*/css/*":         ['permitAll'],
    "/plugins/*/images/*":      ['permitAll'],
    "/simpleCaptcha/captcha":   ['permitAll'],
    "/docs/**":                 ['permitAll'],
    '/**/favicon.ico':          ['permitAll'],
    "/omicsdi/**":              ["hasRole('ROLE_ADMIN')"]
]

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'net.biomodels.jummp.plugins.security.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'net.biomodels.jummp.plugins.security.UserRole'
grails.plugin.springsecurity.authority.className = 'net.biomodels.jummp.plugins.security.Role'
grails.plugin.springsecurity.securityConfigType = "Annotation" // "Annotation", "InterceptUrlMap", "Requestmap"
grails.plugin.springsecurity.successHandler.alwaysUseDefaultTargetUrl = false
grails.plugin.springsecurity.successHandler.defaultTargetUrl = "/"

grails.plugin.springsecurity.controllerAnnotations.staticRules = jummp.controllerAnnotations

// login
grails.plugin.springsecurity.rest.login.active=true
grails.plugin.springsecurity.rest.login.endpointUrl="/api/login"
grails.plugin.springsecurity.rest.login.failureStatusCode=401
grails.plugin.springsecurity.rest.login.useJsonCredentials=true
grails.plugin.springsecurity.rest.login.usernamePropertyName='username'
grails.plugin.springsecurity.rest.login.passwordPropertyName='password'
// logout
grails.plugin.springsecurity.rest.logout.endpointUrl='/api/logout'

// token generation
grails.plugin.springsecurity.rest.token.generation.useSecureRandom=true
grails.plugin.springsecurity.rest.token.generation.useUUID=false

// token rendering
grails.plugin.springsecurity.rest.token.rendering.usernamePropertyName='username'
grails.plugin.springsecurity.rest.token.rendering.authoritiesPropertyName='roles'
grails.plugin.springsecurity.rest.token.rendering.tokenPropertyName='token'

grails.plugin.springsecurity.rest.token.storage.useGorm = true
grails.plugin.springsecurity.rest.token.storage.gorm.tokenDomainClassName = 'net.biomodels.jummp.plugins.security.AuthToken'
grails.plugin.springsecurity.rest.token.storage.gorm.tokenValuePropertyName = "token"
grails.plugin.springsecurity.rest.token.storage.gorm.usernamePropertyName = 'username'
grails.plugin.springsecurity.rest.token.validation.headerName = 'X-Auth-Token'

// Traditional chain

grails.plugin.springsecurity.rest.token.validation.enableAnonymousAccess = true
grails.plugin.springsecurity.filterChain.chainMap = [
    '/api/guest/**': 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor',
    // '/api/**': 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor',
    '/api/**/**': 'JOINED_FILTERS,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-rememberMeAuthenticationFilter',  // Stateless chain
    '/**': 'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter',                                                                         // Traditional chain
    '/css/**': 'none',
    '/images/**': 'none',
    '/js/**': 'none',
    '/fonts/**': 'none'
]
grails.plugin.springsecurity.useSecurityEventListener = true

// ldap
boolean ldapEnabled = Boolean.parseBoolean(jummpConfig.jummp.security.ldap.enabled as String)
if ((jummpConfig.jummp.security.ldap.enabled instanceof ConfigObject) || !ldapEnabled) {
    jummp.security.ldap.enabled = false
    println("INFO\tExcluding ldap")
    pluginsToExclude << "springSecurityLdap"
} else {
    println("INFO\tUsing ldap")
    jummp.security.ldap.enabled = true
    /*grails.plugin.springsecurity.ldap.context.managerDn       = jummpConfig.jummp.security.ldap.managerDn
    grails.plugin.springsecurity.ldap.context.managerPassword   = jummpConfig.jummp.security.ldap.managerPw
    grails.plugin.springsecurity.ldap.context.server            = jummpConfig.jummp.security.ldap.server
    grails.plugin.springsecurity.ldap.search.base               = jummpConfig.jummp.security.ldap.search.base
    grails.plugin.springsecurity.ldap.authorities.searchSubtree = jummpConfig.jummp.security.ldap.search.subTree
    grails.plugin.springsecurity.ldap.search.filter             = jummpConfig.jummp.security.ldap.search.filter*/

    grails.plugin.springsecurity.ldap.context.managerDn         = ''
    grails.plugin.springsecurity.ldap.context.managerPassword   = ''
    grails.plugin.springsecurity.ldap.context.server              = 'ldaps://ldap.ebi.ac.uk'
    grails.plugin.springsecurity.ldap.search.base                 = 'ou=people,dc=ebi,dc=ac,dc=uk'
    grails.plugin.springsecurity.ldap.authorities.searchSubtree   = true
    grails.plugin.springsecurity.ldap.authorities.groupSearchBase = 'ou=groups,dc=ebi,dc=ac,dc=uk'
    grails.plugin.springsecurity.ldap.search.filter               = '(uid={0})'
    grails.plugin.springsecurity.ldap.context.anonymousReadOnly   = true
    // static options
    grails.plugin.springsecurity.ldap.authorities.ignorePartialResultException = true
    grails.plugin.springsecurity.ldap.authorities.retrieveGroupRoles = true
    grails.plugin.springsecurity.ldap.authorities.retrieveDatabaseRoles = true
    grails.plugin.springsecurity.providerNames = [
        'ldapAuthProvider',
        'anonymousAuthenticationProvider',
        'rememberMeAuthenticationProvider',
        'twoFactorAuthenticationProvider',
    ]
}

// version control backend
if (jummpConfig.jummp.vcs.exchangeDirectory) {
    jummp.vcs.exchangeDirectory = jummpConfig.jummp.vcs.exchangeDirectory
}
if (jummpConfig.jummp.vcs.workingDirectory) {
    jummp.vcs.workingDirectory = jummpConfig.jummp.vcs.workingDirectory
}
// tmp directory for storing the data temporarily processed. The directory will be cleaned periodically.
if (jummpConfig.jummp.dirs.tmp) {
    jummp.dirs.tmp = jummpConfig.jummp.dirs.tmp
} else {
    jummp.dirs.tmp = System.getProperty("java.io.tmpdir")
}
// upload directory for storing any files uploaded to the server
if (jummpConfig.jummp.dirs.upload) {
    jummp.dirs.upload = jummpConfig.jummp.dirs.upload
} else {
    Path tmp = Paths.get(System.getProperty("java.io.tmpdir"))
    Path upload = Paths.get(tmp.toString(), "upload")
    jummp.dirs.upload = upload.toString()
}
if (jummpConfig.jummp.model.cache.dir) {
    jummp.model.cache.dir = jummpConfig.jummp.model.cache.dir
} else {
    throw new IAE("""\
Please add the setting 'jummp.model.cache.dir', pointing to a directory where model files are cached, \
to your configuration.""")
}
// search config
// model search strategy setting: "omicsdi" or "solr"
if (!(jummpConfig.jummp.search.strategy instanceof ConfigObject)) {
    jummp.search.strategy = jummpConfig.jummp.search.strategy
} else {
    // default to solr
    jummp.search.strategy = "solr"
}

if (jummpConfig.jummp.model.curators.mailinglist) {
    jummp.model.curators.mailinglist = jummpConfig.jummp.model.curators.mailinglist
}

if (jummpConfig.jummp.cache.dir) {
    jummp.cache.dir = jummpConfig.jummp.cache.dir
}

if (jummpConfig.jummp.classification.endpoint) {
    jummp.classification.endpoint = jummpConfig.jummp.classification.endpoint
}

if (jummp.search.strategy == "solr") {
    if (!(jummpConfig.jummp.search.url instanceof ConfigObject)) {
        final Pattern URL_PATTERN = ~/http:\/\/[a-zA-Z0-9\.\-_]+(:[0-9]+)?(\/[a-zA-Z0-9\-\._]+)*/
        final String solrSetting = jummpConfig.jummp.search.url
        final String solrUrl
        if (solrSetting?.endsWith("/")) {
            solrUrl = solrSetting.substring(0, solrSetting.length() - 1)
        } else {
            solrUrl = solrSetting
        }
        if (!solrUrl || !(solrUrl ==~ URL_PATTERN)) {
            throw new IAE("""The URL for the search server ($solrUrl) does not look right. \
Check the value of setting 'jummp.search.url'.""")
        } else {
            jummp.search.url = solrUrl
            println "INFO\tUsing $solrUrl as the URL of the search server."
        }
    } else {
        throw new IAE("""\
Please add the setting 'jummp.search.url', pointing to a Solr instance, to your configuration.""")
    }
    if (!(jummpConfig.jummp.search.folder instanceof ConfigObject)) {
        final String searchFolder = jummpConfig.jummp.search.folder
        jummp.search.folder = searchFolder
        println "INFO\tSOLR_HOME is set to $searchFolder."
    } else {
        println "WARN\tSetting jummp.search.folder is undefined. Have you set \$SOLR_HOME?"
    }
} else {
    // folder containing the exported OmicsDI entries
    if (!(jummpConfig.jummp.search.exportFolder instanceof ConfigObject)) {
        jummp.search.exportFolder = jummpConfig.jummp.search.exportFolder
    }
}

// FTP Data Mover Service
if (!(jummpConfig.jummp.revision.ftpdatamover.srv instanceof ConfigObject)) {
    jummp.revision.ftpdatamover.srv = jummpConfig.jummp.revision.ftpdatamover.srv
} else {
    jummp.revision.ftpdatamover.srv = "http://0.0.0.0:8000"
}

// folder containing the exports
if (!(jummpConfig.jummp.model.exportFolder instanceof ConfigObject)) {
    jummp.model.exportFolder = jummpConfig.jummp.model.exportFolder
}
else {
    jummp.model.exportFolder = jummp.search.exportFolder
}
// external conversion service url
if (!(jummpConfig.jummp.model.converter.url instanceof ConfigObject)) {
    jummp.model.converter.url = jummpConfig.jummp.model.converter.url
}
else {
    println "ERROR\tSetting jummp.model.converter.url is undefined. The conversion of the model will be failed."
}

if (!(jummpConfig.jummp.search.pathToIndexerExecutable instanceof ConfigObject)) {
    jummp.search.pathToIndexerExecutable = jummpConfig.jummp.search.pathToIndexerExecutable
}
else {
	println "WARN\tSetting jummp.search.pathToIndexerExecutable is undefined. Models will not be indexed correctly in the search engine."
}

// registration settings
boolean emailSend = Boolean.parseBoolean(jummpConfig.jummp.security.registration.email.send as String)
if (!(jummpConfig.jummp.security.registration.email.send instanceof ConfigObject) && emailSend) {
    jummp.security.registration.email.send         = emailSend
    jummp.security.registration.email.sender       = jummpConfig.jummp.security.registration.email.sender
    if (!(jummpConfig.jummp.security.registration.email.sendToAdmin instanceof ConfigObject)) {
        String sendToAdmin = jummpConfig.jummp.security.registration.email.sendToAdmin as String
        jummp.security.registration.email.sendToAdmin = Boolean.parseBoolean(sendToAdmin)
    } else {
        jummp.security.registration.email.sendToAdmin = false
    }
    jummp.security.registration.email.adminAddress = jummpConfig.jummp.security.registration.email.adminAddress
    jummp.security.registration.email.contact      = jummpConfig.jummp.security.registration.email.contact
    jummp.security.registration.email.noreply      = jummpConfig.jummp.security.registration.email.noreply
    jummp.security.registration.email.subject      = jummpConfig.jummp.security.registration.email.subject
    jummp.security.registration.email.body         = jummpConfig.jummp.security.registration.email.body
    jummp.security.registration.verificationURL    = jummpConfig.jummp.security.registration.verificationURL
    jummp.security.activation.email.subject        = jummpConfig.jummp.security.activation.email.subject
    jummp.security.activation.email.body           = jummpConfig.jummp.security.activation.email.body
    jummp.security.activation.activationURL        = jummpConfig.jummp.security.activation.activationURL
    jummp.security.resetPassword.email.body        = jummpConfig.jummp.security.resetPassword.email.body
    jummp.security.resetPassword.email.subject     = jummpConfig.jummp.security.resetPassword.email.subject
} else {
    jummp.security.registration.email.send = false
}

// whether a user has curator rights by default, allowing them to publish models
// they have access to.
if (!(jummpConfig.jummp.security.curatorByDefault instanceof ConfigObject)) {
    jummp.security.curatorByDefault = Boolean.parseBoolean(jummpConfig.jummp.security.curatorByDefault as String)
} else {
    // default to true
    jummp.security.curatorByDefault = true
}

if (!(jummpConfig.jummp.security.certificationRole instanceof ConfigObject)) {
    jummp.security.certificationRole = jummpConfig.jummp.security.certificationRole
} else {
    jummp.security.certificationRole = 'ROLE_ADMIN' // either single value or comma-separated string
}

if (!(jummpConfig.jummp.feedback.receiver.roles instanceof ConfigObject)) {
    jummp.feedback.receiver.roles = jummpConfig.jummp.feedback.receiver.roles
} else {
    jummp.feedback.receiver.roles = ["ROLE_ADMIN","ROLE_CURATOR"]
}

if (!(jummpConfig.jummp.security.certificationAllowed instanceof ConfigObject)) {
    jummp.security.certificationAllowed = Boolean.parseBoolean(jummpConfig.jummp.security.certificationAllowed as String)
} else {
    // default to false
    jummp.security.certificationAllowed = false
}

// whether sbml validation is turned on
if (!(jummpConfig.jummp.plugins.sbml.validation instanceof ConfigObject)) {
	jummp.plugins.sbml.validation = Boolean.parseBoolean(jummpConfig.jummp.plugins.sbml.validation as String)
}

// file preview size, in bytes
if (!(jummpConfig.jummp.web.file.preview instanceof ConfigObject)) {
	jummp.web.file.preview = Integer.parseInt(jummpConfig.jummp.web.file.preview as String)
}
else {
	jummp.web.file.preview = 500 * 1024 * 1024 // default preview size: 500 MB
}

// whether a user is allowed to change the password depends on the setting an if LDAP is used
// in case of LDAP changing the password is not (yet) possible in the application
if (!(jummpConfig.jummp.security.ui.changePassword instanceof ConfigObject)) {
    jummp.security.ui.changePassword = Boolean.parseBoolean(jummpConfig.jummp.security.ui.changePassword as String)
} else {
    // default to true
    jummp.security.ui.changePassword = true
}
if (jummp.security.ldap.enabled) {
    // as long as our LDAP implementation does not support changing passwords we need to disable
    jummp.security.ui.changePassword = false
}

// In case of LDAP there is no need to allow users to register with a password as we cannot (yet) add anything to the LDAP
jummp.security.registration.ui.userPassword = !jummp.security.ldap.enabled

// whether users are allowed to register themselves or not.
// if not only an administrator can create a new user account
// default to users can register themselves
if (!(jummpConfig.jummp.security.anonymousRegistration instanceof ConfigObject)) {
    String anonymousRegistration = jummpConfig.jummp.security.anonymousRegistration
    jummp.security.anonymousRegistration = Boolean.parseBoolean(anonymousRegistration)
} else {
    jummp.security.anonymousRegistration = true
}

// For the job, removing authentication hashes that are unused for a configurable time
// Used by AuthenticationHashService
if (!(jummpConfig.jummp.authenticationHash.startRemoveOffset instanceof ConfigObject)) {
    String sto = jummpConfig.jummp.authenticationHash.startRemoveOffset
    jummp.authenticationHash.startRemoveOffset = Long.parseLong(sto)
} else {
    jummp.authenticationHash.startRemoveOffset = 5*60*1000
}
if (!(jummpConfig.jummp.authenticationHash.removeInterval instanceof ConfigObject)) {
    String interval = jummpConfig.jummp.authenticationHash.removeInterval
    jummp.authenticationHash.removeInterval = Long.parseLong(interval)
} else {
    jummp.authenticationHash.removeInterval = 30*60*1000
}
if (!(jummpConfig.jummp.authenticationHash.maxInactiveTime instanceof ConfigObject)) {
    jummp.authenticationHash.maxInactiveTime = jummpConfig.jummp.authenticationHash.maxInactiveTime
} else {
    jummp.authenticationHash.maxInactiveTime = 30*60*1000
}

if (!(jummpConfig.jummp.threadPool.size instanceof ConfigObject)) {
    jummp.threadPool.size = jummpConfig.jummp.threadPool.size as Integer
} else {
    jummp.threadPool.size = 10
}

if (!(jummpConfig.model.history.maxElements instanceof ConfigObject)) {
    jummp.model.history.maxElements = jummpConfig.model.history.maxElements as Integer
} else {
    jummp.model.history.maxElements = 10
}

// For the appearance of the web front-end defines the color for internal usage
if (!(jummpConfig.jummp.branding.internalColor instanceof ConfigObject)) {
    jummp.branding.internalColor = jummpConfig.jummp.branding.internalColor
}
// For the appearance of the web front-end defines the color for external usage
if (!(jummpConfig.jummp.branding.externalColor instanceof ConfigObject)) {
    jummp.branding.externalColor = jummpConfig.jummp.branding.externalColor
}
// The type of the database server
if (!(jummpConfig.jummp.database.type instanceof ConfigObject)) {
    jummp.database.type = jummpConfig.jummp.database.type
}
// The location of the database server
if (!(jummpConfig.jummp.database.server instanceof ConfigObject)) {
    jummp.database.server = jummpConfig.jummp.database.server
}
// The port of the database server
if (!(jummpConfig.jummp.database.port instanceof ConfigObject)) {
    jummp.database.port = jummpConfig.jummp.database.port as Integer
}
// The name of the database
if (!(jummpConfig.jummp.database.database instanceof ConfigObject)) {
    jummp.database.database = jummpConfig.jummp.database.database
}
// The user of the database server
if (!(jummpConfig.jummp.database.username instanceof ConfigObject)) {
    jummp.database.username = jummpConfig.jummp.database.username
}
// The user's password of the database server
if (!(jummpConfig.jummp.database.password instanceof ConfigObject)) {
    jummp.database.password = jummpConfig.jummp.database.password
}

boolean firstRun = Boolean.parseBoolean(jummpConfig.jummp.firstRun as String)
if (jummpConfig.jummp.firstRun instanceof ConfigObject || !firstRun) {
    // only add side protection if not in first run mode
    boolean protection = Boolean.parseBoolean(jummpConfig.jummp.server.protection as String)
    if (!(jummpConfig.jummp.server.protection instanceof ConfigObject) && protection) {
        jummp.controllerAnnotations.put("/login/**", ['IS_AUTHENTICATED_ANONYMOUSLY'])
        jummp.controllerAnnotations.put("/**", ['ROLE_USER'])
    }
}

if (!"jms".equalsIgnoreCase(System.getenv("JUMMP_EXPORT"))) {
    jms.disabled = true
}

if (pluginsToExclude) {
    grails.plugin.excludes = pluginsToExclude
}

grails.mail.props=[:]
if (!(jummpConfig.jummp.security.mailer.host instanceof ConfigObject)) {
	grails.mail.host=jummpConfig.jummp.security.mailer.host
}
if (!(jummpConfig.jummp.security.mailer.username instanceof ConfigObject)) {
	grails.mail.username=jummpConfig.jummp.security.mailer.username
}
if (!(jummpConfig.jummp.security.mailer.password instanceof ConfigObject)) {
	grails.mail.password=jummpConfig.jummp.security.mailer.password
}
if (!(jummpConfig.jummp.security.mailer.port instanceof ConfigObject)) {
	grails.mail.port=jummpConfig.jummp.security.mailer.port
}
if (!(jummpConfig.jummp.security.mailer.auth instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.auth"]=jummpConfig.jummp.security.mailer.auth
}
if (!(jummpConfig.jummp.security.mailer.ssl instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.ssl.enable"]=jummpConfig.jummp.security.mailer.ssl
	grails.mail.props["mail.smtp.ssl.protocols"]="TLSv1.2"
}
if (!(jummpConfig.jummp.security.mailer.socketFactory instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.ssl.enable"]="true"
	grails.mail.props["mail.smtp.ssl.protocols"]="TLSv1.2"
	grails.mail.props["mail.smtp.socketFactory.class"]=jummpConfig.jummp.security.mailer.socketFactory
	grails.mail.props["mail.smtp.socketFactory.port"]=grails.mail.port
}
if (!(jummpConfig.jummp.security.mailer.fallback instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.socketFactory.fallback"]=jummpConfig.jummp.security.mailer.fallback
}
if (!(jummpConfig.jummp.security.mailer.tlsrequired instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.starttls.enable"]=jummpConfig.jummp.security.mailer.tlsrequired
	grails.mail.props["mail.smtp.starttls.required"]=jummpConfig.jummp.security.mailer.tlsrequired
}
grails.mail.props["mail.smtp.connectiontimeout"] = "30000"
grails.mail.props["mail.smtp.timeout"]           = "30000"
grails.mail.props["mail.smtp.writetimeout"]      = "30000"
if (!(jummpConfig.jummp.security.mailer.smtp2goApiKey instanceof ConfigObject)) {
	jummp.security.mailer.smtp2goApiKey = jummpConfig.jummp.security.mailer.smtp2goApiKey
}
if (!(jummpConfig.jummp.security.mailer.brevoApiKey instanceof ConfigObject)) {
	jummp.security.mailer.brevoApiKey = jummpConfig.jummp.security.mailer.brevoApiKey
}

ConfigObject modelIdentifierSettings = jummpConfig.jummp.model.id
if (!modelIdentifierSettings) {
    throw new Exception("""\
The settings for generating model identifiers are missing. For model identifiers
of the form MODEL0001, MODEL0002, MODEL0003 please use the following settings:
\tjummp.model.id.submission.part1.type=literal
\tjummp.model.id.submission.part1.suffix=MODEL
\tjummp.model.id.submission.part2.type=numerical
\tjummp.model.id.submission.part2.width=4
""")
}
jummp.ddmore.rdfstore.url = jummpConfig.jummp?.ddmore?.rdfstore?.url
jummp.model.id = [:]
modelIdentifierSettings?.entrySet()?.each {
    jummp.model.id."${it.key}" = it.value
}

// Uncomment and edit the following lines to start using Grails encoding & escaping improvements


// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'none' // escapes values inside null
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'raw' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType.'text/html' = 'html'
    }
}

if (!(jummpConfig.jummp.context.help.root instanceof ConfigObject)) {
    def pages = ["root", "browse", "search", "login", "display", "archives", "submission", "update", "profile",
                "sharing", "teams", "notifications", "annotate", "getting-started-with-biomodels"]
    pages.each {
        if (!(jummpConfig.jummp.context.help."${it}" instanceof ConfigObject)) {
            jummp.context.help."${it}" = jummpConfig.jummp.context.help."${it}"
        }
    }
}
jummp.config.maintenance = false

if (!(jummpConfig.jummp.metadata.officialDatabaseName instanceof ConfigObject)) {
    jummp.metadata.officialDatabaseName = jummpConfig.jummp.metadata.officialDatabaseName
} else {
    jummp.metadata.officialDatabaseName = 'BioModels Database'
}

if (!(jummpConfig.jummp.metadata.officialDatabaseDescription instanceof ConfigObject)) {
    jummp.metadata.officialDatabaseDescription = jummpConfig.jummp.metadata.officialDatabaseDescription
} else {
    jummp.metadata.officialDatabaseDescription = """\
        BioModels Database is a repository of computational models of biological processes.
        Models described from literature are manually curated and enriched with cross-references.
        """
}

def dateFormats = ["yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", 'MMddyyyy', 'yyyy-MM-dd HH:mm:ss.S', "yyyy-MM-dd'T'hh:mm:ss'Z'" ]
grails.databinding.dateFormats = dateFormats

/**
 * BELOW ARE SETTINGS FOR REDIS SERVER
 */
// TODO: rewrite the validation to Redis properties. If there is any mismatch, throw an exception
// because this setting is crucial to start the application properly
if (!(jummpConfig.jummp.redis.host instanceof ConfigObject)) {
    jummp.redis.host = jummpConfig.jummp.redis.host
} else {
    jummp.redis.host = "localhost"
}

if (!(jummpConfig.jummp.redis.port instanceof ConfigObject)) {
    jummp.redis.port = jummpConfig.jummp.redis.port as int
} else {
    jummp.redis.port = 6379 // the default port
}

if (!(jummpConfig.jummp.redis.timeout instanceof ConfigObject)) {
    jummp.redis.timeout = jummpConfig.jummp.redis.timeout as int
} else {
    jummp.redis.timeout = 3600 // the default timeout
}

/**
 * SPRING SESSION CONFIGURATION
 * Notes: reuse Redis Server properties above
 */
// common properties
if (!(jummpConfig.jummp.springsession.maxInactiveIntervalInSeconds instanceof ConfigObject)) {
    long interval = jummpConfig.jummp.springsession.maxInactiveIntervalInSeconds as long
    jummp.springsession.maxInactiveIntervalInSeconds = interval
} else {
    jummp.springsession.maxInactiveIntervalInSeconds = 3600
}
springsession.maxInactiveIntervalInSeconds = jummp.springsession.maxInactiveIntervalInSeconds // Session timeout. default is 1800 seconds

// Redis store specific properties
springsession.redis.connectionFactory.hostName = jummp.redis.host
springsession.redis.connectionFactory.port = jummp.redis.port       // Redis server connection timeout
springsession.redis.connectionFactory.timeout = jummp.redis.timeout
// This is crucial to make sure flash messages to be displayed
// See: https://github.com/jeetmp3/spring-session/issues/5
springsession.allow.persist.mutable = true

// HTTP PROXY (used for k8s deployment)
if (!(jummpConfig.jummp.http.proxy.host instanceof ConfigObject)) {
    jummp.http.proxy.host = jummpConfig.jummp.http.proxy.host
} else {
    jummp.http.proxy.host = "localhost"
}
if (!(jummpConfig.jummp.http.proxy.port instanceof ConfigObject)) {
    jummp.http.proxy.port = jummpConfig.jummp.http.proxy.port as int
} else {
    jummp.http.proxy.port = 80
}

// HOME PAGE CONFIGURATION
if (!(jummpConfig.biomodels.homepage.recently.accessed.models.maxRecords instanceof ConfigObject)) {
    int maxRecords = jummpConfig.biomodels.homepage.recently.accessed.models.maxRecords as int
    biomodels.homepage.recently.accessed.models.maxRecords = maxRecords
} else {
    biomodels.homepage.recently.accessed.models.maxRecords = 7
}

if (!(jummpConfig.biomodels.homepage.recently.published.models.maxRecords instanceof ConfigObject)) {
    int maxRecords = jummpConfig.biomodels.homepage.recently.published.models.maxRecords as int
    biomodels.homepage.recently.published.models.maxRecords = maxRecords
} else {
    biomodels.homepage.recently.published.models.maxRecords = 7
}

// cors config
cors.enabled = true
cors.url.pattern = '/*'
cors.headers=[
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Credentials': true,
    'Access-Control-Allow-Headers': 'origin, authorization, accept, content-type, x-requested-with',
    'Access-Control-Allow-Methods': 'GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS',
    'Access-Control-Max-Age': 3600
]

if (!(jummpConfig.ws.client.japi.docs instanceof ConfigObject)) {
    jummp.ws.client.japi.docs = jummpConfig.jummp.ws.client.japi.docs
} else {
    jummp.ws.client.japi.docs = "https://bitbucket.org/biomodels/biomodelswsclient/src/master/"
}

if (!(jummpConfig.ws.client.pyapi.docs instanceof ConfigObject)) {
    jummp.ws.client.pyapi.docs = jummpConfig.jummp.ws.client.pyapi.docs
} else {
    jummp.ws.client.pyapi.docs = "https://bitbucket.org/biomodels/biomodels-resftful-api-client/src/main/"
}

if (!(jummpConfig.jummp.model.download.server instanceof ConfigObject)) {
    jummp.model.download.server = jummpConfig.jummp.model.download.server
} else {
    // it is set to the default server
    jummp.model.download.server = "http://127.0.0.1:7000"
}

if (!(jummpConfig.jummp.model.fileservice.server instanceof ConfigObject)) {
    jummp.model.fileservice.server = jummpConfig.jummp.model.fileservice.server
} else {
    // it is set to the default server
    jummp.model.fileservice.server = "http://127.0.0.1:8090"
}


if (!(jummpConfig.jummp.model.ftp.location instanceof ConfigObject)) {
    jummp.model.ftp.location = jummpConfig.jummp.model.ftp.location
} else {
    jummp.model.ftp.location = ""
}

brutforce {
    loginAttempts {
        time = 5
        allowedNumberOfAttempts = 3
    }
}

// When true, every user is required to complete OTP verification at login,
// regardless of whether they have individually enrolled in 2FA.
// If this property is missing from the config file, the 2FA is disabled by default.
boolean twoFaEnforced = Boolean.parseBoolean(jummpConfig.jummp.security.twofa.enforced as String)
if (!(jummpConfig.jummp.security.twofa.enforced instanceof ConfigObject)) {
    jummp.security.twofa.enforced = twoFaEnforced
} else {
    jummp.security.twofa.enforced = false
}

// When true, unenrolled users see an amber notice banner encouraging them to set up
// 2FA before it becomes mandatory. Use during the grace period before enforcement.
boolean twoFaEnrollmentNotice = Boolean.parseBoolean(jummpConfig.jummp.security.twofa.enrollmentNotice as String)
if (!(jummpConfig.jummp.security.twofa.enrollmentNotice instanceof ConfigObject)) {
    jummp.security.twofa.enrollmentNotice = twoFaEnrollmentNotice
} else {
    jummp.security.twofa.enrollmentNotice = false
}
