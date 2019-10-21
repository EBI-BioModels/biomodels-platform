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
 */

package net.biomodels.jummp.core

import eu.ddmore.metadata.service.ValidationException
import grails.async.Promises
import net.biomodels.jummp.annotation.SectionContainer
import net.biomodels.jummp.annotationstore.ResourceReference
import net.biomodels.jummp.annotationstore.RevisionAnnotation
import net.biomodels.jummp.annotationstore.Statement
import net.biomodels.jummp.core.annotation.*
import net.biomodels.jummp.core.model.AnnotationValidationContext
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.deployment.biomodels.CurationNotesTransportCommand
import net.biomodels.jummp.deployment.biomodels.TagTransportCommand
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.Revision
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled

/**
 * Simple delegate for metadataService.
 *
 * This service is the point of contact for any class outside of Jummp's core
 * that wishes to interact with metadataService.
 *
 * As this service delegates all database work to metadataService,there is no need for
 * transactional behaviour.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class MetadataDelegateService implements IMetadataService {
    private final Log log = LogFactory.getLog(getClass())
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    static transactional = false

    private final Map<String, String> MODELLING_APPROACHES =
        ["MAMO_0000009": "Constraint-based model",
         "MAMO_0000025": "Petri net",
         "MAMO_0000030": "Logical model",
         "MAMO_0000046": "Ordinary differential equation model"]

    /**
     * Dependency injection for the metadata service.
     */
    MetadataService metadataService
    /**
     * Dependency injection for the curation notes service.
     */
    def curationNotesService

    def modelTagService
    /**
     * {@inheritDoc}
     */
    @Profiled(tag = "metadataDelegateService.findAllResourceReferencesForQualifier")
    List<ResourceReferenceTransportCommand> findAllResourceReferencesForQualifier(
            RevisionTransportCommand revision, String qualifier) {
        List<ResourceReference> references = metadataService.
                findAllResourceReferencesForQualifier(revision.id, qualifier)
        wrapResourceReferences(references)
    }

    /**
     * {@inheritDoc}
     */
    @Profiled(tag = "metadataDelegateService.findAllResourceReferencesForSubject")
    List<ResourceReferenceTransportCommand> findAllResourceReferencesForSubject(
            RevisionTransportCommand revision, String subject) {
        List<ResourceReference> references = metadataService.
                findAllResourceReferencesForSubject(revision.id, subject)
        wrapResourceReferences(references)
    }

    /**
     * {@inheritDoc}
     */
    @Profiled(tag = "metadataDelegateService.findAllStatementsForQualifier")
    List<StatementTransportCommand> findAllStatementsForQualifier(RevisionTransportCommand
            revision, String qualifier) {
        List<Statement> statements = metadataService.findAllStatementsForQualifier(
                revision.id, qualifier)
        wrapStatements(statements)
    }

    /**
     * {@inheritDoc}
     */
    @Profiled(tag = "metadataDelegateService.findAllStatementsForSubject")
    List<StatementTransportCommand> findAllStatementsForSubject(RevisionTransportCommand
            revision, String subject) {
        List<Statement> statements = metadataService.findAllStatementsForSubject(
                revision.id, subject)
        wrapStatements(statements)
    }

    @Profiled(tag = "metadataDelegateService.wrapResourceReferences")
    private List<ResourceReferenceTransportCommand> wrapResourceReferences(
            List<ResourceReference> references) {
        use(ResourceReferenceCategory) {
            return references.collect { ResourceReference r ->
                r.toCommandObject()
            }
        }
    }

    @Profiled(tag = "metadataDelegateService.wrapStatements")
    private List<StatementTransportCommand> wrapStatements(List<Statement> statements) {
        use(StatementCategory) {
            return statements.collect { Statement s ->
                s.toCommandObject()
            }
        }
    }

    @Profiled(tag = "metadataDelegateService.saveMetadata")
    boolean saveMetadata(String model, List<StatementTransportCommand> statements) {
        metadataService.saveMetadata(model, statements)
    }

    @Profiled(tag = "metadataDelegateService.persistAnnotationSchema")
    boolean persistAnnotationSchema(Collection<SectionContainer> sections) {
        if (IS_DEBUG_ENABLED) {
            log.debug "Begin persisting annotation schema..."
        }
        Promises.task {
            metadataService.persistAnnotationSchema(sections)
        }.onComplete {
            if (IS_DEBUG_ENABLED) {
                log.debug "...done persisting annotation schema"
            }
        }
    }

    @Profiled(tag = "metadataDelegateService.validateModelRevision")
    AnnotationValidationContext validateModelRevision(RevisionTransportCommand revision, List<StatementTransportCommand> statements) {
        try {
            return metadataService.validateModelRevision(Revision.get(revision.id), statements)
        }catch(ValidationException e){
            throw e
        }
    }

    @Profiled(tag = "metadataDelegateService.getMetadataNamespaces")
    List<String> getMetadataNamespaces() {
        metadataService.getMetadataNamespaces()
    }

    List<ElementAnnotationTransportCommand> fetchAnnotations(RevisionTransportCommand rev) {
        List<ElementAnnotationTransportCommand> annotations = null
        use(ElementAnnotationCategory) {
            List<RevisionAnnotation>  revisionAnnotations = null
            def values = RevisionAnnotation.where {
                revision.id == rev.id
            }
            revisionAnnotations = values.list()
            annotations = revisionAnnotations.collect {RevisionAnnotation ra ->
                ra.elementAnnotation.toCommandObject()
            }
        }
        annotations
    }

    Map<QualifierTransportCommand, List<ResourceReferenceTransportCommand>> fetchGenericAnnotations(
        RevisionTransportCommand rev) {
        List<StatementTransportCommand> statements = getModelLevelAnnotations(rev)
        Map result = [:]
        statements.each { StatementTransportCommand s ->
            final QualifierTransportCommand qualifier = s.predicate
            final ResourceReferenceTransportCommand xref = s.object
            // ignore the biomodels custom annotation denoting curation status
            // because it is already shown at the curation status line
            boolean isCurationStatus = qualifier.type == "biomodelsCustomAnnotation" &&
                qualifier.uri == "curated"

            boolean isOriginalModel = qualifier.accession == "source"
            if (!isCurationStatus && !isOriginalModel) {
                if (result.containsKey(qualifier)) {
                    result[qualifier] << xref
                } else {
                    result[qualifier] = [xref]
                }
            }
        }
        result
    }

    CurationNotesTransportCommand fetchCurationNotes(RevisionTransportCommand rev) {
        curationNotesService.fetchCurationNotesForModel(rev.model.id)
    }

    /**
     * Returns whether a revision command belongs to a curated or non-curated model.
     *
     * This relies on BioModels' workflow of assigning a publication identifier for
     * curated models.
     *
     * @param rev the {@link RevisionTransportCommand} to check
     * @return the String 'curated' iff the model has been curated, or 'non-curated' otherwise.
     */
    String fetchCurationStatus(RevisionTransportCommand rev) {
        rev.model.publicationId ? "curated" : "non-curated"
    }

    Map<String, String> fetchModellingApproaches(RevisionTransportCommand rev) {
        List<StatementTransportCommand> statements = getModelLevelAnnotations(rev)
        Map result = [:]
        statements.each { StatementTransportCommand s ->
            final ResourceReferenceTransportCommand xref = s.object
            if (MODELLING_APPROACHES.containsKey(xref.accession)) {
                result.put(xref.accession, MODELLING_APPROACHES.get(xref.accession))
            }
        }
        result
    }

    List<String> fetchOriginalModels(RevisionTransportCommand rev) {
        List<StatementTransportCommand> statements = getModelLevelAnnotations(rev)
        List<String> result = []
        statements.each { StatementTransportCommand s ->
            if (s.predicate.accession == "source") {
                result << s.object.uri
            }
        }
        result
    }

    Set<String> fetchModelTags(String modelSubmissionId) {
        modelTagService.getTagsByModelId(modelSubmissionId) as Set
    }

    @Override
    List searchModellingApproach(String searchTerm) {
        metadataService.searchModellingApproach(searchTerm)
    }

    @Override
    ModellingApproach getModellingApproach(String name) {
        metadataService.getModellingApproach(name)
    }

    @Override
    ModellingApproach getModellingApproach(ModelTransportCommand model) {
        return model.modellingApproach
    }

    Set<TagTransportCommand> findTagsByModel(ModelTransportCommand model) {
        modelTagService.findTagsByModel(model)
    }

    private List<StatementTransportCommand> getModelLevelAnnotations(RevisionTransportCommand rev) {
        // By default, fetching generic annotations means to grab model-level annotations
        // The specific levels of annotations should be invoked within another methods
        def modelRAs = RevisionAnnotation.where {
            revision.id == rev.id && elementAnnotation.modelElementType.name == 'model'
        }
        List<RevisionAnnotation> revisionAnnotations = modelRAs.list()
        List<ElementAnnotationTransportCommand> annotations
        use(ElementAnnotationCategory) {
            annotations = revisionAnnotations.collect { RevisionAnnotation ra ->
                ra.elementAnnotation.toCommandObject()
            }
        }
        List<StatementTransportCommand> statements = annotations*.statement
        statements
    }
}
