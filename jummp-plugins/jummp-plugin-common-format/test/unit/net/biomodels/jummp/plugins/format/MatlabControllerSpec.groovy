/*
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

package net.biomodels.jummp.plugins.format

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import groovy.transform.CompileStatic
import net.biomodels.jummp.core.IMetadataService
import net.biomodels.jummp.core.annotation.QualifierTransportCommand
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand
import net.biomodels.jummp.core.annotation.StatementTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.ModellingApproach
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
@TestFor(MatlabController)
class MatlabControllerSpec extends Specification {
    MatlabController controller

    def setup() {
        defineBeans {
            metadataDelegateService(NoGenericAnnotationMetadataService) { bean ->
                bean.autowire = 'byName'
                bean.scope = 'singleton'
            }
            matlabController(MatlabController) { bean ->
                bean.autowire = 'byName'
                metadataDelegateService = ref 'metadataDelegateService'
            }
        }

        def matlabService = mockFor MatlabFormatService
        matlabService.demand.getMatlabFilesFromRevision {
            File[] files = new File(".").listFiles()
            Set<File> resultSet = new TreeSet<>()
            resultSet.addAll(files)
            resultSet
        }
        controller = applicationContext.matlabController
        controller.matlabFormatService = matlabService.createMock()
    }

    def cleanup() {
    }

    void "test something"() {
        given:
        flash['genericModel'] = [revision: new RevisionTransportCommand(name: "Foo")]

        when:
        def result = controller.show()

        then:
        result['matlabFiles'].find { File f -> f.name == "application.properties" }
        result['annotations'].isEmpty()
    }
}

@CompileStatic
class NoGenericAnnotationMetadataService implements IMetadataService {
    @Override
    Map<QualifierTransportCommand, List<ResourceReferenceTransportCommand>> fetchGenericAnnotations(
        RevisionTransportCommand rev) {
        [:]
    }

    @Override
    List<ResourceReferenceTransportCommand> findAllResourceReferencesForQualifier(RevisionTransportCommand revision, String qualifier) {
        return null
    }

    @Override
    List<StatementTransportCommand> findAllStatementsForQualifier(RevisionTransportCommand revision, String qualifier) {
        return null
    }

    @Override
    List<ResourceReferenceTransportCommand> findAllResourceReferencesForSubject(RevisionTransportCommand revision, String subject) {
        return null
    }

    @Override
    List<StatementTransportCommand> findAllStatementsForSubject(RevisionTransportCommand revision, String subject) {
        return null
    }

    @Override
    List<String> getMetadataNamespaces() {
        return null
    }

    @Override
    Set<String> fetchModelTags(String modelSubmissionId) {
        return null
    }

    @Override
    List searchModellingApproach(String searchTerm) {
        return null
    }

    @Override
    ModellingApproach getModellingApproach(String accession) {
        return null
    }

    @Override
    ModellingApproach getModellingApproach(ModelTransportCommand model) {
        return null
    }
}
