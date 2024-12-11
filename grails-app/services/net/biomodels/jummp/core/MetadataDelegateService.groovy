/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
import net.biomodels.jummp.annotationstore.ElementAnnotation
import net.biomodels.jummp.annotationstore.ResourceReference
import net.biomodels.jummp.annotationstore.RevisionAnnotation
import net.biomodels.jummp.annotationstore.Statement
import net.biomodels.jummp.core.annotation.ElementAnnotationCategory
import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand as EATC
import net.biomodels.jummp.core.annotation.QualifierTransportCommand as QualifierTC
import net.biomodels.jummp.core.annotation.ResourceReferenceCategory
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand as RRTC
import net.biomodels.jummp.core.annotation.StatementCategory
import net.biomodels.jummp.core.annotation.StatementTransportCommand as STC
import net.biomodels.jummp.core.model.AnnotationValidationContext
import net.biomodels.jummp.core.model.ModelTransportCommand as ModelTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC
import net.biomodels.jummp.deployment.biomodels.CurationNotesTransportCommand as CNTC
import net.biomodels.jummp.deployment.biomodels.TagTransportCommand as TagTC
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.Revision
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled
import org.springframework.beans.factory.InitializingBean

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
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class MetadataDelegateService implements IMetadataService, InitializingBean {
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
    def redisService
    /**
     * {@inheritDoc}
     */
    @Profiled(tag = "metadataDelegateService.findAllResourceReferencesForQualifier")
    List<RRTC> findAllResourceReferencesForQualifier(RevisionTC revision, String qualifier) {
        List<ResourceReference> references = metadataService.
            findAllResourceReferencesForQualifier(revision.id, qualifier)
        wrapResourceReferences(references)
    }

    /**
     * {@inheritDoc}
     */
    @Profiled(tag = "metadataDelegateService.findAllResourceReferencesForSubject")
    List<RRTC> findAllResourceReferencesForSubject(RevisionTC revision, String subject) {
        List<ResourceReference> references = metadataService.
            findAllResourceReferencesForSubject(revision.id, subject)
        wrapResourceReferences(references)
    }

    /**
     * {@inheritDoc}
     */
    @Profiled(tag = "metadataDelegateService.findAllStatementsForQualifier")
    List<STC> findAllStatementsForQualifier(RevisionTC revision, String qualifier) {
        List<Statement> statements = metadataService.findAllStatementsForQualifier(revision.id, qualifier)
        wrapStatements(statements)
    }

    /**
     * {@inheritDoc}
     */
    @Profiled(tag = "metadataDelegateService.findAllStatementsForSubject")
    List<STC> findAllStatementsForSubject(RevisionTC revision, String subject) {
        List<Statement> statements = metadataService.findAllStatementsForSubject(revision.id, subject)
        wrapStatements(statements)
    }

    @Profiled(tag = "metadataDelegateService.wrapResourceReferences")
    private List<RRTC> wrapResourceReferences(
            List<ResourceReference> references) {
        use(ResourceReferenceCategory) {
            return references.collect { ResourceReference r ->
                r.toCommandObject()
            }
        }
    }

    @Profiled(tag = "metadataDelegateService.wrapStatements")
    private List<STC> wrapStatements(List<Statement> statements) {
        use(StatementCategory) {
            return statements.collect { Statement s ->
                s.toCommandObject()
            }
        }
    }

    @Profiled(tag = "metadataDelegateService.saveMetadata")
    boolean saveMetadata(String model, List<STC> statements) {
        metadataService.saveMetadata(model, statements)
    }

    @Profiled(tag = "metadataDelegateService.getPathwaysForModelId")
    List<String> getPathwaysForModelId(String modelId) {
        metadataService.getPathwaysForModelId(modelId)
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
    AnnotationValidationContext validateModelRevision(RevisionTC revision, List<STC> statements) {
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

    /**
     * Caches annotations and organism of a given model revision on Redis server
     * @param revisionTC
     * @return the list of statements
     */
    Map<String, String> cacheAnnotationsAndOrganismOnRedis(final RevisionTC revisionTC) {
        Map<String, String> mapResult = stringifyAnnotations(revisionTC)
        String strOfAnnotations = mapResult.get("annotations")
        redisService.doRedisHSet(revisionTC.model.submissionId, "annotations", strOfAnnotations)
        if (revisionTC.model.publicationId) {
            redisService.doRedisHSet(revisionTC.model.publicationId, "annotations", strOfAnnotations)
        }
        if (mapResult.containsKey("organism")) {
            String organism = mapResult.get("organism")
            redisService.doRedisHSet(revisionTC.model.submissionId, "organism", organism)
            if (revisionTC.model.publicationId) {
                redisService.doRedisHSet(revisionTC.model.publicationId, "organism", organism)
            }
        }

        return mapResult
    }

    /**
     * Converts the list of {@link EATC} objects to a map of strings. It is used for REST API calls and caches.
     * @param revisionTC a {@link RevisionTC} object
     * @return a map
     */
    Map<String, String> stringifyAnnotations(final RevisionTC revisionTC) {
        List<EATC> annotations = fetchAnnotations(revisionTC)
        List statements = annotations*.statement
        statements = statements.unique { it.object.uri }
        String hasTaxon = ""
        String strOfAnnotations = ""
        Map mapResult = new HashMap()
        statements.each {
            if (it.predicate.accession == "hasTaxon" && it.object.datatype ==
                    "taxonomy") {
                hasTaxon = "${it.object.accession}|${it.object.name}|${it.object.uri}"
            }
            strOfAnnotations += """${it.predicate.accession}\t${it.object.datatype} \t${it.object.accession}\t${it.
                    object.uri}\t${it.object.name}|"""
        }

        // remove the last pile - vertical line
        if (strOfAnnotations) {
            strOfAnnotations = strOfAnnotations.substring(0, strOfAnnotations.length() - 1)
        }
        mapResult.put("annotations", strOfAnnotations)
        if (hasTaxon) {
            mapResult.put("organism", hasTaxon)
        }
        return mapResult
    }

    /**
     * Gets the annotations and organism from Redis Cache Server
     * @param modelId
     * @return
     */
    Map<String, String> getAnnotationsAndOrganismFromRedis(final String modelId) {
        Map<String, String> mapResult = new HashMap<>()
        String organism = redisService.doRedisHGet(modelId, "organism")
        if (organism) {
            mapResult.put("organism", organism)
        }
        String strOfAnnotations = redisService.doRedisHGet(modelId, "annotations")
        if (strOfAnnotations) {
            mapResult.put("annotations", strOfAnnotations)
        }
        return mapResult
    }

    List<EATC> fetchAnnotations(final RevisionTC revisionTC) {
        fetchAnnotations(revisionTC.id)
    }

    List<EATC> fetchAnnotations(final Long revId) {
        List<EATC> annotations = null
        use(ElementAnnotationCategory) {
            List<RevisionAnnotation>  revisionAnnotations = null
            def values = RevisionAnnotation.where {
                revision.id == revId
            }
            revisionAnnotations = values.list()
            annotations = revisionAnnotations.collect {RevisionAnnotation ra ->
                ra.elementAnnotation.toCommandObject()
            }
        }
        annotations
    }

    Map<QualifierTC, List<RRTC>> fetchGenericAnnotations(RevisionTC rev) {
        List<STC> statements = getModelLevelAnnotations(rev)
        fetchGenericAnnotations(statements)
    }

    Map<QualifierTC, List<RRTC>> fetchGenericAnnotations(final List<STC> statements) {
        Map result = [:]
        statements.each { STC s ->
            final QualifierTC qualifier = s.predicate
            final RRTC xref = s.object
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
        result as Map<QualifierTC, List<RRTC>>
    }

    CNTC fetchCurationNotes(RevisionTC rev) {
        curationNotesService.fetchCurationNotesForModel(rev.model.id)
    }

    /**
     * Returns whether a revision command belongs to a curated or non-curated model.
     *
     * This relies on BioModels' workflow of assigning a publication identifier for
     * curated models.
     *
     * @param rev the {@link RevisionTC} to check
     * @return the String 'curated' iff the model has been curated, or 'non-curated' otherwise.
     */
    String fetchCurationStatus(RevisionTC rev) {
        rev.model.publicationId ? "curated" : "non-curated"
    }

    Map<String, String[]> fetchModellingApproaches(RevisionTC rev) {
        ModellingApproach modellingApproach =  rev.model.modellingApproach
        Map result = [:]
        if (modellingApproach) {
            result.put(modellingApproach.accession, [modellingApproach.name, modellingApproach.resource] as String[])
        }
        return result
    }

    List<String> fetchOriginalModels(RevisionTC rev) {
        List<STC> statements = getModelLevelAnnotations(rev)
        fetchOriginalModels(statements)
    }

    List<String> fetchOriginalModels(List<STC> statements) {
        List<String> result = []
        statements.each { STC s ->
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
    ModellingApproach getModellingApproach(ModelTC model) {
        return model.modellingApproach
    }

    Set<TagTC> findTagsByModel(ModelTC model) {
        modelTagService.findTagsByModel(model)
    }

    List<STC> getModelLevelAnnotations(RevisionTC rev) {
        getModelLevelAnnotations(rev?.id)
    }

    List<STC> getModelLevelAnnotations(long revisionId) {
        // By default, fetching generic annotations means to grab model-level annotations
        // The specific levels of annotations should be invoked within another methods
        if (!revisionId) {
            return null
        }
        def modelRAs = RevisionAnnotation.where {
            revision.id == revisionId && elementAnnotation.modelElementType.name == 'model'
        }
        List<RevisionAnnotation> revisionAnnotations = modelRAs.list()
        List<ElementAnnotation> annotationList = revisionAnnotations.collect { RevisionAnnotation ra ->
            ra.elementAnnotation
        }
        List<EATC> annotations = new ArrayList<>()
        annotationList.collect { ElementAnnotation ea ->
            use(ElementAnnotationCategory) {
                annotations.add(ea.toCommandObject())
            }
        }
        List<STC> statements = annotations.collect {
            it.statement
        }
        statements
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Finished the bean initialisation")
    }
}
