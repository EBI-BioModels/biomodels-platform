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





grails.servlet.version = "2.5"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.war.file = "target/${appName}.war"
grails.project.groupId = "net.biomodels.jummp"
grails.project.source.level = 1.7
grails.project.target.level = 1.7
grails.project.dependency.resolver = "maven"

customJvmArgs = ["-server", "-noverify", "-XX:+UseConcMarkSweepGC", "-XX:+UseParNewGC" ]
grails.project.fork = [
    // configure settings for the test-app JVM, uses the daemon by default
    test: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 512, daemon:true],
    // configure settings for the run-app JVM
    run: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 512, forkReserve:false, jvmArgs: customJvmArgs],
    // configure settings for the run-war JVM
    war: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 512, forkReserve:false, jvmArgs: customJvmArgs],
    // configure settings for the Console UI JVM
    console: [maxMemory: 1024, minMemory: 64, debug: false, maxPerm: 256, jvmArgs: customJvmArgs]
]

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
        excludes 'javassist'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    legacyResolve true
    repositories {
        inherits true //inherit repo definitions from plugins
        if (System.getenv("JUMMP_ARTIFACTORY_URL")) {
            println "Artifactory URL: " + System.getenv("JUMMP_ARTIFACTORY_URL")
            mavenRepo "${System.getenv('JUMMP_ARTIFACTORY_URL')}"
        }
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
        mavenRepo "http://www.ebi.ac.uk/~maven/m2repo"
        mavenRepo "http://www.ebi.ac.uk/~maven/m2repo_snapshots/"
        mavenRepo "http://download.eclipse.org/jgit/maven"
        mavenRepo "http://www.biojava.org/download/maven/"
        mavenRepo "http://maven.mango-solutions.com/ddmore/"
    }
    dependencies {
        // required by OntologyLookupResolver
        compile "org.ccil.cowan.tagsoup:tagsoup:1.2"
        compile 'org.codehaus.groovy:groovy-backports-compat23:2.3.5'
        compile "com.googlecode.multithreadedtc:multithreadedtc:1.01"
        runtime 'mysql:mysql-connector-java:5.1.17'
        runtime "postgresql:postgresql:9.1-901.jdbc4"

        //using the latest groovy eclipse compiler 2.9.2-01
        compile "net.biomodels.jummp:AnnotationStore:0.2.8-SNAPSHOT"
        compile("org.apache.solr:solr-solrj:4.10.1") {
            excludes 'wstx-asl', //a newer version of woodstox comes with jsbml
                // httpcomponents 4.3.1 is incompatible with 4.2, which breaks jena-arq
                'httpclient', 'httpcore', 'httpmime'
        }
        //required by both JSBML and SolrJ
        compile "org.codehaus.woodstox:woodstox-core-lgpl:4.4.1"
        // fixes https://issues.apache.org/jira/browse/HTTPCLIENT-1418
        def httpComponentsVersion = '4.3.2'
        compile "org.apache.httpcomponents:httpclient:$httpComponentsVersion"
        compile "org.apache.httpcomponents:httpcore:$httpComponentsVersion"
        compile "org.apache.httpcomponents:httpmime:$httpComponentsVersion"

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
        compile 'log4j:log4j:1.2.17'
        compile "org.apache.tika:tika-core:1.3"
        /**
         * Weceem lists it as a runtime dependency, while jsbml needs it during compilation.
         * Unfortunately, Grails misbehaves and leaves xstream out at compile time unless we
         * explicitly add it as a dependency.
         */
        compile "com.thoughtworks.xstream:xstream:1.4.7"

        runtime("commons-jexl:commons-jexl:1.1") { excludes 'junit', 'commons-logging' }
        test "org.grails:grails-datastore-test-support:1.0-grails-2.3"
        runtime 'org.javassist:javassist:3.17.1-GA'
        runtime "org.apache.camel:camel-exec:2.13.0"

        // DDMoRe Metadata Information Service uses jena 2.13
        compile("org.mbine.co:libCombineArchive:0.1-SNAPSHOT") {
            excludes 'junit', 'slf4j-api', 'slf4j-log4j12', 'jmock-junit4', 'jena-core'
        }
        // need to add this as an explicit dependency to configure exclusions
        // can't use apache-jena-libs due to pom packaging, rely on jena-tdb instead
        compile("eu.ddmore:lib-metadata:0.1.3-SNAPSHOT") {
            excludes 'apache-jena-libs'
        }
        compile "org.apache.jena:jena-tdb:1.1.2"

        compile ("eu.ddmore.metadata:lib-metadata:1.5.1-SNAPSHOT") {
            excludes 'spring-context','spring-core','spring-test'
        }
    }

    plugins {
        build ":tomcat:7.0.54"

        runtime(":codenarc:0.21")
        compile ":webxml:1.4.1"
        compile ":perf4j:0.1.1"
        compile ":routing:1.3.2"
        //compile ":jms:1.2"
        compile ":executor:0.3"
        compile(":mail:1.0.7")
        compile ":simple-captcha:1.0.0"
        compile(":quartz:1.0.2")
        compile ":spring-security-acl:1.1.1"
        compile ":spring-security-core:1.2.7.3"
        compile ":spring-security-ldap:1.0.6"
        //compile ":svn:1.0.2"
        compile ":locale-variant:0.1"
        compile ":webflow:2.0.8.1"

        runtime(":weceem:1.2") {
            excludes 'ckeditor', 'xstream'
        }
        runtime ":ckeditor:3.6.6.1.1"
        runtime ":database-migration:1.4.0"
        runtime ":hibernate:3.6.10.16"
        runtime ":jquery:1.11.1"
        runtime ":jquery-datatables:1.7.5"
        runtime ":jquery-ui:1.10.4"

        test ":gmetrics:0.3.1"

    }
}

grails.plugin.location.'jummp-plugin-security' = "jummp-plugins/jummp-plugin-security"
grails.plugin.location.'jummp-plugin-core-api' = "jummp-plugins/jummp-plugin-core-api"
grails.plugin.location.'jummp-plugin-configuration' = "jummp-plugins/jummp-plugin-configuration"
grails.plugin.location.'jummp-plugin-git' = "jummp-plugins/jummp-plugin-git"
// Disconnect SVN for now because of the changes to the VcsManager interface and lack of time
//grails.plugin.location.'jummp-plugin-subversion' = "jummp-plugins/jummp-plugin-subversion"
grails.plugin.location.'jummp-plugin-sbml' = "jummp-plugins/jummp-plugin-sbml"
grails.plugin.location.'jummp-plugin-combine-archive' = "jummp-plugins/jummp-plugin-combine-archive"
grails.plugin.location.'jummp-plugin-pharmml' = "jummp-plugins/jummp-plugin-pharmml"
grails.plugin.location.'jummp-plugin-mdl' = "jummp-plugins/jummp-plugin-mdl"
grails.plugin.location.'jummp-plugin-bives' = "jummp-plugins/jummp-plugin-bives"
grails.plugin.location.'jummp-plugin-simple-logging' = "jummp-plugins/jummp-plugin-simple-logging"
grails.plugin.location.'jummp-plugin-web-application' = "jummp-plugins/jummp-plugin-web-application"
grails.plugin.location.'jummp-plugin-annotation-source-ddmore' = "jummp-plugins/jummp-plugin-annotation-source-ddmore"
grails.plugin.location.'jummp-plugin-annotation-core' = "jummp-plugins/jummp-plugin-annotation-core"
//grails.plugin.location.'jummp-plugin-jms-remote' = "jummp-plugins/jummp-plugin-jms-remote"
if ("jms".equalsIgnoreCase(System.getenv("JUMMP_EXPORT"))) {
    println "Enabling JMS remoting..."
    grails.plugin.location.'jummp-plugin-ast' = 'jummp-plugins/jummp-plugin-ast'
    grails.plugin.location.'jummp-plugin-remote' = "jummp-plugins/jummp-plugin-remote"
    grails.plugin.location.'jummp-plugin-jms' = "jummp-plugins/jummp-plugin-jms"
} else {
    println "JMS disabled"
}

// Remove any files not needed in production mode
grails.war.resources = { stagingDir ->
}

//ensure that AST.jar is put in the right place. See scripts/AST.groovy
System.setProperty("jummp.basePath", new File("./").getAbsolutePath())
