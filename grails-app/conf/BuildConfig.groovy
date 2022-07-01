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





grails.servlet.version = "3.0" // needed to allow httpOnly cookies
grails.reload.enable = true
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.war.file = "target/${appName}.war"
grails.project.groupId = "net.biomodels.jummp"
grails.project.source.level = 1.8
grails.project.target.level = 1.8
grails.server.host="0.0.0.0"
grails.server.port=8080
grails.project.dependency.resolver = "maven"

customJvmArgs = ["-server", "-noverify", "-XX:+UseConcMarkSweepGC", "-XX:+UseParNewGC" ]
grails.project.fork = [
    // configure settings for the test-app JVM, uses the daemon by default
    //test: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 512, daemon: true],
    test: false,
    // configure settings for the run-app JVM
    run: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 512, forkReserve:false, jvmArgs: customJvmArgs],
    // configure settings for the run-war JVM
    war: [maxMemory: 8192, minMemory: 64, debug: false, maxPerm: 512, forkReserve:false, jvmArgs: customJvmArgs],
    // configure settings for the Console UI JVM
    console: [maxMemory: 1024, minMemory: 64, debug: false, maxPerm: 256, jvmArgs: customJvmArgs]
]

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
        excludes 'javassist'
        excludes 'grails-plugin-logging', 'grails-plugin-log4j', 'log4j'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility
    repositories {
        inherits true //inherit repo definitions from plugins
        if (System.getenv("JUMMP_ARTIFACTORY_URL")) {
            println "INFO\tArtifactory URL: " + System.getenv("JUMMP_ARTIFACTORY_URL")
            mavenRepo "${System.getenv('JUMMP_ARTIFACTORY_URL')}"
        }
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenLocal()
        mavenCentral()
        mavenRepo "https://www.ebi.ac.uk/~maven/m2repo"
        mavenRepo "https://www.ebi.ac.uk/~maven/m2repo_snapshots/"
        mavenRepo "https://repo.spring.io/milestone"
        mavenRepo "http://repo.grails.org/grails/core"

        // for spock-reports
        mavenRepo "https://jcenter.bintray.com"
    }
    dependencies {
        // required by OntologyLookupResolver
        compile "org.ccil.cowan.tagsoup:tagsoup:1.2"
        compile 'org.codehaus.groovy:groovy-backports-compat23:2.4.13'
        compile "com.googlecode.multithreadedtc:multithreadedtc:1.01"
        runtime 'mysql:mysql-connector-java:5.1.34'
        runtime "postgresql:postgresql:9.1-901.jdbc4"

        compile "uk.ac.ebi.ddi:ddi-ebe-ws-dao:1.2.1"
        // Jackson DataBinder has 'provided' scope in DDI: See
        //      https://github.com/BD2K-DDI/ddi-base-master/blob/2326b4/pom.xml
        //      https://github.com/BD2K-DDI/ddi-ebeye-ws-dao/blob/8bd08f/pom.xml
        compile "com.fasterxml.jackson.core:jackson-databind:2.9.0"
        compile "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.0"

        // remember to update this setting in jummp-plugin-configuration, jummp-plugin-core-api
        compile "net.biomodels.jummp:AnnotationStore:0.3.6-SNAPSHOT"
        compile "org.apache.solr:solr-solrj:5.4.1"
        //required by both JSBML and SolrJ
        compile "org.codehaus.woodstox:woodstox-core-lgpl:4.4.1"
        // fixes https://issues.apache.org/jira/browse/HTTPCLIENT-1418

        /* jms
        runtime('org.apache.activemq:activeio-core:3.1.2',
                'org.apache.activemq:activemq-core:5.5.0',
                'org.apache.activemq:activemq-spring:5.5.0',
                'org.apache.xbean:xbean-spring:3.7') {
            excludes 'commons-logging',
                    'commons-io',
                    'commons-pool',
                    'groovy-all',
                    'howl-logger',
                    'log4j',
                    'spring-beans',
                    'spring-context',
                    'spring-core',
                    'spring-test',
                    'slf4j-api',
                    'xalan',
                    'xml-apis'
        }*/
        runtime "ch.qos.logback:logback-core:0.9.29"
        compile "org.apache.tika:tika-core:1.23"
        /**
         * Weceem lists it as a runtime dependency, while jsbml needs it during compilation.
         * Unfortunately, Grails misbehaves and leaves xstream out at compile time unless we
         * explicitly add it as a dependency.
         */
        compile "com.thoughtworks.xstream:xstream:1.4.7"

        runtime("commons-jexl:commons-jexl:1.1") {
            excludes 'junit', 'commons-logging'
        }
        test "org.grails:grails-datastore-test-support:1.0-grails-2.4"

        // for spock-reports
        test "com.athaydes:spock-reports:1.3.0"
//        build "com.athaydes:spock-reports:1.3.0"
//        compile "com.athaydes:spock-reports:1.3.0"
//        runtime "com.athaydes:spock-reports:1.3.0"

        runtime 'org.javassist:javassist:3.17.1-GA'
        runtime "org.apache.camel:camel-exec:2.13.0"

        // DDMoRe Metadata Information Service uses jena 2.13
        compile("org.mbine.co:libCombineArchive:0.3-SNAPSHOT") {
            excludes 'junit', 'slf4j-api', 'slf4j-log4j12', 'slf4j-log4j12-impl', 'jmock-junit4', 'jena-core', 'icu4j'
        }
        compile "de.uni-rostock.sbi:CombineExt:1.3.1"
        // need to add this as an explicit dependency to configure exclusions
        // can't use apache-jena-libs due to pom packaging, rely on jena-tdb instead
        compile("eu.ddmore:lib-metadata:0.1.3-SNAPSHOT") {
            excludes 'apache-jena-libs'
        }
        compile("org.apache.jena:jena-tdb:1.1.2") {
            excludes 'slf4j-log4j12', 'slf4j-log4j12-impl'
        }
        compile("org.apache.jena:jena-core:2.13.0") {
            excludes 'slf4j-log4j12', 'icu4j'
        }
        compile "com.ibm.icu:icu4j:4.8.1"
        compile ("eu.ddmore.metadata:lib-metadata:1.5.2-SNAPSHOT") {
            excludes 'spring-context','spring-core','spring-test', 'jena', 'slf4j-log4j12'
        }
        compile "com.rometools:rome:1.11.1"
        /* Jedis and spring-data-redis clash in Grails2,
           though not Grails 3 https://stackoverflow.com/a/30776364 */
        compile("org.springframework.data:spring-data-redis:1.8.10.RELEASE") {
            excludes("spring-context", "spring-context-support", "spring-aop")
        }
        compile "redis.clients:jedis:2.9.0"
    }

    plugins {
        build ":tomcat:7.0.55.3"
        build ":codenarc:1.2"
        compile":rest-client-builder:2.1.1"
        // plugins for the compile step
        compile ":cache:1.1.8"
        compile ":cache-ehcache:1.0.5"
        compile ":webxml:1.4.1"
        compile ":perf4j:0.2.1"
        compile ":routing:1.3.2" //1.4.0
        //compile ":jms:1.2"
        compile(":mail:1.0.7")
        compile ":simple-captcha:1.0.0"
        compile(":quartz:1.0.2")
        compile ":scaffold-core:1.3.2"
        compile ":spring-security-acl:2.0.1"
        compile ":spring-security-core:2.0.0"
        compile ":spring-security-ldap:2.0.1"
        //compile ":svn:1.0.2"
        compile ":locale-variant:0.1"
        compile ":webflow:2.1.0"
        compile (":spring-session:1.2") {
            excludes "spring-data-redis"
        }

        runtime (":weceem:1.4") {
            /* feeds plugin clashes with rome api rendering Model of The Month RSS feed */
            excludes "feeds"
        }
        //compile ":weceem-spring-security:1.4"
        runtime ":database-migration:1.4.1"
        runtime ":hibernate4:4.3.10"
        runtime ":jquery-datatables:1.7.5"
        runtime ":console:1.5.8"
        runtime ":cors:1.3.0"
    }
}

grails.plugin.location.'jummp-plugin-core-api' = "jummp-plugins/jummp-plugin-core-api"
grails.plugin.location.'jummp-plugin-configuration' = "jummp-plugins/jummp-plugin-configuration"
grails.plugin.location.'jummp-plugin-git' = "jummp-plugins/jummp-plugin-git"
// Disconnect SVN for now because of the changes to the VcsManager interface and lack of time
//grails.plugin.location.'jummp-plugin-subversion' = "jummp-plugins/jummp-plugin-subversion"
grails.plugin.location.'jummp-plugin-common-format' = "jummp-plugins/jummp-plugin-common-format"
grails.plugin.location.'jummp-plugin-sbml' = "jummp-plugins/jummp-plugin-sbml"
grails.plugin.location.'jummp-plugin-combine-archive' = "jummp-plugins/jummp-plugin-combine-archive"
grails.plugin.location.'jummp-plugin-pharmml' = "jummp-plugins/jummp-plugin-pharmml"
grails.plugin.location.'jummp-plugin-mdl' = "jummp-plugins/jummp-plugin-mdl"
grails.plugin.location.'jummp-plugin-bives' = "jummp-plugins/jummp-plugin-bives"
grails.plugin.location.'jummp-plugin-simple-logging' = "jummp-plugins/jummp-plugin-simple-logging"
grails.plugin.location.'jummp-plugin-simple-cms' = "jummp-plugins/jummp-plugin-simple-cms"
grails.plugin.location.'jummp-plugin-web-application' = "jummp-plugins/jummp-plugin-web-application"
grails.plugin.location.'jummp-plugin-biomodels-dom' = "jummp-plugins/jummp-plugin-biomodels-dom"
grails.plugin.location.'jummp-plugin-annotation-source-ddmore' = "jummp-plugins/jummp-plugin-annotation-source-ddmore"
grails.plugin.location.'jummp-plugin-annotation-core' = "jummp-plugins/jummp-plugin-annotation-core"
grails.plugin.location.'jummp-plugin-omicsdi' = "jummp-plugins/jummp-plugin-omicsdi"
grails.plugin.location.'jummp-plugin-qc-info' = "jummp-plugins/jummp-plugin-qc-info"
//grails.plugin.location.'jummp-plugin-jms-remote' = "jummp-plugins/jummp-plugin-jms-remote"
if ("jms".equalsIgnoreCase(System.getenv("JUMMP_EXPORT"))) {
    println "INFO\tEnabling JMS remoting..."
    grails.plugin.location.'jummp-plugin-ast' = 'jummp-plugins/jummp-plugin-ast'
    grails.plugin.location.'jummp-plugin-remote' = "jummp-plugins/jummp-plugin-remote"
    grails.plugin.location.'jummp-plugin-jms' = "jummp-plugins/jummp-plugin-jms"
} else {
    println "INFO\tJMS disabled"
}

// Remove any files not needed in production mode
grails.war.resources = { stagingDir ->
}

//ensure that AST.jar is put in the right place. See scripts/AST.groovy
System.setProperty("jummp.basePath", new File("./").getAbsolutePath())
