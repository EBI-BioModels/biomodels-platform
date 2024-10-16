/**
* Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.webapp.rest.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import grails.util.Holders
import net.biomodels.jummp.core.annotation.QualifierTransportCommand as QualifierTC
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand as RRTC
import net.biomodels.jummp.core.annotation.StatementTransportCommand as STC
import net.biomodels.jummp.core.model.ContributorDto
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.user.PersonTransportCommand as PersonTC
import net.biomodels.jummp.deployment.biomodels.TagTransportCommand as TagTC

class Model {
    String name
    String description
    Format format
    Publication publication
    ModelFiles files
    History history
    Long firstPublished // seconds in Unix epoch
    /** perennial model identifiers */
    String submissionId
    String publicationId
    ModellingApproach modellingApproach
    String curationStatus
    List<String> modelTags
    Map<String, Set<ContributorDto>> contributors
    String vcsIdentifier
    List<Annotation> modelLevelAnnotations

    Model(RTC revision, boolean isPrivate) {
        MTC model = revision.model
        submissionId = model.submissionId
        name = revision.name
        description = revision.description
        format = new Format(revision.format)
        if (model.modellingApproach) {
            modellingApproach = new ModellingApproach(model.modellingApproach)
        }
        if (!isPrivate) {
            if (model.publication) {
                PubTC pubTC = model.publication
                publication = new Publication(pubTC)
                pubTC.authors.each { PersonTC personTC ->
                    publication.authors << new PublicationAuthor(personTC)
                }
            }
            files = new ModelFiles(revision.files.findAll { !it.hidden })
            history = new History(model.submissionId)
            publicationId = model.publicationId
            firstPublished = model.firstPublished.getTime()/1_000 as Long

            curationStatus = revision.curationState.toString() //revision.curationState.name()
            def mdds = Holders.grailsApplication.mainContext.getBean("metadataDelegateService")
            Set<TagTC> tags = mdds.findTagsByModel(revision.model)
            List<String> tagList = tags.collect { it.name }
            modelTags = tagList

            def mds = Holders.grailsApplication.mainContext.getBean("modelDelegateService")
            contributors = mds.buildDetailedContributors(revision.contributors)
            vcsIdentifier = mds.getRevisionsState(revision.model.submissionId)["vcsId"]
            modelLevelAnnotations = retrieveModelLevelAnnotations(revision, mdds)
        } else {
            files = new ModelFiles()
        }
    }

    /*private List<Annotation> getModelLevelAnnotations(final RTC revision, def redisService, def metaDS) {
        List<Annotation> result = redisService
    }*/

    private static List<Annotation> retrieveModelLevelAnnotations(final RTC revision, def metaDS) {
        List<STC> modelLevelAnnotations = metaDS.getModelLevelAnnotations(revision)
        Map<QualifierTC, List<RRTC>> genericAnnotations = metaDS.fetchGenericAnnotations(modelLevelAnnotations)
        List<Annotation> result = new ArrayList<>()
        genericAnnotations.each { QualifierTC qualifierTC, List<RRTC> references ->
            String type = "unknown"
            if (qualifierTC.type == "ModelQualifier" ||
                qualifierTC.type == "http://biomodels.net/model-qualifiers/") {
                type = "bqmodel"
            } else if (qualifierTC.type == "BiologicalQualifier" ||
                qualifierTC.type == "http://biomodels.net/biology-qualifiers/") {
                type = "bqbiol"
            }
            String qualifier = qualifierTC.accession
            if (type != "unknown") {
                qualifier = type + ":" + qualifierTC.accession
            }
            references.each { RRTC ref ->
                Annotation annotation = new Annotation(qualifier: qualifier, name: ref.name,
                    accession: ref.accession, resource: ref.collectionName, uri: ref.uri)
                result.add(annotation)
            }
        }
        return result
    }

    String outputModelAsString(String contentType) {
        // the contentType is either "application/json" or "application/xml"
        ObjectMapper mapper = contentType == "application/json" ? new ObjectMapper() : new XmlMapper()
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        String result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
        result
    }
}
