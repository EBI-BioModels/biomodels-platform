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
grails.project.groupId = "net.biomodels.jummp.core"
grails.project.source.level = 1.8
grails.project.target.level = 1.8

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

grails.project.dependency.resolver = "maven"
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
        mavenRepo "https://www.ebi.ac.uk/~maven/m2repo"
        mavenRepo "https://www.ebi.ac.uk/~maven/m2repo_snapshots/"
    }
    dependencies {
        compile("eu.ddmore.pharmml:libPharmML:0.4-beta-b3")
        compile("net.biomodels.jummp:AnnotationStore:0.3.5") {
            excludes 'slf4j-log4j12'
        }
        compile("eu.ddmore.metadata:lib-metadata:1.5.2-SNAPSHOT") {
            excludes 'spring-context','spring-core','spring-test', 'jena', 'slf4j-log4j12'
        }
        compile("uk.ac.ebi.ddi:ddi-ebe-ws-dao:1.0") {
            excludes 'slf4j-log4j12'
        }
        // Jackson DataBinder has 'provided' scope in DDI: See
        //      https://github.com/BD2K-DDI/ddi-base-master/blob/2326b4/pom.xml
        //      https://github.com/BD2K-DDI/ddi-ebeye-ws-dao/blob/8bd08f/pom.xml
        compile "com.fasterxml.jackson.core:jackson-databind:2.5.2"
        compile "org.apache.commons:commons-lang3:3.3.2"
        compile "org.apache.tika:tika-core:1.23"
    }
    plugins {
        build ":tomcat:7.0.55.3"
        compile ":perf4j:0.2.1"
        runtime ":hibernate4:4.3.10"
        compile ":spring-security-acl:2.0.1"
        compile ":spring-security-core:2.0.0"
        compile ":spring-security-ldap:2.0.1"
    }
}
