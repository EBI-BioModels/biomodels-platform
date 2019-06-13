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
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.groupId = "net.biomodels.jummp.jms"
grails.project.source.level = 1.7
grails.project.target.level = 1.7
grails.project.dependency.resolver = "maven"

grails.project.fork = [
    // configure settings for the test-app JVM, uses the daemon by default
    test: false, //[maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 512, daemon:true],
    // configure settings for the run-app JVM
    run: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 512, forkReserve:false],
    // configure settings for the run-war JVM
    war: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 512, forkReserve:false],
    // configure settings for the Console UI JVM
    console: [maxMemory: 1024, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    legacyResolve false
    repositories {
        if (System.getenv("JUMMP_ARTIFACTORY_URL")) {
            mavenRepo "${System.getenv('JUMMP_ARTIFACTORY_URL')}"
        }
        grailsCentral()

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        mavenLocal()
        mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
        mavenRepo "http://www.ebi.ac.uk/~maven/m2repo"
        mavenRepo "http://www.ebi.ac.uk/~maven/m2repo_snapshots/"
        //flatDir name: "jummpLibs", dirs: "../../lib/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        runtime('org.apache.activemq:activeio-core:3.1.2',
                'org.apache.activemq:activemq-core:5.5.0',
                'org.apache.activemq:activemq-spring:5.5.0',
                'org.apache.xbean:xbean-spring:3.7') {
            excludes 'commons-logging',
                    'commons-pool',
                    'commons-io',
                    'groovy-all',
                    'howl-logger',
                    'log4j',
                    'spring-beans',
                    'spring-context',
                    'spring-core',
                    'spring-test',
                    'slf4j-api',
                    'xalan'
                    //'xml-apis'
        }

        compile "xml-apis:xml-apis:1.4.01"
        compile 'org.jdom:jdom:1.1.3'
        compile 'org.springframework:spring-jms:3.2.4.RELEASE'
        // work around for 2.3.2's wonderful dependency resolution issues
        //compile "org.springframework:spring-test:3.2.4.RELEASE"
    }
    plugins {
        build ":tomcat:7.0.55.3"

        compile ":perf4j:0.2"
        compile ":spring-security-core:2.0.0"
        compile(":spring-security-ldap:2.0.1")
        compile ":jms:1.2"

        runtime ":hibernate4:4.3.10"
        runtime ":jquery:1.11.1"
    }
}
grails.plugin.location.'jummp-plugin-core-api'="../jummp-plugin-core-api"
//grails.plugin.location.'jummp-plugin-bives'="../jummp-plugin-bives"
grails.plugin.location.'jummp-plugin-sbml'="../jummp-plugin-sbml"
grails.plugin.location.'jummp-plugin-remote'="../jummp-plugin-remote"

//ensure that AST.jar is put in the right place. See ../../scripts/AST.groovy
if ("jummp-plugin-jms".equals(appName)) {
    System.setProperty("jummp.basePath", "${new File('../../').getAbsolutePath()}")
}
