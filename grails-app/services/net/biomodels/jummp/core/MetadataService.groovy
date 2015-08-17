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

import net.biomodels.jummp.annotationstore.*
import net.biomodels.jummp.model.Revision
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import org.perf4j.aop.Profiled

/**
 * Service class that facilitates interaction with model metadata.
 *
 * The service currently provides means of querying the metadata related to a particular
 * revision of a model.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class MetadataService {
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

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
}
