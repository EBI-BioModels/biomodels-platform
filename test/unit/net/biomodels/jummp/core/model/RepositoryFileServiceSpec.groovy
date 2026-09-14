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
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import spock.lang.Specification

import java.nio.file.Files
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

    void "invalidateModelRevisionCache removes an existing revision cache directory"() {
        given: "a revision whose files were cached on an earlier access"
        String modelId = "MODEL2609130001"
        int revisionNumber = 4
        Revision revision = new Revision(model: new Model(submissionId: modelId), revisionNumber: revisionNumber)
        // build the expected path from the service's own view of the cache dir, not the
        // spec's local field, in case they resolve differently in this test harness
        Path revisionCacheDir = Paths.get(service.modelCacheDir, modelId, revisionNumber as String)
        Files.createDirectories(revisionCacheDir)
        Files.createFile(revisionCacheDir.resolve("stale-file-from-a-since-deleted-revision.xml"))
        assert revisionCacheDir.toFile().exists()
        when: "the revision's cache is invalidated, e.g. after a mid-history delete remaps its vcsId"
        boolean removed = service.invalidateModelRevisionCache(revision)
        then: "the stale cache directory - and everything in it - is gone"
        removed
        !revisionCacheDir.toFile().exists()
    }

    void "invalidateModelRevisionCache is a no-op when the revision was never cached"() {
        given: "a revision that has no cache directory yet"
        Revision revision = new Revision(model: new Model(submissionId: "MODEL2609130001"), revisionNumber: 99)
        when: "its cache is invalidated anyway"
        boolean removed = service.invalidateModelRevisionCache(revision)
        then: "nothing is removed and no exception is thrown"
        !removed
    }
}
