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
 */





package net.biomodels.jummp.core.model

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.core.ModelException
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

@TestMixin(GrailsUnitTestMixin)
class RepositoryFileServiceSpec extends Specification {
    def service
    String cacheDir = File.createTempDir().getAbsolutePath()

    def doWithConfig(c) {
        c.jummp.model.cache.dir = cacheDir
    }

    def doWithSpring = {
        repositoryFileService(RepositoryFileService) { bean ->
            bean.scope = 'prototype'
            grailsApplication = ref('grailsApplication')
        }
    }

    def setup() {
        assert cacheDir
        service = grailsApplication.mainContext.repositoryFileService
    }

    def cleanup() {
        Path cachePath = Paths.get(cacheDir)
        cachePath.deleteDir()
    }

    void "test model cache directory configuration"() {
        when: "getting the model cache directory"
        String cacheDirectory = service.modelCacheDir
        then: "the bean sets the model cache directory effectively"
        cacheDirectory
    }

    void "test get method against non-existent model"() {
        given: "give a model identifier and a revision number"
        String modelId = "MODEL2003050001"
        int revisionNumber = 1
        when: "call get() method"
        service.get(modelId, revisionNumber)
        then: "catch an exception"
        final ModelException exception = thrown()
        "The model ${modelId} does not exist" == exception.message
    }
}
