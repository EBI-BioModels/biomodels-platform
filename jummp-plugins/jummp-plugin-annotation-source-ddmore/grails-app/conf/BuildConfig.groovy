/**
 * Copyright (C) 2010-2015 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    // configure settings for the run-app JVM
    run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the run-war JVM
    war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
    }
    log "warn"
    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
        mavenRepo "http://maven.mango-solutions.com/ddmore/"
        mavenRepo "http://www.ebi.ac.uk/~maven/m2repo_snapshots/"
    }

    String ddmoreMetadataIntegrationServiceVersion = "0.1.3-SNAPSHOT"
    String jungVersion = "2.0.1"

    dependencies {
        compile "eu.ddmore:lib-metadata-api:$ddmoreMetadataIntegrationServiceVersion"
        // can't use apache-jena-libs due to pom packaging, rely on jena-tdb instead
        compile("eu.ddmore:lib-metadata:$ddmoreMetadataIntegrationServiceVersion") {
            excludes 'apache-jena-libs'
        }
        compile "org.apache.jena:jena-tdb:1.1.2"

        compile("eu.ddmore.metadata:lib-metadata:1.5.2-SNAPSHOT") {
            // can't use Spring 4.1 yet, fall back on Grails defaults
            excludes 'spring-core', 'spring-context'
        }

/*        compile "net.biomodels.jummp:AnnotationStore:0.2.9-SNAPSHOT"*/
        compile("net.sf.jung:jung-graph-impl:$jungVersion")

        // useful for WordUtils.capitalise()
        compile "org.apache.commons:commons-lang3:3.3.2"
    }

    plugins {
        build(":release:3.0.1",
              ":rest-client-builder:1.0.3") {
            export = false
        }
    }
}

grails.plugin.location.'jummp-plugin-security' = "../jummp-plugin-security"
grails.plugin.location.'jummp-plugin-core-api' = "../jummp-plugin-core-api"
grails.plugin.location.'jummp-plugin-configuration' = "../jummp-plugin-configuration"
grails.plugin.location.'jummp-plugin-annotation-core' = "../jummp-plugin-annotation-core"
