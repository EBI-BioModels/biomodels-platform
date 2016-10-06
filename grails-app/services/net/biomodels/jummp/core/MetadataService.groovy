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

import eu.ddmore.metadata.service.ValidationError
import eu.ddmore.metadata.service.ValidationErrorStatus
import grails.transaction.Transactional
import net.biomodels.jummp.annotation.CompositeValueContainer
import net.biomodels.jummp.annotation.PropertyContainer
import net.biomodels.jummp.annotation.SectionContainer
import net.biomodels.jummp.annotation.ValueContainer
import net.biomodels.jummp.annotationstore.Qualifier
import net.biomodels.jummp.annotationstore.ResourceReference
import net.biomodels.jummp.annotationstore.Statement
import net.biomodels.jummp.core.adapters.DomainAdapter
import net.biomodels.jummp.core.annotation.StatementTransportCommand
import net.biomodels.jummp.core.model.AnnotationValidationContext
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.commons.validator.UrlValidator
import org.perf4j.aop.Profiled
import org.springframework.transaction.annotation.Isolation

/**
 * Service class that facilitates interaction with model metadata.
 *
 * The service currently provides means of querying the metadata related to a particular
 * revision of a model.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@Transactional(readOnly = true)
class MetadataService {
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    static final String DEFAULT_PHARMML_NAMESPACE = "http://www.pharmml.org/2013/10/PharmMLMetadata"
    static final String DEFAULT_DDMORE_WAT_NAMESPACE =
            "http://www.ddmore.org/ontologies/webannotationtool#"
    static final String DEFAULT_PKPD_NAMESPACE =
            "http://www.ddmore.org/ontologies/ontology/pkpd-ontology#"
    /**
     * Dependency injection for Grails Application.
     */
    def grailsApplication
    /**
     * Dependency injection for Model Service.
     */
    def modelService
    /**
     * Dependency injection for Model Format Service.
     */
    def modelFileFormatService
    def rdfMetadataWriter
    def sbmlMetadataWriter
    def metadataValidator

    /**
     * Fetches any ResourceReferences defined for a given qualifier from a revision.
     *
     * Convenience method for fetching statements that match qualifier @p qName in revision
     * @p rev.
     *
     * @param rev the identifier of an existing revision.
     * @param qName the accession of the desired qualifier.
     * @return a list of ResourceReferences that represent the objects of the statements from
     * the revision denoted by @param rev.
     * @see net.biomodels.jummp.annotationstore.ResourceReference
     * @see net.biomodels.jummp.annotationstore.Statement
     */
    @Profiled(tag = "metadataService.findAllResourceReferencesForQualifier")
    List<ResourceReference> findAllResourceReferencesForQualifier(Long rev, String qName) {
        if (IS_DEBUG_ENABLED) {
            log.debug "Finding cross references with qualifier $qName for revision $rev."
        }
        Revision revision = Revision.load(rev)
        List<ResourceReference> result = ResourceReference.executeQuery('''
                select statement.object
                from Statement statement
                    join statement.qualifier qualifier
                where
                    statement.annotation.revision = ? and
                    qualifier.accession = ?''', [revision, qName])
        if (IS_DEBUG_ENABLED) {
            log.debug "Found ${result.size()} matching annotations."
        }
        return result
    }

    /**
     * Fetches all Statements containing a given qualifier from a revision.
     *
     * Convenience method for fetching statements that match qualifier @p qualifier
     * in revision @p $revisionId.
     *
     * @param revisionId the identifier of an existing revision.
     * @param qualifier the accession of the desired qualifier.
     * @return a list of matching Statements from the revision denoted by @param revisionId.
     * @see net.biomodels.jummp.annotationstore.Statement
     */
    @Profiled(tag = "metadataService.findAllStatementsForQualifier")
    List<Statement> findAllStatementsForQualifier(Long revisionId, String qualifier) {
        if (IS_DEBUG_ENABLED) {
            log.debug "Finding annotations with qualifier $qualifier for revision $revisionId."
        }
        Revision revision = Revision.load(revisionId)
        List<Statement> result = Statement.executeQuery('''select statement
                from Statement statement
                    join statement.qualifier qualifier
                where
                    statement.annotation.revision = :revision and
                    qualifier.accession = :qName''', [revision: revision, qName: qualifier])
        if (IS_DEBUG_ENABLED) {
            log.debug "Found ${result.size()} matching annotations."
        }
        return result
    }

    /**
     * Fetches all Statements containing a given RDF subject from a revision.
     *
     * Convenience method for fetching statements that match subject @p subject
     * in revision @p revisionId.
     *
     * @param revisionId the identifier of an existing revision.
     * @param subject the accession of the desired subject.
     * @return a list of matching Statements from the revision denoted by @param revisionId.
     * @see net.biomodels.jummp.annotationstore.Statement
     */
    @Profiled(tag = "metadataService.findAllStatementsForSubject")
    List<Statement> findAllStatementsForSubject(Long revisionId, String subject) {
        if (IS_DEBUG_ENABLED) {
            log.debug "Finding annotations with subject $subject for revision $revisionId."
        }
        Revision revision = Revision.load(revisionId)
        List<Statement> result = Statement.executeQuery('''
                from Statement statement
                where
                    statement.annotation.revision = :revision and
                    statement.subjectId = :subject''', [revision: revision, subject: subject])
        if (IS_DEBUG_ENABLED) {
            log.debug "Found ${result.size()} matching annotations."
        }
        return result
    }

    /**
     * Fetches any ResourceReferences defined for a given RDF subject from a revision.
     *
     * Convenience method for fetching statements that match subject @p subject in revision
     * @p revisionId.
     *
     * @param revision the identifier of an existing revision.
     * @param subject the accession of the desired subject.
     * @return a list of ResourceReferences that represent the objects of the statements from
     * the revision denoted by @param revisionId.
     * @see net.biomodels.jummp.annotationstore.ResourceReference
     */
    @Profiled(tag = "metadataService.findAllResourceReferencesForSubject")
    List<ResourceReference> findAllResourceReferencesForSubject(Long revisionId, String subject) {
        if (IS_DEBUG_ENABLED) {
            log.debug "Finding cross references with subject $subject for revision $revisionId."
        }
        Revision revision = Revision.load(revisionId)
        List<ResourceReference> result = ResourceReference.executeQuery('''select reference
                from Statement statement
                    join statement.object reference
                where
                    statement.annotation.revision = :revision and
                    statement.subjectId = :subject''', [revision: revision, subject: subject])
        if (IS_DEBUG_ENABLED) {
            log.debug "Found ${result.size()} matching annotations."
        }
        return result
    }

    @Profiled(tag = "metadataService.saveMetadata")
    // this cannot work with isolation level REPEATABLE_READS (MySQL default)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    boolean saveMetadata(String model, List<StatementTransportCommand> statements) {
        Model theModel = Model.findBySubmissionIdOrPublicationId(model, model)
        Revision baseRevision = modelService.getLatestRevision(theModel, false)
        boolean isUpdate = baseRevision.annotations?.size() > 0
        RevisionTransportCommand newRevision = DomainAdapter.getAdapter(baseRevision).toCommandObject()
        newRevision.comment = "Updated model annotations."
        AnnotationValidationContext avc = validateModelRevision(baseRevision,statements)
        newRevision.validationLevel = avc.validationLevel
        newRevision.validationReport = avc.validationReport

        List<RepositoryFileTransportCommand> files = newRevision.files
        try {
            files = executeMetadataSavingStrategy(newRevision, statements, isUpdate)

            def result = modelService.addValidatedRevision(files, [], newRevision)
            if (!result) {
                log.error """\
            Could not update revision ${baseRevision.id} with annotations ${statements.dump()}"""
            }//else{ result.save(flush: true) }
            return result != null
        } catch(Exception e) {
            log.error(e.message, e)
            return false
        }
    }

    List<RepositoryFileTransportCommand> executeMetadataSavingStrategy(
                    RevisionTransportCommand revisionTransportCommand,
                    List<StatementTransportCommand> statementTransportCommands,
                    boolean isUpdate) {
        String format = revisionTransportCommand.format.identifier
        MetadataSavingStrategy strategy = createMetadataSavingStrategyForFormat format
        strategy.marshallAnnotations(revisionTransportCommand, statementTransportCommands, isUpdate)
    }

    private MetadataSavingStrategy createMetadataSavingStrategyForFormat(String format) {
        MetadataSavingStrategy strategy
        switch(format) {
            case "SBML":
                strategy = sbmlMetadataWriter
                break
            case "PharmML":
            default:
                strategy = rdfMetadataWriter
                break
        }
        strategy
    }

    @Profiled(tag="metadataService.validateModelRevision")
    @Transactional
    public AnnotationValidationContext validateModelRevision(Revision revision, List<StatementTransportCommand> statements){

        if (!revision) {
            throw new IllegalArgumentException("Revision may not be null")
        }

        String format = revision.format.identifier
        MetadataSavingStrategy strategy = createMetadataSavingStrategyForFormat format

        RevisionTransportCommand revisionTC = DomainAdapter.getAdapter(revision).toCommandObject()
        def metadataWriter = strategy.createMetadataWriter(revisionTC, statements)

        StringBuffer validationReport = new StringBuffer();

        metadataValidator.validate(metadataWriter.model)

        for(ValidationError validationError: metadataValidator.validationHandler.getValidationList()) {
            if (validationError.errorStatus == ValidationErrorStatus.EMPTY) {
                validationReport.append(getQualifierLabel(validationError.qualifier))
                validationReport.append(" is empty.")
            }else if(validationError.errorStatus == ValidationErrorStatus.INVALID){
                validationReport.append(getQualifierLabel(validationError.qualifier))
                validationReport.append(" ")
                UrlValidator urlValidator = new UrlValidator();
                if(urlValidator.isValid(validationError.getValue())) {
                    validationReport.append(ResourceReference.findByUri(uri).name)
                }else{
                    validationReport.append(validationError.getValue())
                }
                validationReport.append(" is invalid.")
            }
            validationReport.append("<br>")

        }

        AnnotationValidationContext anc = new AnnotationValidationContext();
        anc.validationReport = validationReport.toString();
        anc.validationLevel = metadataValidator.getValidationErrorStatus();

        return anc;

    }

    private String getQualifierLabel(String qualifierString){
        Qualifier qualifier = Qualifier.findByUri(qualifierString);
        if(qualifier!=null) {
            return qualifier.accession;
        }
        else{
            int indexhash = qualifierString.indexOf("#")
            if(indexhash!= -1)
                return qualifierString.substring(indexhash+1)
            else
                return qualifierString
        }
    }

    @Profiled(tag = "metadataService.getMetadataNamespaces")
    List<String> getMetadataNamespaces() {
        return [MetadataService.DEFAULT_DDMORE_WAT_NAMESPACE,
                MetadataService.DEFAULT_PHARMML_NAMESPACE,
                MetadataService.DEFAULT_PKPD_NAMESPACE]
    }

    /**
     * Dumb cache of the annotation schema.
     *
     * Persists in the database the qualifiers and corresponding cross-references that make
     * up the fields and the values rendered by the annotation editor.
     */
    @Profiled(tag = "metadataService.persistAnnotationSchema")
    @Transactional
    boolean persistAnnotationSchema(Collection<SectionContainer> sections) {
        def result = false
        def promise = Qualifier.async.task {
            sections.each { SectionContainer s ->
                def qualifiers = s.annotationProperties
                qualifiers.each { PropertyContainer p ->
                    String uri = p.uri
                    String name = p.value
                    def refreshedQualifier = saveOrUpdateQualifier(uri, name)
                    if (!refreshedQualifier) {
                        result = false
                    }
                    if (!p.values) {
                        return
                    }

                    List<ValueContainer> values = p.values
                    values.each { ValueContainer v ->
                        if (!v.uri) { // ignore xrefs without a URI
                            return
                        }
                        def xref = saveOrUpdateResourceReference(v)
                        if (!xref) {
                            result = false
                        }
                    }
                }
            }
            return result
        }
        promise.onComplete {
            result = true
        }
        promise.onError { Throwable t ->
            log.error("Failed to persist annotation schema in database: ${t.message}", t)
            result = false
        }
        promise.get()
        return result
    }

    /*
     * Saves a new qualifier in the database or updates the existing one.
     *
     * In the case of existing qualifiers, this triggers an update if the qualifier's label
     * does not match @p name.
     * @param uri the URI of the qualifier that should be saved.
     * @param name the label that the qualifier with URI @p uri should have
     * @return the updated Qualifier, or null if there was a validation error.
     */
    @Transactional
    private Qualifier saveOrUpdateQualifier(String uri, String name) {
        def existingQualifier = Qualifier.findByUri(uri)
        if (!existingQualifier) {
            def q = new Qualifier(accession: name, uri: uri)
            if (uri.startsWith(DEFAULT_PHARMML_NAMESPACE) ||
                    uri.startsWith(DEFAULT_DDMORE_WAT_NAMESPACE)) {
                q.namespace = DEFAULT_PHARMML_NAMESPACE
                q.qualifierType = 'pharmML'
            }
            if (!q.save(flush: true)) {
                log.error "Could not save new qualifier with uri $uri: ${q.errors.allErrors}"
            }
            existingQualifier = q
        } else {
            if (existingQualifier.accession != name) {
                existingQualifier.accession = name
                existingQualifier.save(flush: true)
                if (existingQualifier.hasErrors()) {
                    log.error "Could not update qualifier $existingQualifier with new name $name"
                }
            }
        }
        return existingQualifier
    }

    /*
     * Turns a ValueContainer into the appropriate ResourceReference(s) in the database.
     *
     * If there is no corresponding ResourceReference in the database, a new one is created.
     * CompositeValueContainers are converted to nested ResourceReferences.
     *
     * Otherwise, if there is already a ResourceReference with the same URI as @p v, then this
     * method checks that the value of @p v matches the name of the ResourceReference and that
     * all members of @p parents are included in the set of parents of the corresponding
     * ResourceReference, triggering a database update if necessary.
     * @param v the ValueContainer that should be stored in the database
     * @param parents a collection of ResourceReferences that should be stored as parents
     * @return the saved ResourceReference or null if there was an error.
     */
    @Transactional
    private ResourceReference saveOrUpdateResourceReference(ValueContainer v,
            List<ResourceReference> parents = []) {
        String uri = v.uri
        String name = v.value
        def existing = ResourceReference.findByUri(uri)
        if (!existing) {
            existing = new ResourceReference(uri: uri, shortName: name, name: name)
            // work out the accession from the presence of the hash (#) or the last /
            int accessionDelim
            int hash = uri.indexOf('#')
            if (-1 != hash) {
                accessionDelim = hash
            } else {
                accessionDelim = uri.lastIndexOf('/')
            }
            String accession = uri.substring(accessionDelim + 1)
            existing.accession = accession
            if (uri.startsWith(DEFAULT_PKPD_NAMESPACE)) {
                existing.datatype = 'pkpd'
            }
            if (parents) {
                existing.parents.addAll(parents)
            }
            def savedXref = existing.save(flush: true)
            if (!savedXref) {
                log.error """\
Unable to save xref with uri ${v.uri}: ${existing?.errors?.allErrors?.inspect()} - will not \
attempt to save any of its children"""
                return savedXref
            }
            if (v instanceof CompositeValueContainer && v.children) {
                v.children.each { ValueContainer child ->
                    saveOrUpdateResourceReference(child, parents << existing)
                }
            }
            return existing
        } else {
            if (existing.name != name) {
                existing.name = name
                return existing.save(flush: true)
            }
            if (existing.parents != parents) {
                existing.parents.addAll(parents)
                return existing.save(flush: true)
            }
            return existing
        }
    }
}
