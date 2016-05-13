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
**/


import net.biomodels.jummp.core.model.identifier.ModelIdentifierUtils

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
Properties jummpProperties = new Properties()
try {
	def service = new net.biomodels.jummp.plugins.configuration.ConfigurationService()
    String pathToConfig=service.getConfigFilePath()
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

grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
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
grails.views.default.codec = "none" // none, html, base64
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

// set per-environment serverURL stem for creating absolute links
environments {
    production {
        grails.serverURL = jummpConfig.jummp.server.url
    }
    development {
        grails.serverURL = "http://localhost:8080/${appName}"
    }
    test {
        grails.serverURL = "http://localhost:8080/${appName}"
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
log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
    appenders {
        // file appender that writes out the URLs of the Google Chart API graphs generated by the performanceGraphAppender
        def performanceGraphFileAppender = new org.apache.log4j.FileAppender(
            fileName: "logs/perfGraphs.log",
            layout: pattern(conversionPattern: '%m%n')
        )
        appender name: 'performanceGraphFileAppender', performanceGraphFileAppender

        // this appender creates the Google Chart API graphs
        def performanceGraphAppender = new org.perf4j.log4j.GraphingStatisticsAppender(
            graphType: 'Mean',      // possible options: Mean, Min, Max, StdDev, Count or TPS
            tagNamesToGraph: 'tag1,tag2,tag3',
            dataPointsPerGraph: 5
        )
        performanceGraphAppender.addAppender(performanceGraphFileAppender)
        appender name: 'performanceGraph', performanceGraphAppender


        // file appender that writes out the textual, aggregated performance stats generated by the performanceStatsAppender
        def performanceStatsFileAppender = new org.apache.log4j.FileAppender(
            fileName: "logs/perfStats.log",
            layout: pattern(conversionPattern: '%m%n')  // alternatively use the StatisticsCsvLayout to generate CSV
        )
        appender name: 'performanceStatsFileAppender', performanceStatsFileAppender


        // this is the most important appender and first in the appender chain. it aggregates all profiling data withing a certain time frame.
        // the GraphingStatisticsAppender is attached as a child to this appender and uses its aggregated data.
        def performanceStatsAppender = new org.perf4j.log4j.AsyncCoalescingStatisticsAppender(
            timeSlice: 30 * 60 * 1000    // 30 minutes in ms
        )
        performanceStatsAppender.addAppender(performanceStatsFileAppender)
        performanceStatsAppender.addAppender(performanceGraphAppender)
        appender name: 'performanceStatsAppender', performanceStatsAppender

        rollingFile name: "jummpAppender", file: "logs/jummp-core.log", threshold: org.apache.log4j.Level.WARN
        rollingFile name: "eventsAppender", file: "logs/jummp-events.log", threshold: org.apache.log4j.Level.DEBUG

        // change the threshold to DEBUG to have debug output in development mode
        console name: "stdout", threshold: org.apache.log4j.Level.WARN
    }

    // configure the performanceStatsAppender to log at INFO level
    info   performanceStatsAppender: 'org.perf4j.TimingLogger'
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
        'net.sf.ehcache.hibernate',
        'org.weceem'
    ]

    warn   jummpAppender: 'org.mortbay.log'
    // Simple Logging goes to its own file
    info   eventsAppender: 'net.biomodels.jummp.plugins.simplelogging'

    rollingFile name: "debugAppender", file: "logs/jummp-debug.log", threshold: org.apache.log4j.Level.DEBUG
    rollingFile name: "hibernateAppender", file: "logs/jummp-hibernate.log", threshold: org.apache.log4j.Level.DEBUG

    debug debugAppender: [
        'net.biomodels.jummp',
        'net.biomodels.jummp.core',
        'net.biomodels.jummp.model',
        'net.biomodels.jummp.core.model',
        'net.biomodels.jummp.core.model.identifier',
        'net.biomodels.jummp.core.model.identifier.decorator',
        'net.biomodels.jummp.core.model.identifier.generator',
        'net.biomodels.jummp.core.model.identifier.support',
        'net.biomodels.jummp.plugins.pharmml'
    ]
    debug hibernateAppender: [
        'org.codehaus.groovy.grails.orm.hibernate',
        'org.codehaus.groovy.grails.orm.support',
        'org.hibernate.SQL',
        'org.springframework.orm.hibernate3.support'

    ]
    trace hibernateAppender: 'org.hibernate.type.descriptor.sql.BasicBinder'
}

// Added by the Spring Security Core plugin:
grails.plugins.springsecurity.userLookup.userDomainClassName = 'net.biomodels.jummp.plugins.security.User'
grails.plugins.springsecurity.userLookup.authorityJoinClassName = 'net.biomodels.jummp.plugins.security.UserRole'
grails.plugins.springsecurity.authority.className = 'net.biomodels.jummp.plugins.security.Role'

jummp.controllerAnnotations = [
        // protect the springsecurity ui plugin controllers
        '/aclclass/**':          ['ROLE_ADMIN'],
        '/aclentry/**':          ['ROLE_ADMIN'],
        '/aclobjectidentity/**': ['ROLE_ADMIN'],
        '/aclsid/**':            ['ROLE_ADMIN'],
        '/persistentlogin/**':   ['ROLE_ADMIN'],
        '/register/**':          ['ROLE_ADMIN'],
        '/registrationcode/**':  ['ROLE_ADMIN'],
        '/requestmap/**':        ['ROLE_ADMIN'],
        '/role/**':              ['ROLE_ADMIN'],
        '/securityinfo/**':      ['ROLE_ADMIN'],
        '/user/**':              ['ROLE_ADMIN'],
        '/wcm-tools/**':         ['ROLE_ADMIN'],
        '/searchable/**':        ['ROLE_ADMIN'],
        '/ck/**':                ['ROLE_ADMIN'],
        "/wcmEditor/**":         ["hasRole('ROLE_ADMIN')"],
        "/wcmPortal/**":         ["hasRole('ROLE_ADMIN')"],
        "/wcmRepository/**":     ["hasRole('ROLE_ADMIN')"],
        "/wcmSpace/**":          ["hasRole('ROLE_ADMIN')"],
        "/wcmSynchronization/**": ["hasRole('ROLE_ADMIN')"],
        "/wcmVersion/**":        ["hasRole('ROLE_ADMIN')"],
        "/wcm*/**":              ["permitAll"],
        "/WeceemFiles/**":       ["permitAll"],
        "/css/**":               ["permitAll"],
        "/images/**":            ["permitAll"],
        "/js/**":                ["permitAll"],
        "/plugins/jquery*/**":   ["permitAll"],
        "/plugins/navigation*/**": ["permitAll"],
        "/plugins/blueprint*/**": ["permitAll"],
        "/plugins/ckeditor*/**":  ["permitAll"],
        "/plugins/weceem*/**":    ["permitAll"]
]

// ldap
if ((jummpConfig.jummp.security.ldap.enabled instanceof ConfigObject) || !Boolean.parseBoolean(jummpConfig.jummp.security.ldap.enabled)) {
    jummp.security.ldap.enabled = false
    println("Excluding ldap")
    pluginsToExclude << "springSecurityLdap"
} else {
    println("using ldap")
    jummp.security.ldap.enabled = true
    grails.plugins.springsecurity.ldap.context.managerDn         = jummpConfig.jummp.security.ldap.managerDn
    grails.plugins.springsecurity.ldap.context.managerPassword   = jummpConfig.jummp.security.ldap.managerPw
    grails.plugins.springsecurity.ldap.context.server            = jummpConfig.jummp.security.ldap.server
    grails.plugins.springsecurity.ldap.search.base               = jummpConfig.jummp.security.ldap.search.base
    grails.plugins.springsecurity.ldap.authorities.searchSubtree = jummpConfig.jummp.security.ldap.search.subTree
    grails.plugins.springsecurity.ldap.search.filter             = jummpConfig.jummp.security.ldap.search.filter

    // static options
    grails.plugins.springsecurity.ldap.authorities.ignorePartialResultException = true
    grails.plugins.springsecurity.ldap.authorities.retrieveGroupRoles = true
    grails.plugins.springsecurity.ldap.authorities.retrieveDatabaseRoles = true
}

// version control backend
if (jummpConfig.jummp.vcs.exchangeDirectory) {
    jummp.vcs.exchangeDirectory = jummpConfig.jummp.vcs.exchangeDirectory
}
if (jummpConfig.jummp.vcs.workingDirectory) {
    jummp.vcs.workingDirectory = jummpConfig.jummp.vcs.workingDirectory
}
// search config
if (!(jummpConfig.jummp.search.url instanceof ConfigObject)) {
    final Pattern URL_PATTERN = ~/http:\/\/[a-zA-Z0-9\.\-_]+(:[0-9]+)?(\/[a-zA-Z0-9\-\._]+)*/
    final String solrSetting = jummpConfig.jummp.search.url
    final String solrUrl
    if (solrSetting?.endsWith("/")) {
        solrUrl = solrSetting.substring(0, solrSetting.length() - 1)
    } else {
        solrUrl = solrSetting
    }
    if (!solrUrl || ! (solrUrl ==~ URL_PATTERN)) {
        throw new IllegalArgumentException("""The URL for the search server ($solrUrl) does \
not look right. Check the value of setting 'jummp.search.url'.""")
    } else {
        jummp.search.url = solrUrl
        println "INFO\tUsing $solrUrl as the URL of the search server."
    }
} else {
    throw new IllegalArgumentException("""\
Please add the setting 'jummp.search.url', pointing to a Solr instance, to your configuration.""")
}
if (!(jummpConfig.jummp.search.folder instanceof ConfigObject)) {
    final String searchFolder = jummpConfig.jummp.search.folder
    jummp.search.folder = searchFolder
    println "INFO\tSOLR_HOME is set to $searchFolder."
} else {
    println "WARN\tSetting jummp.search.folder is undefined. Have you set \$SOLR_HOME?"
}
if (!(jummpConfig.jummp.search.pathToIndexerExecutable instanceof ConfigObject)) {
    jummp.search.pathToIndexerExecutable = jummpConfig.jummp.search.pathToIndexerExecutable
}
else {
	println "WARN\tSetting jummp.search.pathToIndexerExecutable is undefined. Models will not be indexed correctly in the search engine."
}
// registration settings
if (!(jummpConfig.jummp.security.registration.email.send instanceof ConfigObject) && Boolean.parseBoolean(jummpConfig.jummp.security.registration.email.send)) {
    jummp.security.registration.email.send         = Boolean.parseBoolean(jummpConfig.jummp.security.registration.email.send)
    jummp.security.registration.email.sender       = jummpConfig.jummp.security.registration.email.sender
    if (!(jummpConfig.jummp.security.registration.email.sendToAdmin instanceof ConfigObject)) {
        jummp.security.registration.email.sendToAdmin = Boolean.parseBoolean(jummpConfig.jummp.security.registration.email.sendToAdmin)
    } else {
        jummp.security.registration.email.sendToAdmin = false
    }
    jummp.security.registration.email.adminAddress = jummpConfig.jummp.security.registration.email.adminAddress
    jummp.security.registration.email.subject      = jummpConfig.jummp.security.registration.email.subject
    jummp.security.registration.email.body         = jummpConfig.jummp.security.registration.email.body
    jummp.security.registration.verificationURL    = jummpConfig.jummp.security.registration.verificationURL
    jummp.security.activation.email.subject        = jummpConfig.jummp.security.activation.email.subject
    jummp.security.activation.email.body           = jummpConfig.jummp.security.activation.email.body
    jummp.security.activation.activationURL        = jummpConfig.jummp.security.activation.activationURL
    jummp.security.resetPassword.email.body    = jummpConfig.jummp.security.resetPassword.email.body
    jummp.security.resetPassword.email.subject = jummpConfig.jummp.security.resetPassword.email.subject
} else {
    jummp.security.registration.email.send = false
}

// whether a user has curator rights by default, allowing them to publish models
// they have access to.
if (!(jummpConfig.jummp.security.curatorByDefault instanceof ConfigObject)) {
    jummp.security.curatorByDefault = Boolean.parseBoolean(jummpConfig.jummp.security.curatorByDefault)
} else {
    // default to true
    jummp.security.curatorByDefault = true
}
// whether sbml validation is turned on
if (!(jummpConfig.jummp.plugins.sbml.validation instanceof ConfigObject)) {
	jummp.plugins.sbml.validation = Boolean.parseBoolean(jummpConfig.jummp.plugins.sbml.validation)
}

// file preview size, in bytes
if (!(jummpConfig.jummp.web.file.preview instanceof ConfigObject)) {
	jummp.web.file.preview = Integer.parseInt(jummpConfig.jummp.web.file.preview)
}
else {
	jummp.web.file.preview = 100 * 1024 //default preview size: 100 kb
}

// whether a user is allowed to change the password depends on the setting an if LDAP is used
// in case of LDAP changing the password is not (yet) possible in the application
if (!(jummpConfig.jummp.security.ui.changePassword instanceof ConfigObject)) {
    jummp.security.ui.changePassword = Boolean.parseBoolean(jummpConfig.jummp.security.ui.changePassword)
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
    jummp.security.anonymousRegistration = Boolean.parseBoolean(jummpConfig.jummp.security.anonymousRegistration)
} else {
    jummp.security.anonymousRegistration = true
}

// For the job, removing authentication hashes that are unused for a configurable time
// Used by AuthenticationHashService
if (!(jummpConfig.jummp.authenticationHash.startRemoveOffset instanceof ConfigObject)) {
    jummp.authenticationHash.startRemoveOffset = Long.parseLong(jummpConfig.jummp.authenticationHash.startRemoveOffset)
} else {
    jummp.authenticationHash.startRemoveOffset = 5*60*1000
}
if (!(jummpConfig.jummp.authenticationHash.removeInterval instanceof ConfigObject)) {
    jummp.authenticationHash.removeInterval = Long.parseLong(jummpConfig.jummp.authenticationHash.removeInterval)
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

if (jummpConfig.jummp.firstRun instanceof ConfigObject || !Boolean.parseBoolean(jummpConfig.jummp.firstRun)) {
    // only add side protection if not in first run mode
    if (!(jummpConfig.jummp.server.protection instanceof ConfigObject) && Boolean.parseBoolean(jummpConfig.jummp.server.protection)) {
        jummp.controllerAnnotations.put("/login/**", ['IS_AUTHENTICATED_ANONYMOUSLY'])
        jummp.controllerAnnotations.put("/**", ['ROLE_USER'])
    }
}

if (!(jummpConfig.jummp.security.cms.policy instanceof ConfigObject)) {
    jummp.security.cms.policy = jummpConfig.jummp.security.cms.policy
} else if (System.getenv("JUMMP_SECURITY_CMS_POLICY") != null) {
    jummp.security.cms.policy = System.getenv("JUMMP_SECURITY_CMS_POLICY")
} else {
    jummp.security.cms.policy = null
}

if (jummp.security.cms.policy != null) {
    println "Using ${jummp.security.cms.policy} to configure Weceem permissions."
} else {
    println "Using Weceem's default permissions."
}

grails.plugins.springsecurity.controllerAnnotations.staticRules = jummp.controllerAnnotations
if (!"jms".equalsIgnoreCase(System.getenv("JUMMP_EXPORT"))) {
    jms.disabled = true
}

// tweak searchable configuration so that it plays nicely with database-migration
searchable {
    mirrorChanges = false
    bulkIndexOnStartup = false
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

if (pluginsToExclude) {
    grails.plugin.excludes = pluginsToExclude
}

//platform-core 1.0RC5, used by weceem
plugin.platformCore.events.catchFlushExceptions = true
// weceem
grails.mime.file.extensions = false
weceem.content.prefix = 'content'
weceem.tools.prefix = 'wcm-tools'
weceem.admin.prefix = 'wcm-admin'
weceem.create.default.space = true
weceem.default.space.template = "classpath:/weceem-jummp-default-space.zip"
weceem.security.policy.path = jummp.security.cms.policy
grails.resources.adhoc.excludes=["/content/*"]

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

grails.mails.props=[:]
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
	grails.mails.props["mail.smtp.socketFactory.port"]=grails.mail.port
}
if (!(jummpConfig.jummp.security.mailer.auth instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.auth"]=jummpConfig.jummp.security.mailer.auth
}
if (!(jummpConfig.jummp.security.mailer.socketFactory instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.socketFactory.class"]=jummpConfig.jummp.security.mailer.socketFactory
}
if (!(jummpConfig.jummp.security.mailer.fallback instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.socketFactory.fallback"]=jummpConfig.jummp.security.mailer.fallback
}
if (!(jummpConfig.jummp.security.mailer.tlsrequired instanceof ConfigObject)) {
	grails.mail.props["mail.smtp.starttls.required"]=jummpConfig.jummp.security.mailer.tlsrequired
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
modelIdentifierSettings?.entrySet().each {
    jummp.model.id."${it.key}" = it.value
}

// Uncomment and edit the following lines to start using Grails encoding & escaping improvements

/* remove this line
// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside null
                scriptlet = 'none' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}
remove this line */
if (!(jummpConfig.jummp.context.help.root instanceof ConfigObject)) {
    def pages=["root", "browse", "search", "login", "display", "archives", "submission", "update", "profile", "sharing", "teams", "notifications","annotate"]
    pages.each {
        if (!(jummpConfig.jummp.context.help."${it}" instanceof ConfigObject)) {
            jummp.context.help."${it}" = jummpConfig.jummp.context.help."${it}"
        }
    }
}
jummp.config.maintenance = false

jummp.id.generators = ModelIdentifierUtils.processGeneratorSettings(jummp)
