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
grails.project.source.level = 1.8
grails.project.target.level = 1.8
// maven can't handle flatDirs, would break sbml and bives
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
    inherits("global") {
    }
    log "warn"
    // circumvent http://jira.grails.org/browse/GRAILS-9984
    legacyResolve false
    repositories {
        if (System.getenv("JUMMP_ARTIFACTORY_URL")) {
            mavenRepo "${System.getenv('JUMMP_ARTIFACTORY_URL')}"
        }
        grailsCentral()
        mavenRepo("http://mvn.sems.uni-rostock.de/releases/")
        mavenCentral()
        mavenRepo "https://www.ebi.ac.uk/~maven/m2repo"
        mavenRepo "https://www.ebi.ac.uk/~maven/m2repo_snapshots/"
        mavenRepo "https://www.ebi.ac.uk/Tools/maven/repos/content/repositories/pst-release/"
        mavenRepo "https://www.ebi.ac.uk/Tools/maven/repos/content/repositories/pst-snapshots/"
    }
    dependencies {
        compile("org.mbine.co:libCombineArchive:0.3.1-SNAPSHOT") {
            excludes 'junit', 'slf4j-api', 'slf4j-log4j12', 'slf4j-log4j12-impl', 'jmock-junit4', 'jena-core', 'icu4j'
        }
        compile "de.uni-rostock.sbi:CombineExt:1.3.1"
        compile "com.ibm.icu:icu4j:4.8.1"

        runtime("commons-jexl:commons-jexl:1.1") { excludes 'junit', 'commons-logging', 'slf4j-log4j12' }
        compile "commons-io:commons-io:2.1"
        compile 'xml-apis:xml-apis:1.4.01'
        // mime-type detection
        compile "org.apache.tika:tika-core:1.23"
    }

    plugins {
        build ":tomcat:7.0.55.3"
        build(":codenarc:1.2")
    }
}
grails.plugin.location.'jummp-plugin-core-api' = "../jummp-plugin-core-api"
