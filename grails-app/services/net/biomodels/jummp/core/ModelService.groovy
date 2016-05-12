/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Lucene, Apache Commons, Perf4j, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Lucene, Apache Commons, Perf4j, Spring Security used as well as
* that of the covered work.}
**/

package net.biomodels.jummp.core

import eu.ddmore.publish.service.PublishContext
import eu.ddmore.publish.service.PublishException
import eu.ddmore.publish.service.PublishInfo
import grails.transaction.Transactional
import net.biomodels.jummp.annotationstore.Qualifier
import net.biomodels.jummp.annotationstore.ResourceReference
import net.biomodels.jummp.annotationstore.Statement
import net.biomodels.jummp.core.adapters.DomainAdapter
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.events.LoggingEventType
import net.biomodels.jummp.core.events.ModelCreatedEvent
import net.biomodels.jummp.core.events.PostLogging
import net.biomodels.jummp.core.events.RevisionCreatedEvent
import net.biomodels.jummp.core.model.ModelAuditTransportCommand
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PermissionTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.model.ValidationState
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.core.vcs.VcsFileDetails
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelAudit
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.RepositoryFile
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.perf4j.aop.Profiled
import org.perf4j.log4j.Log4JStopWatch
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.Acl
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation

/**
 * @short Service class for managing Models
 *
 * This service provides the high-level API to access models and their revisions.
 * It is recommended to use this service instead of accessing Models and Revisions
 * directly through GORM. The service methods respect the ACL on the objects and by
 * that ensures that no user can perform actions he is not allowed to.
 * @see Model
 * @see Revision
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Sarala Wimalaratne <sarala@ebi.ac.uk>
 * @date 20151014
 */
@SuppressWarnings("GroovyUnusedCatchParameter")
@Transactional
class ModelService {
    /**
     * The class logger.
     */
    private static final Log log = LogFactory.getLog(this)
    /**
     * Threshold for the verbosity of the logger.
     */
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Threshold for the verbosity of the logger.
     */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Dependency Injection of Spring Security Service
     */
    def springSecurityService
    /**
     * Dependency Injection of AclUtilService
     */
    def aclUtilService
    /**
     * Dependency Injection of VcsService
     */
    def vcsService
    /**
     * Dependency Injection of ModelFileFormatService
     */
    def modelFileFormatService
    /**
     * Dependency Injection for GrailsApplication
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication
    /**
     * Dependency Injection for PubMedService
     */
    def pubMedService
    /**
     * Dependency injection of ModelHistoryService
     */
    def modelHistoryService
    /**
     * Dependency Injection of FileSystemService
     */
    def fileSystemService
    /**
     * Dependency Injection of userService
     */
    def userService
    /**
     * Dependency injection of submissionIdGenerator
     */
    def submissionIdGenerator
    /**
     * Dependency injection of publicationIdGenerator
     */
    def publicationIdGenerator

    def publishValidator

    final boolean MAKE_PUBLICATION_ID = !(publicationIdGenerator instanceof NullModelIdentifierGenerator)

    /**
    * Returns list of Models the user has access to.
    *
    * Searches for all Models the current user has access to, that is @ref getLatestRevision
    * does not return @c null for any Model in the returned list.
    * This method provides pagination.
    * @param offset Offset in the list
    * @param count Number of models to return
    * @param sortOrder @c true for ascending, @c false for descending
    * @param sortColumn the column which should be sorted
    * @param filter Optional filter for search
    * @return List of Models
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getAllModels")
    public List<Model> getAllModels(int offset, int count, boolean sortOrder, ModelListSorting sortColumn,
                String filter = null, boolean deletedOnly=false) {
        if (offset < 0 || count <= 0) {
            // safety check
            return []
        }

        String sortingDirection = sortOrder ? 'asc' : 'desc'

        boolean filterIsValid = filterValid(filter)

        Map metaParams = [
            max: count, offset: offset
        ]

        Map namedParams = [:]
        if (filterIsValid) {
            namedParams.put("filter", "%${filter.toLowerCase()}%");
        }

        String query
        // for Admin - sees all (not deleted) models
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            query = getQueryForAdmin(sortColumn, deletedOnly, filterIsValid, sortingDirection)

        } else {
            Set<String> roles = getSpringDatabaseRoles()

            query = getQueryForUser(sortColumn, deletedOnly, filterIsValid, sortingDirection)
            namedParams += [
                className  :  Revision.class.getName(),
                permissions: [BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()],
                roles      :  roles
            ]
        }
        List<List<Model, String, Date, String, Long, String>> resultSet = Model.executeQuery(query, namedParams, metaParams)
        return resultSet.collect{ it.first() }
    }

    private String getQueryForUser(ModelListSorting sortColumn, boolean deletedOnly, boolean filterIsValid, String sortingDirection) {
        String query = '''
SELECT DISTINCT m, r.name, r.uploadDate, r.format.name, m.id, u.person.userRealName
FROM Revision AS r
JOIN r.model AS m
JOIN r.owner as u
WHERE r.deleted = false
'''
//do we want to show information from the latest revision?
        if (sortColumn == ModelListSorting.LAST_MODIFIED || sortColumn == ModelListSorting.FORMAT || sortColumn == ModelListSorting.NAME) {
            query += '''AND r.revisionNumber=(SELECT MAX(r2.revisionNumber) from Revision r2,
                        AclEntry ace2  where r.model=r2.model
                        AND r2.id=ace2.aclObjectIdentity.objectId
                        AND ace2.aclObjectIdentity.aclClass.className = :className
                        AND ace2.sid.sid IN (:roles) AND ace2.mask IN (:permissions)
                        AND ace2.granting = true)'''
        } else {
            ////otherwise sortColumn must be the following .. ie we want to sort by the first revision (sortColumn==ModelListSorting.SUBMITTER || sortColumn==ModelListSorting.SUBMISSION_DATE)
            query += '''AND r.revisionNumber=(SELECT MIN(r2.revisionNumber) from Revision r2,
                        AclEntry ace2  where r.model=r2.model
                        AND r2.id=ace2.aclObjectIdentity.objectId
                        AND ace2.aclObjectIdentity.aclClass.className = :className
                        AND ace2.sid.sid IN (:roles) AND ace2.mask IN (:permissions)
                        AND ace2.granting = true)'''
        }

        query += " AND m.deleted = ${deletedOnly} "
        if (filterIsValid) {
            query += '''
AND (
lower(m.publication.journal) like :filter
OR lower(m.publication.title) like :filter
OR lower(m.publication.affiliation) like :filter
)
'''
        }
        query += '''
ORDER BY
'''
        query += " " + getSortColumnAsString(sortColumn) + " " + sortingDirection
        return query
    }

    private String getQueryForAdmin(ModelListSorting sortColumn, boolean deletedOnly, boolean filterIsValid, String sortingDirection) {
        String query = '''
SELECT DISTINCT m, r.name, r.uploadDate, r.format.name, m.id, u.person.userRealName
FROM Revision AS r
JOIN r.model AS m JOIN r.owner as u
WHERE
'''
        if (sortColumn == ModelListSorting.LAST_MODIFIED || sortColumn == ModelListSorting.FORMAT ||
            sortColumn == ModelListSorting.NAME) {
            query += '''r.revisionNumber=(SELECT MAX(r2.revisionNumber) from Revision r2 where r.model=r2.model) AND '''
        } else if (sortColumn == ModelListSorting.SUBMITTER || sortColumn == ModelListSorting.SUBMISSION_DATE) {
            query += '''r.revisionNumber=(SELECT MIN(r2.revisionNumber) from Revision r2 where r.model=r2.model) AND '''
        }
        query += "m.deleted = ${deletedOnly} AND r.deleted = false"
        if (filterIsValid) {
            query += '''
AND (
lower(m.publication.journal) like :filter
OR lower(m.publication.title) like :filter
OR lower(m.publication.affiliation) like :filter
)
'''
        }
        query += '''
ORDER BY
'''
        query += " " + getSortColumnAsString(sortColumn) + " " + sortingDirection
        return query
    }

    /**
     * Short method to get the SQL-name of the {@Link ModelListSorting} enum
     * @param sortColumn
     * @return
     */
    // ToDo: this should really be enum-properties in the ModelListSorting enum itself..
    private java.lang.String getSortColumnAsString(ModelListSorting sortColumn) {
        String result
        switch (sortColumn) {
            case ModelListSorting.NAME:
                result = "r.name"
                break
            case ModelListSorting.LAST_MODIFIED:
                result = "r.uploadDate"
                break
            case ModelListSorting.FORMAT:
                result = "r.format.name"
                break
            case ModelListSorting.SUBMITTER:
                result = "u.person.userRealName"
                break
            case ModelListSorting.SUBMISSION_DATE:
                /*
                 * Hard to get to model submission date directly. However as model ids
                 * are sequentially generated, they are used as a surrogate.
                 */
                result = "m.id"
                break
            case ModelListSorting.PUBLICATION:
                // TODO: implement, fall through to default
            case ModelListSorting.ID: // Id is the default
            default:
                result = "m.id"
                break
        }
        result
    }

    /**
    * Convenient method for sorting by the id column.
    *
    * @return List of Models sorted ascending
    * @see ModelService#getAllModels(int offset, int count, boolean sortOrder)
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getAllModels")
    public List<Model> getAllModels(int offset, int count, boolean sortOrder) {
        getAllModels(offset, count, sortOrder, ModelListSorting.ID)
    }

    /**
    * Convenient method for ascending sorting.
    *
    * @return List of Models sorted ascending by @p sortColumn
    * @see ModelService#getAllModels(int offset, int count, boolean sortOrder)
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getAllModels")
    public List<Model> getAllModels(int offset, int count, ModelListSorting sortColumn) {
        return getAllModels(offset, count, true, sortColumn)
    }

    /**
    * Convenient method for ascending sorting by id.
    *
    * @return List of Models sorted ascending by id
    * @see ModelService#getAllModels(int offset, int count, boolean sortOrder)
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getAllModels")
    public List<Model> getAllModels(int offset, int count) {
        return getAllModels(offset, count, ModelListSorting.ID)
    }

    /**
    * Convenient method for ascending sorting of first ten models.
    *
    * @param sortColumn the column which should be sorted
    * @return List of first 10 Models sorted ascending by @p sortColumn
    * @see ModelService#getAllModels(int offset, int count, boolean sortOrder)
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getAllModels")
    public List<Model> getAllModels(ModelListSorting sortColumn) {
        return getAllModels(0, 10, true, sortColumn)
    }

    /**
    * Convenient method for ascending sorting of first ten models by id.
    *
    * @return List of first 10 Models sorted ascending by id
    * @see ModelService#getAllModels(int offset, int count, boolean sortOrder)
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getAllModels")
    public List<Model> getAllModels() {
        return getAllModels(ModelListSorting.ID)
    }

    /**
    * Returns the number of Models the user has access to.
    *
    * @param filter Optional filter for search
    * @see ModelService#getAllModels()
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getModelCount")
    public Integer getModelCount(String filter = null, boolean deletedOnly = false) {
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            // special handling for Admin - is allowed to see all (not deleted) Models
            def criteria = Model.createCriteria()
            return criteria.get {
                ne("deleted", !deletedOnly)
                if (filterValid(filter)) {
                    or {
                        ilike("name", "%${filter}%")
                        publication {
                            or {
                                ilike("journal", "%${filter}%")
                                ilike("title", "%${filter}%")
                                ilike("affiliation", "%${filter}%")
                            }
                        }
                    }
                }
                projections {
                    count("id")
                }
            } as Integer
        }

        Set<String> roles = getSpringDatabaseRoles()

        String query = '''
SELECT COUNT(DISTINCT m.id) FROM Revision AS r, AclEntry AS ace
JOIN r.model AS m
JOIN ace.aclObjectIdentity AS aoi
JOIN aoi.aclClass AS ac
JOIN ace.sid AS sid
WHERE
aoi.objectId = r.id
AND ac.className = :className
AND sid.sid IN (:roles)
AND ace.mask IN (:permissions)
AND ace.granting = true
AND r.deleted = false
'''
query+=" AND m.deleted=${deletedOnly} "
        if (filterValid(filter)) {
            query += '''
AND (
lower(m.publication.journal) like :filter
OR lower(m.publication.title) like :filter
OR lower(m.publication.affiliation) like :filter
)
'''
        }
        Map params = [
            className: Revision.class.getName(),
            permissions: [BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()],
            roles: roles]
        if (filterValid(filter)) {
            params.put("filter", "%${filter.toLowerCase()}%");
        }

        return Model.executeQuery(query, params)[0] as Integer
    }

    /** convenience method to check if our filter is OK */
    private java.lang.Boolean filterValid(String filter) {
        return filter && filter.length() >= 3
    }

    /**
     * Returns the Model identified by perennial identifier @p id.
     * @param id The perennial id of the model.
     * @return The Model
     */
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getModel")
    public Model getModel(String id) {
        Model model = ModelAdapter.findByPerennialIdentifier(id)
        if (model) {
            if (!getLatestRevision(model)) {
                throw new AccessDeniedException("No access to Model with Id ${id}")
            }
        } else {
            throw new AccessDeniedException("No access to Model with Id ${id}")
        }
        return model
    }

    /**
    * Queries the @p model for the latest available revision the user has read access to.
    * @param model The Model for which the latest revision should be retrieved.
    * @param addToHistory Optional field to allow history not to be modified - e.g. if called from modelhistoryService
    * @return Latest Revision the current user has read access to. If there is no such revision null is returned
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getLatestRevision")
    public Revision getLatestRevision(Model model, boolean addToHistory = true) {
        if (!model) {
            return null
        }
        /*if (model.deleted) {
            // exclude deleted models
            return null
        }*/
        // admin gets max (non deleted) revision
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            List<Long> result = Revision.executeQuery('''
                SELECT rev.id
                FROM Revision AS rev
                JOIN rev.model.revisions AS revisions
                WHERE
                rev.model = :model
                AND revisions.deleted = false
                GROUP BY rev.model, rev.id, rev.revisionNumber
                HAVING rev.revisionNumber = max(revisions.revisionNumber)''', [model: model]) as List
            if (!result) {
                return null
            }
//            modelHistoryService.addModelToHistory(model)
            return Revision.get(result[0])
        }

        Set<String> roles = getSpringDatabaseRoles()
        List<Long> result = Revision.executeQuery('''
SELECT rev.id
FROM Revision AS rev, AclEntry AS ace
JOIN rev.model.revisions AS revisions
JOIN ace.aclObjectIdentity AS aoi
JOIN aoi.aclClass AS ac
JOIN ace.sid AS sid
WHERE
rev.model = :model
AND revisions.deleted = false
AND aoi.objectId = revisions.id
AND ac.className = :className
AND sid.sid IN (:roles)
AND ace.mask IN (:permissions)
AND ace.granting = true
GROUP BY rev.model, rev.id, rev.revisionNumber
HAVING rev.revisionNumber = max(revisions.revisionNumber)''', [
                model: model,
                className: Revision.class.getName(),
                roles: roles,
                permissions: [BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()] ]) as List
            if (!result) {
                return null
            }
            if (addToHistory) {
            	modelHistoryService.addModelToHistory(model)
            }
            return Revision.get(result[0])
    }

    /** deduplication method for getting Spring's authorities as Database-Role strings */
    private Set<String> getSpringDatabaseRoles() {
        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add(getUsername())
        }
        return roles
    }

    /**
    * Queries the @p model for all revisions the user has read access to.
    * The returned list is ordered by revision number of the model.
    * @param model The Model for which all revisions should be retrieved
    * @return List of Revisions ordered by revision numbers of underlying VCS.
    * If the user has no access to any revision an empty list is returned
    * @todo: add paginated version with offset and count. Problem: filter
    **/
    @PostFilter("hasPermission(filterObject, read) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getAllRevisions")
    public List<Revision> getAllRevisions(Model model) {
        /*if (model.deleted) {
            return []
        }*/
        // exclude deleted revisions
        modelHistoryService.addModelToHistory(model)
        return model.revisions.toList().findAll { !it.deleted }.sort {it.revisionNumber}
    }

    /**
     * Parses the @p identifier to query for a model and optionally
     * a revision number, separated by the . character. If no revision
     * is specified the latest revision is returned.
     * @param identifier The identifier in the format Model.Revision
     * @return The revision or @c null if there is no such revision
     */
    /*
     * The authorisation has been disabled because model.findByPerennialIdentifier calls
     * this method indirectly.
     */
    //@PostAuthorize("hasPermission(returnObject, read) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getRevisionByIdentifier")
    public Revision getRevision(String identifier) {
        String[] parts = identifier.split("\\.")
        String modelId = parts[0]
        Model model = ModelAdapter.findByPerennialIdentifier(modelId)
        if (parts.length == 1) {
            Revision revision = getLatestRevision(model)
            if (!revision) {
                throw new AccessDeniedException("Sorry you are not allowed to access this Model.")
            }
            return revision
        }
        return getRevision(model, Integer.parseInt(parts[1]))
    }

    /**
     * Queries the @p model for the revision with @p revisionNumber.
     * If there is no such revision @c null will be returned. The
     * returned object is filtered against the ACL.
     * @param model The Model to which the revision belongs
     * @param revisionNumber The revision number in context of the Model
     * @return The revision or @c null if there is no such revision
     */
    @PostAuthorize("hasPermission(returnObject, read) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getRevision")
    public Revision getRevision(Model model, int revisionNumber) {
        Revision revision = Revision.findByRevisionNumberAndModel(revisionNumber, model)
        if (!revision || revision.deleted /*|| model.deleted */) {
            throw new AccessDeniedException("Sorry you are not allowed to access this Model.")
        } else {
            modelHistoryService.addModelToHistory(model)
            revision.refresh()
            return revision
        }
    }

    /**
     * Returns the reference publication of this @p model.
     * @param model The Model for which the reference publication should be returned.
     * @return The reference publication
     * @throws IllegalArgumentException if @p model is null
     * @throws AccessDeniedException if the current user is not allowed to access at least one Model Revision
     */
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getPublication")
    public Publication getPublication(final Model model) throws AccessDeniedException, IllegalArgumentException {
        if (!model) {
            throw new IllegalArgumentException("Model may not be null")
        }
        if (!getLatestRevision(model, false)) {
            throw new AccessDeniedException("You are not allowed to view Model with id ${model.id}")
        }
        return model.publication
    }

    /**
    * Creates a new Model and stores it in the VCS.
    *
    * Stores the @p repoFile as a new file in the VCS and creates a Model for it.
    * The Model will have one Revision attached to it. The MetaInformation for this
    * Model is taken from @p meta. The user who uploads the Model becomes the owner of
    * this Model. The new Model is not visible to anyone except the owner.
    * @param repoFile The wrapper for the model file that will be stored in the VCS.
    * @param meta Meta Information to be added to the model
    * @return The newly-created Model, or null if the model could not be created
    * @throws ModelException If Model File is not valid or the Model could not be stored in VCS
    **/
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostLogging(LoggingEventType.CREATION)
    @Profiled(tag="modelService.uploadModelAsFile")
    public Model uploadModelAsFile(final RepositoryFileTransportCommand repoFile, ModelTransportCommand meta)
            throws ModelException {
        if (repoFile) {
           return uploadModelAsList([repoFile], meta)
        }
        log.error("No file provided during the update of ${meta.properties}")
        throw new ModelException(meta, "The new version of the model does not have any files.")
    }

    private List<File> getFilesFromRF(List<RepositoryFileTransportCommand> files) {
        List<File> modelFiles = []
        if (files) {
            for (rf in files) {
                final def f = new File(rf.path)
                modelFiles.add(f)
            }
        }
        return modelFiles
    }

    /**
    * Adds a new Revision to the model, to be used by SubmissionService
    * The provided @p modelFiles will be stored in the VCS as an update to the existing files of the same @p model.
    * A new Revision will be created and appended to the list of Revisions of the @p model.
    * The revision will not be validated, as the checks are assumed to have been conducted
    * already
    * @param model The Model the revision should be added
    * @param modelFiles The model files to be stored in the VCS as a new revision
    * @param format The format of the model files
    * @param comment The commit message for the new revision
    * @return The newly-added Revision. In case an error occurred while accessing the VCS @c null will be returned.
    * @throws ModelException If either @p model, @p modelFiles or @p comment are null or if the files do not exist or are directories.
    **/
//    @PreAuthorize("hasPermission(#model, write) or hasRole('ROLE_ADMIN')")
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.addValidatedRevision")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Revision addValidatedRevision(final List<RepositoryFileTransportCommand> repoFiles,
                final List<RepositoryFileTransportCommand> deleteFiles, RevisionTransportCommand rev) throws
                ModelException {
        Revision revision
        def txDefinition = [
            // this tx will use a different session than the current one
            propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW
        ]
        Revision.withTransaction(txDefinition) {
            // the returned revision is detached from the Hibernate session
            revision = doAddValidatedRevision(repoFiles, deleteFiles, rev)
        }
        if (revision) {
            /*
             * Force the new revision to be fetched from the database.
             *
             * With the default MySQL transaction isolation level, the following query would
             * return null because within the current tx we're already loaded the model's
             * revisions and REPEATABLE_READS means that we're always going to get the same
             * result within the same tx (in order to avoid dirty reads).
             * Here there is no risk of dirty reads, it's actually desired behaviour, so
             * we need isolation level READ_COMMITTED.
             *
             * Use eager loading for the model and the format because we expect them to be
             * unproxied downstream when we're creating the revision transport command.
             */
            def attachedRevision = Revision.findByModelAndRevisionNumber(revision.model,
                revision.revisionNumber, [fetch: [model: "eager", format: 'eager']])

            def revisionAdapter = DomainAdapter.getAdapter(attachedRevision)
            RevisionTransportCommand cmd = revisionAdapter.toCommandObject()
            // can't inject searchService -- cyclic dependency
            def searchService = grailsApplication.mainContext.searchService
            searchService.updateIndex(cmd)
        }
        revision
    }

    /**
     * Persists a new model revision in the database.
     *
     * This unit of work is performed in a dedicated transaction.so as to ensure that it is
     * committed before the [synchronous] indexing process tries to load the revision from the
     * database.
     * @param repoFiles the files of the revision
     * @param deleteFiles the files that should be deleted compared to the previous revision
     * @param rev the transport command from which to construct the new revision
     * @return the new revision
     * @throws ModelException if there is no model associated with @p rev, if its model has
     * been deleted or if the comment is null.
     */
    Revision doAddValidatedRevision(List<RepositoryFileTransportCommand> repoFiles,
            List<RepositoryFileTransportCommand> deleteFiles, RevisionTransportCommand rev)
            throws ModelException {
        // TODO: the method should be thread safe, add a lock
        if (!rev.model) {
            throw new ModelException(null, "Model may not be null")
        }
        if (rev.model.deleted) {
            throw new ModelException(rev.model, "A new Revision cannot be added to a deleted model")
        }
        if (rev.comment == null) {
            throw new ModelException(rev.model, "Comment may not be null, empty comment is allowed")
        }
        List<File> modelFiles = getFilesFromRF(repoFiles)
        List<File> filesToDelete = getFilesFromRF(deleteFiles)

        final User currentUser = User.findByUsername(springSecurityService.authentication.name)
        final String PERENNIAL_ID = (rev.model.publicationId) ?: (rev.model.submissionId)
        Model model = getModel(PERENNIAL_ID)
        final String formatVersion = modelFileFormatService.getFormatVersion(rev)
        Revision revision = new Revision(model: model, name: rev.name, description: rev.description,
                    comment: rev.comment, uploadDate: new Date(), owner: currentUser, minorRevision: false,
                    validated:rev.validated,
                    format: ModelFormat.findByIdentifierAndFormatVersion(rev.format.identifier, formatVersion),
                    validationReport: rev.validationReport, validationLevel: rev.validationLevel)
        def stopWatch = new Log4JStopWatch("modelService.addValidatedRevision.rftcCreation")
        List<RepositoryFile> domainObjects = convertRepositoryFilesFromTransportCommands(repoFiles, revision)

        stopWatch.lap("RepositoryFileTransportCommands created.")
        // save the new model in the database
        stopWatch.setTag("modelService.addValidatedRevision.persistModel")
        try {
            String vcsId = vcsService.updateModel(model, modelFiles, filesToDelete, revision.comment)
            revision.vcsId = vcsId
        } catch (VcsException e) {
            revision.discard()
            domainObjects.each{ it.discard() }
            log.error("Exception occurred during uploading a new Model Revision to VCS: ${e.getMessage()}")
            stopWatch.stop()
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(),
                "Could not store new Model Revision for Model ${model.id} with VcsIdentifier ${model.vcsIdentifier} in VCS", e)
        }
        domainObjects.each {
            revision.addToRepoFiles(it)
        }
        // calculate the new revision number - accessing the revisions directly to circumvent ACL
        revision.revisionNumber = model.revisions.sort {it.revisionNumber}.last().revisionNumber + 1

        if (revision.validate()) {
            model.addToRevisions(revision)
            PublicationTransportCommand publicationTC = rev.model.publication
            if (!publicationTC && model.publication) {
                // delete db association if corresponding publication was removed in the UI
                model.publication = null
            } else if (publicationTC) {
                // update db association with the value from the UI
                try {
                    model.publication = pubMedService.fromCommandObject(publicationTC)
                } catch(Exception e) {
                    log.error("Unable to record publication for ${rev.model}: ${e.message}", e)
                }
            }
            //save repoFiles, revision and model in one go
            revision.save()
            model.save()
            stopWatch.lap("Model persisted to the database.")
            stopWatch.setTag("modelService.addValidatedRevision.grantPermissions")
            aclUtilService.addPermission(revision, currentUser.username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(revision, currentUser.username, BasePermission.READ)
            aclUtilService.addPermission(revision, currentUser.username, BasePermission.DELETE)

            //grant admin rights to the owner of the model
            Revision earliest = Revision.findByModelAndRevisionNumber(revision.model, 1)
            aclUtilService.addPermission(revision, earliest.owner.username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(revision, earliest.owner.username, BasePermission.DELETE)

            // grant read access to all users having read access to the model
            Acl acl = aclUtilService.readAcl(model)
            for (ace in acl.entries) {
                if (ace.sid instanceof PrincipalSid && ace.permission == BasePermission.READ) {
                    aclUtilService.addPermission(revision, ace.sid.principal, BasePermission.READ)
                }
                if (ace.sid instanceof PrincipalSid && ace.permission == BasePermission.ADMINISTRATION) {
                    aclUtilService.addPermission(revision, ace.sid.principal, BasePermission.ADMINISTRATION)
                }
            }
            stopWatch.stop()
            // !! THIS HAS TO BE IN A SEPARATE METHOD WITH A DEDICATED TRANSACTION CONTEXT !!
            //grailsApplication.mainContext.publishEvent(new RevisionCreatedEvent(this,
            //        DomainAdapter.getAdapter(revision).toCommandObject(), vcsService.retrieveFiles(revision)))
        } else {
            // TODO: this means we have imported the revision into the VCS, but it failed to be saved in the database, which is pretty bad
            revision.errors.allErrors.each {
               log.error(it)
            }
            revision.discard()
            final def m = DomainAdapter.getAdapter(model).toCommandObject()
            log.error("New Revision containing ${repoFiles.inspect()} for Model ${m} with VcsIdentifier ${model.vcsIdentifier} added to VCS, but not stored in database")
            stopWatch.stop()
            throw new ModelException(m, "Revision stored in VCS, but not in database")
        }
        return revision
    }

    /*
     * Creates validated RepositoryFile objects from corresponding RepositoryFileTransportCommands.
     *
     * This method is used to complement the validation mechanism available for domain
     * classes because the latter is applied even for operations that don't change repository file
     * objects such as deletion or publishing of models.
     *
     * This method throws ModelException if
     *      there is at least one entry in the supplied list with an undefined or inexistent path,
     *      there is at least one empty file, or
     *      there are no main files.
     *
     * @param repoFiles a list of RepositoryFileTransportCommand objects to validate and convert into
     * domain objects.
     */
    private List<RepositoryFile> convertRepositoryFilesFromTransportCommands(
            List<RepositoryFileTransportCommand> repoFileCmds, Revision revision) {
        def results = []
        boolean foundValidMainFile = false
        for (rf in repoFileCmds) {
            // validate
            String filePath = rf.path
            if (!filePath) {
                log.error("Missing path for RepositoryFile ${rf.dump()} from ${repoFileCmds.dump()}")
                throw new ModelException("We lost track of one of the files you provided for this revision.")
            }
            File f = new File(filePath)
            boolean fileExists = f.exists()
            if (!fileExists) {
                log.error("Non-existent path for RepositoryFile ${rf.dump()} from ${repoFileCmds.dump()}")
                throw new ModelException("There was a problem saving file ${f.name} for this revision.")
            }
            boolean fileIsEmpty = !f.length()
            if (fileIsEmpty) {
                log.error("Empty file ${f.name} included in ${repoFileCmds.dump()}")
                throw new ModelException("Cannot save empty file ${f.name} for this revision.")
            }
            if (rf.mainFile) {
                foundValidMainFile = true
            }
            // work out MIME type
            def sherlock = new DefaultDetector()
            def is = new BufferedInputStream(new FileInputStream(f))
            String mimeType = sherlock.detect(is, new Metadata()).toString()

            // create the domain object
            final String fileName = f.name
            final def domain = new RepositoryFile(path: fileName, description: rf.description,
                    mimeType: mimeType, revision: revision)
            if (rf.mainFile) {
                domain.mainFile = rf.mainFile
            }
            if (rf.userSubmitted) {
                domain.userSubmitted = rf.userSubmitted
            }
            if (rf.hidden) {
                domain.hidden = rf.hidden
            }
            if (!domain.validate()) {
                final def m = DomainAdapter.getAdapter(revision.model).toCommandObject()
                def msg = new StringBuffer("Invalid file ${rf.properties} uploaded for model ${m.properties}.")
                msg.append("The file failed due to ${domain.errors.allErrors.inspect()}")
                log.error(msg)
                throw new ModelException(m, """\
Your submission appears to contain invalid file ${fileName}. Please review it and try again.""")
            } else {
                results.add(domain)
            }
        }
        if (!foundValidMainFile) {
            final def m = DomainAdapter.getAdapter(revision.model).toCommandObject()
            log.error("Can't persist repository files ${repoFileCmds.dump()} for revision ${revision.dump()} without main file")
            throw new ModelException(m, "Missing main file for the new model revision ${revision.name}")
        }
        results
    }

    /**
    * Retrieves information related to a file from the VCS
    * Passes the @p revision and filename to the vcsService, gets
    * info related to the specified @p filename, and filters the returned
    * values based on the revisions available to the user
    * @param rev The model revision
    * @param filename The file to be queried
    * @return A list of VcsFileDetails objects
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getFileDetails")
    List<VcsFileDetails> getFileDetails(Revision rev, String filename) {
        def details = vcsService.getFileDetails(rev, filename)
        def accessibleRevs = getAllRevisions(rev.model)
        return details.findAll { detail ->
            boolean retval = false
            accessibleRevs.each { revision ->
                if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN") || aclUtilService.hasPermission(
                    springSecurityService.authentication, revision, BasePermission.READ)) {
                        if (revision.vcsId == detail.revisionId) {
                            retval = true
                        }
                    }
            }
            return retval
        }
    }

    /**
    * Creates a new Model and stores it in the VCS. Stripped down version suitable
    * for calling from SubmissionService, where model has already been validated
    *
    * Stores the @p modelFile as a new file in the VCS and creates a Model for it.
    * The Model will have one Revision attached to it. The MetaInformation for this
    * Model is taken from @p meta. The user who uploads the Model becomes the owner of
    * this Model. The new Model is not visible to anyone except the owner.
    * @param repoFiles The list of command objects corresponding to the files
    * of the model that is to be stored in the VCS.
    * @param rev Meta Information to be added to the model
    * @return The new created Model, or null if the model could not be created
    * @throws ModelException If Model File is not valid or the Model could not be stored in VCS
    **/
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostLogging(LoggingEventType.CREATION)
    @Profiled(tag="modelService.uploadValidatedModel")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Model uploadValidatedModel(final List<RepositoryFileTransportCommand> repoFiles,
            RevisionTransportCommand rev) throws ModelException {
        Model model
        // this tx will use a different session than the current one
        def txDefinition = [propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW]
        Model.withTransaction(txDefinition) {
            // the returned revision is detached from the Hibernate session
            model = doUploadValidatedModel(repoFiles, rev)
        }
        if (model) {
            // As it was created in a separate transaction, the model is detached from the
            // persistence context. Reattach it and its associations before attempting to
            // turn them into transport commands in order to avoid LazyInitialisationExceptions
            def attachedModel = Model.get(model.id)
            Revision r = attachedModel.revisions.first()
            RevisionTransportCommand cmd = DomainAdapter.getAdapter(r).toCommandObject()
            // can't inject searchService -- cyclic dependency
            def searchService = grailsApplication.mainContext.searchService
            searchService.updateIndex(cmd)
        }
        model
    }

    Model doUploadValidatedModel(final List<RepositoryFileTransportCommand> repoFiles,
            RevisionTransportCommand rev) throws ModelException {
        def stopWatch = new Log4JStopWatch("modelService.uploadValidatedModel.catchDuplicate")
        if (IS_DEBUG_ENABLED) {
            log.debug "About to store the following model: ${rev.name}"
        }
        // TODO: to support anonymous submissions this method has to be changed
        if (Revision.findByName(rev.name)) {
            final String msg = "There is already a Model with name ${rev.name}".toString()
            log.warn(msg)
            /*log.error(msg)
            throw new ModelException(rev.model, msg)*/
        }
        stopWatch.lap("Finished checking for model duplicates.")
        stopWatch.setTag("modelService.uploadValidatedModel.addFiles")
        Model model = new Model()
        List<File> modelFiles = getFilesFromRF(repoFiles)
        stopWatch.lap("Finished adding RepositoryFiles to the Model")
        stopWatch.setTag("modelService.uploadValidatedModel.prepareVcsStorage")
        ModelFormat format = ModelFormat.findByIdentifierAndFormatVersion(rev.format.identifier, rev.format.formatVersion)

        // vcs identifier is container name + upload date + submissionId - this should by all means be unique
        String timestamp = new Date().format("yyyy-MM-dd'T'HH-mm-ss-SSS")
        final String submissionId = submissionIdGenerator.generate()
        String modelPath = new StringBuilder(timestamp).append("_").append(submissionId).
                append(File.separator).toString()
        String container = fileSystemService.findCurrentModelContainer()
        String containerName = new File(container).name
        File modelFolder = new File(container, modelPath)
        boolean success = modelFolder.mkdirs()
        if (!success) {
            def err = "Cannot create the directory where the ${rev.name} should be stored"
            log.error(err)
            throw new ModelException(rev.model, err)
        }
        model.vcsIdentifier = new StringBuilder(containerName).append(File.separator).
                append(modelPath).toString()

        if (IS_DEBUG_ENABLED) {
            log.debug "The new model will be stored in $modelPath"
        }
        model.submissionId = submissionId
        Revision revision = new Revision(model: model,
                revisionNumber: 1,
                owner: User.findByUsername(springSecurityService.authentication.name),
                minorRevision: false,
                validated: rev.validated,
                name: rev.name,
                description: rev.description,
                comment: rev.comment,
                uploadDate: new Date(),
                format: format)

        // keep a list of RFs closeby, as we may need to discard all of them
        List<RepositoryFile> domainObjects =
                convertRepositoryFilesFromTransportCommands(repoFiles, revision)
        stopWatch.lap("Finished preparing what to store in the VCS.")
        stopWatch.setTag("modelService.uploadValidatedModel.doVcsStorage")
        try {
            String vcsId = vcsService.importModel(model, modelFiles)
            revision.vcsId = vcsId
            if (IS_DEBUG_ENABLED) {
                log.debug "First commit for ${revision.model.vcsIdentifier} is $vcsId"
            }
        } catch (VcsException e) {
            revision.discard()
            domainObjects.each { it.discard() }
            model.discard()
            //TODO undo the addition of the files to the VCS.
            def errMsg = new StringBuffer("Exception occurred while storing new Model ")
           // errMsg.append("${model.toCommandObject().properties} to VCS: ${e.getMessage()}.\n")
            errMsg.append("${model.errors.allErrors.inspect()}\n")
            errMsg.append("${revision.errors.allErrors.inspect()}\n")
            log.error(errMsg)
            stopWatch.stop()
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(),
                "Could not store new Model ${DomainAdapter.getAdapter(model).toCommandObject().properties} in VCS", e)
        }
        stopWatch.lap("Finished importing the model into the VCS.")
        stopWatch.setTag("modelService.uploadValidatedModel.gormValidation")
        domainObjects.each {
           revision.addToRepoFiles(it)
        }
        if (revision.validate()) {
            model.addToRevisions(revision)
            if (rev.model.publication) {
                model.publication = pubMedService.fromCommandObject(rev.model.publication)
            }
            if (!model.validate()) {
                // TODO: this means we have imported the file into the VCS, but it failed to be saved in the database, which is pretty bad
                revision.discard()
                model.discard()
                def msg = new StringBuffer("New Model ${rev.name} does not validate:\n")
                msg.append("${model.errors.allErrors.inspect()}\n")
                msg.append("${revision.errors.allErrors.inspect()}\n")
                log.error(msg)
                stopWatch.stop()
                throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), "New model does not validate")
            }
            model.save()
            domainObjects.each { rf ->
                if (!rf.isAttached()) {
                    rf.attach()
                }
                String path = rf.path
                String sep = File.separator.equals("/") ? "/" : "\\\\"
                if (path.contains(sep)) {
                    String fileName = path.split(sep).last()
                    rf.path = fileName
                }
                rf.save()
            }
            stopWatch.lap("Finished GORM validation.")
            stopWatch.setTag("modelService.uploadValidatedModel.grantPermissions")
            // let's add the required rights
            final String username = revision.owner.username
            aclUtilService.addPermission(model, username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(model, username, BasePermission.DELETE)
            aclUtilService.addPermission(model, username, BasePermission.READ)
            aclUtilService.addPermission(model, username, BasePermission.WRITE)
            aclUtilService.addPermission(revision, username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(revision, username, BasePermission.DELETE)
            aclUtilService.addPermission(revision, username, BasePermission.READ)
            stopWatch.stop()
            if (IS_DEBUG_ENABLED) {
                log.debug("Model $submissionId stored with id ${model.id}")
            }

            // don't broadcast event yet,wait for the current tx to commit
            //grailsApplication.mainContext.publishEvent(new ModelCreatedEvent(this, DomainAdapter.getAdapter(model).toCommandObject(), modelFiles))
        } else {
            // TODO: this means we have imported the file into the VCS, but it failed to be saved in the database, which is pretty bad
            revision.discard()
            domainObjects.each {it.discard()}
            model.discard()
            log.error("New Model does not validate:${revision.errors.allErrors.inspect()}")
            stopWatch.stop()
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), "Sorry, but the new Model does not seem to be valid.")
        }
        return model
    }

    /**
     * See https://bitbucket.org/jummp/jummp/issue/120
     * Creates a new Model and stores it in the VCS.
     *
     * Stores the @p modelFile as a new file in the VCS and creates a Model for it.
     * The Model will have one Revision attached to it. The MetaInformation for this
     * Model is taken from @p meta. The user who uploads the Model becomes the owner of
     * this Model. The new Model is not visible to anyone except the owner.
     * @param repoFiles The list of command objects corresponding to the files of the model that is to be stored in the VCS.
     * @param meta Meta Information to be added to the model
     * @return The new created Model, or null if the model could not be created
     * @throws ModelException If Model File is not valid or the Model could not be stored in VCS
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostLogging(LoggingEventType.CREATION)
    @Profiled(tag="modelService.uploadModelAsList")
    public Model uploadModelAsList(final List<RepositoryFileTransportCommand> repoFiles, ModelTransportCommand meta)
            throws ModelException {
        def stopWatch = new Log4JStopWatch("modelService.uploadModelAsList.sanityChecks")
        // TODO: to support anonymous submissions this method has to be changed
        if (!repoFiles || repoFiles.size() == 0) {
            log.error("No files were provided as part of the submission of model ${meta.properties}")
            throw new ModelException(meta, "A model must contain at least one file.")
        }
        Model model = new Model()
        List<File> modelFiles = []
        for (rf in repoFiles) {
            if (!rf || !rf.path) {
                log.error("No file was provided as part of the submission of model ${meta.properties}")
                throw new ModelException(meta, "Please supply at least one file for this model.")
            }
            final String path = rf.path
            if (!path || path.isEmpty()) {
                log.error("Null file encountered while creating ${meta.properties}: ${repoFiles.properties}")
                throw new ModelException(meta,
                    "Sorry, there was a problem with one of the files you submitted. Please refine the files you wish to upload and try again.")
            }
            final def f = new File(path)
            if (!f.exists()) {
                log.error("Non-existent file detected while uploading a new revision for ${meta.properties}: ${f.properties}")
                throw new ModelException(meta,
                    "Sorry, one of the files you submitted does not appear to exist. Please refine the files you wish to upload and try again")
            }
            if (f.isDirectory()) {
                log.error("Folder detected while uploading a new revision for ${meta.properties}: ${repoFiles.properties}")
                throw new ModelException(meta, "Sorry, we currently do not accept models organised into subfolders.")
            }
            modelFiles.add(f)
        }
        stopWatch.lap("Finished performing sanity checks.")
        stopWatch.setTag("modelService.uploadModelAsList.prepareVcsStorage")
        boolean valid = true
        ModelFormat format = ModelFormat.findByIdentifierAndFormatVersion(meta.format.identifier, "*")
        if (!modelFileFormatService.validate(modelFiles, format, [])) {
            def err = "The files ${modelFiles.inspect()} do no comprise valid ${meta.format.identifier}"
            log.error(err)
       //     throw new ModelException(meta, "Invalid ${meta.format.identifier} submission.")v
            valid = false
        }
        // model is valid, create a new repository and store it as revision1
        // vcs identifier is upload date + submissionId - this should by all means be unique
        String name = modelFileFormatService.extractName(modelFiles, format)
        if (!name && meta.name ) {
            name = meta.name
        }
        String container = fileSystemService.findCurrentModelContainer()
        String containerName = new File(container).name
        String timestamp = new Date().format("yyyy-MM-dd'T'HH-mm-ss-SSS")
        final String submissionId = submissionIdGenerator.generate()
        String modelPath = new StringBuilder(timestamp).append("_").append(submissionId).
                append(File.separator).toString()
        File modelFolder = new File(container, modelPath)
        boolean success = modelFolder.mkdirs()
        if (!success) {
            def err = "Cannot create the directory where the ${name} should be stored"
            log.error(err)
            throw new ModelException(meta, err)
        }
        model.vcsIdentifier = new StringBuilder(containerName).append(File.separator).
                    append(modelPath).toString()
        model.submissionId = submissionId
        Revision revision = new Revision(model: model,
                revisionNumber: 1,
                owner: User.findByUsername(springSecurityService.authentication.name),
                minorRevision: false,
                validated: valid,
                name: name,
                description: modelFileFormatService.extractDescription(modelFiles, format),
                comment: meta.comment,
                uploadDate: new Date())

        // keep a list of RFs closeby, as we may need to discard all of them
        List<RepositoryFile> domainObjects =
                convertRepositoryFilesFromTransportCommands(repoFiles, revision)
        String formatVersion = modelFileFormatService.getFormatVersion(revision)
        revision.format = ModelFormat.findByIdentifierAndFormatVersion(meta.format.identifier, formatVersion)
        assert formatVersion != null && revision.format != null
        try {
            revision.vcsId = vcsService.importModel(model, modelFiles)
        } catch (VcsException e) {
            revision.discard()
            domainObjects.each { it.discard() }
            model.discard()
            //TODO undo the addition of the files to the VCS.
            def errMsg = new StringBuffer("Exception occurred while storing new Model ")
            errMsg.append("${DomainAdapter.getAdapter(model).toCommandObject().properties} to VCS: ${e.getMessage()}.\n")
            errMsg.append("${model.errors.allErrors.inspect()}\n")
            errMsg.append("${revision.errors.allErrors.inspect()}\n")
            log.error(errMsg)
            stopWatch.stop()
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(),
                "Could not store new Model ${DomainAdapter.getAdapter(model).toCommandObject().properties} in VCS", e)
        }
        stopWatch.lap("Finished importing model in VCS.")
        stopWatch.setTag("modelService.uploadModelAsList.gormValidation")
        domainObjects.each {
            revision.addToRepoFiles(it)
        }
        if (revision.validate()) {
            model.addToRevisions(revision)
            if (meta.publication) {
                model.publication = pubMedService.fromCommandObject(meta.publication)
            }
            if (!model.validate()) {
                // TODO: this means we have imported the file into the VCS, but it failed to be saved in the database, which is pretty bad
                revision.discard()
                model.discard()
                def msg  = new StringBuffer("New Model ${name} does not validate:\n")
                msg.append("${model.errors.allErrors.inspect()}\n")
                msg.append("${revision.errors.allErrors.inspect()}\n")
                log.error(msg)
                stopWatch.stop()
                throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), "New model does not validate")
            }
            model.save(flush: true)
            stopWatch.lap("Finished GORM validation.")
            stopWatch.setTag("modelService.uploadModelAsList.grantPermissions")
            // let's add the required rights
            final String username = revision.owner.username
            aclUtilService.addPermission(model, username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(model, username, BasePermission.DELETE)
            aclUtilService.addPermission(model, username, BasePermission.READ)
            aclUtilService.addPermission(model, username, BasePermission.WRITE)
            aclUtilService.addPermission(revision, username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(revision, username, BasePermission.DELETE)
            aclUtilService.addPermission(revision, username, BasePermission.READ)
            stopWatch.stop()

            // broadcast event
            grailsApplication.mainContext.publishEvent(new ModelCreatedEvent(this, DomainAdapter.getAdapter(model).toCommandObject(), modelFiles))
        } else {
            // TODO: this means we have imported the file into the VCS, but it failed to be saved in the database, which is pretty bad
            revision.discard()
            domainObjects.each {it.discard()}
            model.discard()
            log.error("New Model ${model.properties} with properties ${meta.properties} does not validate:${revision.errors.allErrors.inspect()}")
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), "Sorry, but the new Model does not seem to be valid.")
        }
        return model
    }

    /**
    * Adds a new Revision to the model.
    *
    * The provided @p file will be stored in the VCS as an update to an existing file of the same @p model.
    * A new Revision will be created and appended to the list of Revisions of the @p model.
    * @param model The Model the revision should be added
    * @param repoFile The RepositoryFileTransportCommand object corresponding to the file that is to be stored in the VCS as a new revision
    * @param format The format of the model file
    * @param comment The commit message for the new revision
    * @return The new added Revision. In case an error occurred while accessing the VCS @c null will be returned.
    * @throws ModelException If either @p model, @p file or @p comment are null or if the file does not exists or is a directory
    */
    @PreAuthorize("hasPermission(#model, write) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.addRevisionAsFile")
    public Revision addRevisionAsFile(Model model, final RepositoryFileTransportCommand repoFile,
            final ModelFormat format, final String comment) throws ModelException {
        return addRevisionAsList(model, [repoFile], format, comment)
    }

    /**
    * Adds a new Revision to the model.
    * The provided @p modelFiles will be stored in the VCS as an update to the existing files of the same @p model.
    * A new Revision will be created and appended to the list of Revisions of the @p model.
    * @param model The Model the revision should be added
    * @param modelFiles The model files to be stored in the VCS as a new revision
    * @param format The format of the model files
    * @param comment The commit message for the new revision
    * @return The newly-added Revision. In case an error occurred while accessing the VCS @c null will be returned.
    * @throws ModelException If either @p model, @p modelFiles or @p comment are null or if the files do not exist or are directories.
    **/
    @PreAuthorize("hasPermission(#model, write) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.addRevisionAsList")
    public Revision addRevisionAsList(Model model, final List<RepositoryFileTransportCommand> repoFiles,
            final ModelFormat format, final String comment) throws ModelException {
        // TODO: the method should be thread safe, add a lock
        if (!model) {
            throw new ModelException(null, "Model may not be null")
        }
        if (model.deleted) {
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), "A new Revision cannot be added to a deleted model")
        }
        if (comment == null) {
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), "Comment may not be null, empty comment is allowed")
        }
        if (!repoFiles || repoFiles.size() == 0) {
            log.error("No files were provided as part of the update of model ${model.properties}")
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), "A new version of the model must contain at least one file.")
        }
        List<File> modelFiles = []
        for (rf in repoFiles) {
            if (!rf || !rf.path) {
                log.error("No file was provided as part of the update of model ${model.properties}")
                throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), "Please supply at least one file for the new version of this model.")
            }
            final String path = rf.path
            if (!path || path.isEmpty()) {
                log.error("Null file encountered while uploading a new revision for ${model.properties}: ${repoFiles.properties}")
                throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(),
                    "Sorry, there was something wrong with one of the files you submitted. Please refine the files you wish to upload and try again.")
            }
            final def f = new File(path)
            if (!f.exists()) {
                log.error("Non-existent file detected while uploading a new revision for ${model.properties}: ${f.properties}")
                throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(),
                    "Sorry, one of the files you submitted does not appear to exist. Please refine the files you wish to upload and try again")
            }
            if (f.isDirectory()) {
                log.error("Folder detected while uploading a new revision for ${model.properties}: ${repoFiles.properties}")
                throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(),
                    "Sorry, we currently do not accept model organised into sub-folders.")
            }
            if (rf.mainFile && f.length() == 0) {
                def err = "File ${f.name} cannot be empty because it is the main file of the submission."
                log.error err
                throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(), err)
            }
            modelFiles.add(f)
        }
        boolean valid = true
        if (!modelFileFormatService.validate(modelFiles, format, [])) {
            final def m = DomainAdapter.getAdapter(model).toCommandObject()
            log.warn("New revision of model ${m.properties} containing ${modelFiles.inspect()} does not comprise valid ${format.identifier}")
            //throw new ModelException(m, "The file list does not comprise valid ${format.identifier}")
            valid = false
        }

        final User currentUser = User.findByUsername(springSecurityService.authentication.name)
        Revision revision = new Revision(model: model, name: modelFileFormatService.extractName(modelFiles, format),
                        description: modelFileFormatService.extractDescription(modelFiles, format), comment: comment,
                        uploadDate: new Date(), owner: currentUser,
                minorRevision: false, validated:valid)
        List<RepositoryFile> domainObjects = convertRepositoryFilesFromTransportCommands(repoFiles, revision)
        String formatVersion = modelFileFormatService.getFormatVersion(revision)
        revision.format = ModelFormat.findByIdentifierAndFormatVersion(format.identifier, formatVersion)

        // save the new model in the database
        try {
            String vcsId = vcsService.updateModel(model, modelFiles, null, comment)
            revision.vcsId = vcsId
        } catch (VcsException e) {
            revision.discard()
            domainObjects.each{ it.discard() }
            log.error("Exception occurred during uploading a new Model Revision to VCS: ${e.getMessage()}")
            throw new ModelException(DomainAdapter.getAdapter(model).toCommandObject(),
                "Could not store new Model Revision for Model ${model.id} with VcsIdentifier ${model.vcsIdentifier} in VCS", e)
        }
        domainObjects.each {
            revision.addToRepoFiles(it)
        }

        // calculate the new revision number - accessing the revisions directly to circumvent ACL
        revision.revisionNumber = model.revisions.sort {it.revisionNumber}.last().revisionNumber + 1

        if (revision.validate()) {
            model.addToRevisions(revision)
            //save repoFiles, revision and model in one go
            model.save(flush: true)
            aclUtilService.addPermission(revision, currentUser.username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(revision, currentUser.username, BasePermission.READ)
            aclUtilService.addPermission(revision, currentUser.username, BasePermission.DELETE)
            // grant read access to all users having read access to the model
            Acl acl = aclUtilService.readAcl(model)
            for (ace in acl.entries) {
                if (ace.sid instanceof PrincipalSid && ace.permission == BasePermission.READ) {
                    aclUtilService.addPermission(revision, ace.sid.principal, BasePermission.READ)
                }
            }
            revision.refresh()
            grailsApplication.mainContext.publishEvent(new RevisionCreatedEvent(this,
                    DomainAdapter.getAdapter(revision).toCommandObject(), vcsService.retrieveFiles(revision)))
        } else {
            // TODO: this means we have imported the revision into the VCS, but it failed to be saved in the database, which is pretty bad
            revision.discard()
            final def m = DomainAdapter.getAdapter(model).toCommandObject()
            log.error("New Revision containing ${repoFiles.inspect()} for Model ${m} with VcsIdentifier ${model.vcsIdentifier} added to VCS, but not stored in database")
            throw new ModelException(m, "Revision stored in VCS, but not in database")
        }
        return revision
    }

    /**
     * Returns whether the current user has the right to add a revision to the model.
     * @param model The model to check
     * @return @c true if the user has write permission on the revision or is an admin user, @c false otherwise.
     */
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.canAddRevision")
    public Boolean canAddRevision(final Model model) {
        if (model.deleted) {
            return false
        }
        return (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN") ||
                aclUtilService.hasPermission(springSecurityService.authentication, model, BasePermission.WRITE))
    }

    /**
     * Retrieves the model files for the @p revision.
     * @param revision The Model Revision for which the files should be retrieved.
     * @return Byte Array of the content of the Model files for the revision.
     * @throws ModelException In case retrieving from VCS fails.
     */
    //@PreAuthorize("hasPermission(#revision, read) or hasRole('ROLE_ADMIN')") Not working. Seems related to: https://bitbucket.org/jummp/jummp/issue/23/spring-security-doesnt-work-as-expected-in
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.retrieveModelRepFiles")
    List<File> retrieveModelRepFiles(final Revision revision) throws ModelException {
        if (!aclUtilService.hasPermission(springSecurityService.authentication, revision, BasePermission.READ)
                && !SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
            throw new AccessDeniedException("Sorry you are not allowed to download this Model.")
        }
        List<File> files
        try {
            files = vcsService.retrieveFiles(revision)
        } catch (VcsException e) {
            log.error("Retrieving Revision ${revision.vcsId} for Model ${revision.name} from VCS failed.", e)
            throw new ModelException(DomainAdapter.getAdapter(revision.model).toCommandObject(), "Retrieving Revision ${revision.vcsId} from VCS failed.", e)
        }
        return files
    }

    /**
     * Retrieves the model files for the @p revision.
     * @param revision The Model Revision for which the files should be retrieved.
     * @return Byte Array of the content of the Model files for the revision.
     * @throws ModelException In case retrieving from VCS fails.
     */
    //@PreAuthorize("hasPermission(#revision, read) or hasRole('ROLE_ADMIN')")  Not working. Seems related to: https://bitbucket.org/jummp/jummp/issue/23/spring-security-doesnt-work-as-expected-in
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.retrieveModelFiles")
    List<RepositoryFileTransportCommand> retrieveModelFiles(final Revision revision) throws ModelException {
        if (aclUtilService.hasPermission(springSecurityService.authentication, revision, BasePermission.READ)
                || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
            return DomainAdapter.getAdapter(revision).getRepositoryFilesForRevision()
        } else {
            log.error "you can't access revision ${revision.id}!"
            throw new AccessDeniedException("Sorry you are not allowed to download this Model.")
        }
    }

    /**
     * Retrieves the model files for the latest revision of the @p model
     * @param model The Model for which the files should be retrieved
     * @return Byte Array of the content of the Model files.
     * @throws ModelException In case retrieving from VCS fails.
     */
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.retrieveModelFiles")
    List<RepositoryFileTransportCommand> retrieveModelFiles(final Model model) throws ModelException {
        final Revision revision = getLatestRevision(model, false)
        if (!revision) {
            log.error("you cant access model ${model}")
            throw new AccessDeniedException("Sorry you are not allowed to download this Model.")
        }
        return retrieveModelFiles(revision)
    }

    /**
    * Grants read access for @p model to @p collaborator.
    *
    * The @p collaborator receives the right to read all future revisions of the @p model
    * as well as read access to all revisions the current user has read access to.
    * The current user can only grant read access in case he has read access on the @p model
    * himself and the right to grant read access.
    *
    * @param model The Model for which read access should be granted
    * @param collaborator The user who should receive read access
    **/
    @PreAuthorize("hasPermission(#model, admin) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.grantReadAccess")
    public void grantReadAccess(Model model, User collaborator) {
        final String username = collaborator.username
        // Read access is modeled by adding read access to the model (user will get read access for future revisions)
        // and by adding read access to all revisions the user has access to
        aclUtilService.addPermission(model, username, BasePermission.READ)
        Set<Revision> revisions = model.revisions
        boolean isCurator = userService.isCurator(collaborator)
        if (isCurator) {
            // check if admin rights have not already been granted to avoid duplication
            if (!hasAdminPermission(model, username)) {
                aclUtilService.addPermission(model, username, BasePermission.ADMINISTRATION)
            }
            model.revisions.each { Revision it ->
                // may have been granted already through grantWriteAccess for instance
                if (!hasAdminPermission(it, username)) {
                    aclUtilService.addPermission(it, username, BasePermission.ADMINISTRATION)
                }
                aclUtilService.addPermission(it, username, BasePermission.READ)
            }
        } else {
            boolean isAdmin = SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')
            model.revisions.each { Revision it ->
                boolean canRead = aclUtilService.hasPermission(
                        springSecurityService.authentication, it, BasePermission.READ)
                if ( canRead || isAdmin ) {
                    aclUtilService.addPermission(it, username, BasePermission.READ)
                }
            }
        }
        def notification = [
                model: new ModelAdapter(model: model).toCommandObject(),
                user: springSecurityService.currentUser,
                grantedTo: collaborator,
                perms: getPermissionsMap(model)]
        sendMessage("seda:model.readAccessGranted", notification)
    }

    private String getPermissionString(int p) {
        switch(p) {
            case BasePermission.READ.getMask(): return "r"
            case BasePermission.WRITE.getMask(): return "w"
        }
        return null
    }

    /**
    * Returns permissions of a @p model. The @p authenticated parameter allows
    * notifications to be generated from non-admin updaters of the model.
    *
    * returns the users with access to the model
    *
    * @param model The Model
    **/
   // @PreAuthorize("hasPermission(#model, admin) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getPermissionsMap")
    public Collection<PermissionTransportCommand> getPermissionsMap(Model model, boolean authenticated = true) {
        def map = new HashMap<Integer, PermissionTransportCommand>()
        if (!authenticated || aclUtilService.hasPermission(springSecurityService.authentication, model,
                    BasePermission.ADMINISTRATION ) || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
            def permissions = aclUtilService.readAcl(model).getEntries()
            permissions.each {
                String permission = getPermissionString(it.getPermission().getMask())
                String principal = it.getSid().principal
                if (permission) {
                    User user = User.findByUsername(principal)
                    String userRealName = user.person.userRealName
                    int userId = user.id
                    if (!map.containsKey(userId)) {
                        PermissionTransportCommand ptc = new PermissionTransportCommand(
                                name: userRealName, id: userId, username: user.username)
                        map.put(userId, ptc)
                    }
                    if (principal == springSecurityService.principal.username) {
                        map.get(userId).show = false
                    }
                    if (permission == "r") {
                        map.get(userId).read = true
                    }
                    else {
                        map.get(userId).write = true
                        //disable editing for curators and for users who have contributed revisions
                        if (userService.isCurator(user) &&
                                model.revisions*.owner*.id.contains(user.id)) {
                            map.get(userId).disabledEdit = true
                        }
                        model.revisions.each {
                            if (it.owner.username == principal) {
                                map.get(userId).disabledEdit = true
                            }
                        }
                    }
                }
            }
        }
        else {
            throw new AccessDeniedException("You cant access permissions if you dont have them.")
        }
        return map.values()
    }

    /**
    * Grants permissions to a @model given a list of @permissions
    *
    *
    * @param model The Model
    * @param permissions A list of permissions
    **/
   // @PreAuthorize("hasPermission(#model, admin) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getPermissionsMap")
    public void setPermissions(Model model, List<PermissionTransportCommand> permissions) {
        if (aclUtilService.hasPermission(springSecurityService.authentication, model,
                    BasePermission.ADMINISTRATION ) || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
            Collection<PermissionTransportCommand> existing = getPermissionsMap(model)
            permissions.each { newPerm ->
                PermissionTransportCommand current=existing.find {
                    it.id == newPerm.id
                }
                User user = User.get(newPerm.id)
                if (current) {
                    if (current.read && !(newPerm.read)) {  //revoke previously held read access
                        revokeReadAccess(model, user)
                    }
                    if (current.write && !(newPerm.write)) { //revoke previously held write access
                        revokeWriteAccess(model, user)
                    }
                    if (!(current.read) && newPerm.read) {
                        grantReadAccess(model, user)
                    }
                    if (!(current.write) && newPerm.write) {
                        grantWriteAccess(model, user)
                    }
                }
                else {
                    if (newPerm.read) {
                        grantReadAccess(model, user)
                    }
                    if (newPerm.write) {
                        grantWriteAccess(model, user)
                    }
                }
            }
            existing.each { oldPerm ->
                def retained = permissions.find {
                    it.id == oldPerm.id
                }
                if (!retained) {
                    User user = User.get(oldPerm.id)
                    if (oldPerm.read) {
                        revokeReadAccess(model, user)
                    }
                    if (oldPerm.write) {
                        revokeWriteAccess(model, user)
                    }
                }
            }
        }
        else {
            throw new AccessDeniedException("You cant access permissions if you dont have them.")
        }
    }

    private String getUsername() {
        if (springSecurityService.isLoggedIn()) {
            return (springSecurityService.currentUser as User).getUsername()
        }
        return "anonymous"
    }

    /**
    * Grants write access for @p model to @p collaborator.
    *
    * The @p collaborator receives the right to add new revisions to the @p model.
    * The current user can only grant write access in case he has write access on the @p model
    * himself and the right to grant write access.
    *
    * @param model The Model for which write access should be granted
    * @param collaborator The user who should receive write access
    **/
    @PreAuthorize("hasPermission(#model, admin) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.grantWriteAccess")
    public void grantWriteAccess(Model model, User collaborator) {
        final String principal = collaborator.username
        aclUtilService.addPermission(model, principal, BasePermission.WRITE)
        boolean isCurator = userService.isCurator(collaborator)
        if (isCurator) {
            // check if admin rights have not already been granted to avoid duplication
            if (!hasAdminPermission(model, principal)) {
                aclUtilService.addPermission(model, principal, BasePermission.ADMINISTRATION)
            }
            model.revisions.each { Revision it ->
                // may have been granted already through grantReadAccess for instance
                if (!hasAdminPermission(it, username)) {
                    aclUtilService.addPermission(it, username, BasePermission.ADMINISTRATION)
                }
            }
        }
        def notification = [
                model: new ModelAdapter(model: model).toCommandObject(),
                user: springSecurityService.currentUser,
                grantedTo: collaborator,
                perms: getPermissionsMap(model)]
        sendMessage("seda:model.writeAccessGranted", notification)
    }

    /**
    * Revokes read access for @p model from @p collaborator.
    *
    * The @p collaborator gets the right to read future revisions to the @p model revoked.
    * Read access to existing revisions is not revoked.
    * Write access to the model (that is uploading new revisions) is also revoked.
    * The current user can only revoke the right if he has the right to read future revisions
    * himself and has the right to grant/revoke read rights on the model. The right is not revoked
    * if the user is an administrator of the model.
    * @param model The Model for which read access should be revoked
    * @param collaborator The User whose read access should be revoked
    * @return @c true if the right has been revoked, @c false otherwise
    **/
    @PreAuthorize("hasPermission(#model, admin) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.revokeReadAccess")
    public boolean revokeReadAccess(Model model, User collaborator) {
        final String principal = collaborator.username
        if (principal == springSecurityService.authentication.name) {
            // the user cannot revoke his own rights
            return false
        }
        boolean isCurator = userService.isCurator(collaborator)
        Set<Revision> revisions = model.revisions

        aclUtilService.deletePermission(model, principal, BasePermission.READ)
        aclUtilService.deletePermission(model, principal, BasePermission.WRITE)
        if (isCurator) {
            aclUtilService.deletePermission(model, principal, BasePermission.ADMINISTRATION)
            revisions.each { Revision r ->
                aclUtilService.deletePermission(r, principal, BasePermission.ADMINISTRATION)
                aclUtilService.deletePermission(r, principal, BasePermission.READ)
            }
        } else {
            boolean adminToModel = hasAdminPermission(model, principal)
            if (adminToModel) {
                aclUtilService.deletePermission(model, principal, BasePermission.ADMINISTRATION)
            }
            final boolean isAdmin = SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')
            for (Revision revision in revisions) {
                boolean canRead = aclUtilService.hasPermission(
                        springSecurityService.authentication, revision, BasePermission.READ)
                if (canRead || isAdmin) {
                    try {
                        aclUtilService.deletePermission(revision, principal, BasePermission.READ)
                    } catch(Exception e) {
                        log.error e.message, e
                        return false
                    }
                }
            }
        }
        return true
    }

    /*
     * Convenience method for checking if a user has admin privileges on a model or revision.
     *
     * @param modelOrRevision the model or revision for which to test the permissions.
     * @param username the username of the person for which to test the permissions.
     * @return true if we find a matching ACL entry, false otherwise.
     */
    private boolean hasAdminPermission(def modelOrRevision, String username) {
        Acl acl = aclUtilService.readAcl(modelOrRevision)
        return null != acl.entries.find { ace ->
            if (!(ace instanceof PrincipalSid)) {
                return
            }
            def aceAsPrincipalSid = ace.sid as PrincipalSid
            aceAsPrincipalSid.principal == username &&
                    ace.permission == BasePermission.ADMINISTRATION
        }
    }

    /**
    * Revokes write access for @p model from @p collaborator.
    *
    * The @p collaborator gets the right to add revisions to the @p model revoked.
    * The current user can only revoke the right if he has the right to add revisions
    * himself and has the right to grant/revoke write rights on the model
    * @param model The Model for which write access should be revoked
    * @param collaborator The User whose write access should be revoked
    * @return @c true if the right has been revoked, @c false otherwise
    **/
    @PreAuthorize("hasPermission(#model, admin) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.revokeWriteAccess")
    public boolean revokeWriteAccess(Model model, User collaborator) {
        final String principal = collaborator.username
        if (principal == springSecurityService.authentication.name) {
            // the user cannot revoke his own rights
            return false
        }
        boolean adminToModel = hasAdminPermission(model, principal)
        if (adminToModel) {
            aclUtilService.deletePermission(model, principal, BasePermission.ADMINISTRATION)
        }
        aclUtilService.deletePermission(model, principal, BasePermission.WRITE)
        boolean isCurator = userService.isCurator(collaborator)
        if (isCurator) {
            model.revisions.each { Revision r ->
                aclUtilService.deletePermission(r, principal, BasePermission.ADMINISTRATION)
            }
        }
        return true
    }

    /**
    * Transfers the ownership of the @p model to @p collaborator.
    *
    * The ownership can only be transferred from a user having the right to grant
    * read/write access and the @p model is not yet under curation or published.
    * The @p collaborator has to have read access to future revisions of the model.
    *
    * All Model specific rights are revoked from the owner and granted to the @p collaborator.
    * This includes:
    * @li Write access to the @p model
    * @li Read access to future revisions of the @p model
    * @li Start of curation
    * @li Grant/Revoke read/write access to the @p model
    * @param model The Model for which the ownership should be transferred.
    * @param collaborator The User who becomes the new owner
    **/
    @PreAuthorize("hasPermission(#model, admin) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.transferOwnerShip")
    public void transferOwnerShip(Model model, User collaborator) {
        // TODO: implement me
    }

    /**
    * Checks if the model can be deleted
    *
    * @param model The Model to be deleted
    * @return @c true in case the Model can be deleted, @c false otherwise.
    **/
    @PostLogging(LoggingEventType.DELETION)
    @Profiled(tag="modelService.canDelete")
    public boolean canDelete(Model model) {
        if (!model) {
            throw new IllegalArgumentException("Model may not be null")
        }
        if (model.deleted) {
            return false
        }
        boolean publicRev = hasPublicRevision(model)
        if (publicRev) {
            return false
        }
        boolean isAdmin = SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")
        boolean hasDeleteRight = aclUtilService.hasPermission(
                springSecurityService.authentication, model, BasePermission.DELETE)
        return isAdmin || hasDeleteRight
    }

    /**
    * Checks if the model can be shared
    *
    * @param model The Model to be shared
    * @return @c true in case the Model can be shared, @c false otherwise.
    **/
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.canShare")
    public boolean canShare(Model model) {
        if (!model) {
            throw new IllegalArgumentException("Model may not be null")
        }
        if (model.deleted) {
            return false
        }
        return (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN") || aclUtilService.hasPermission(
                springSecurityService.authentication, model, BasePermission.ADMINISTRATION))
    }

    /**
    * Deletes the @p model.
    *
    * Flags the @p model as deleted in the database and the search index.
    *
    * The corresponding revision objects are not set as deleted in the database
    * because that would prevent users from being able to access archived models.
    *
    * Deletion of @p model is only possible if the model is neither under curation nor published.
    * @param model The Model to be deleted
    * @return @c true in case the Model has been deleted, @c false otherwise.
    * @see ModelService#restoreModel(Model model)
    **/
    /*@PreAuthorize("hasPermission(#model, delete) or hasRole('ROLE_ADMIN')")*/ //Doesnt work
    @PostLogging(LoggingEventType.DELETION)
    @Profiled(tag="modelService.deleteModel")
    public boolean deleteModel(Model model) {
        if (!model) {
            throw new IllegalArgumentException("Model may not be null")
        }
        if (!canDelete(model)) {
            throw new AccessDeniedException("You do not have permission to delete this model")
        }
        if (model.deleted) {
            return false
        }
        boolean modelAlreadyPublic = hasPublicRevision(model)
        if (modelAlreadyPublic) {
            if (IS_DEBUG_ENABLED) {
                log.debug "Refusing to delete published model ${model.submissionId}"
            }
            return false
        }
        if (IS_DEBUG_ENABLED) {
            log.debug("Attempting to delete model ${model.submissionId}")
        }
        // can't inject searchService - cyclic dependency
        def searchService = grailsApplication.mainContext.searchService
        searchService.setDeleted(model)
        if (!searchService.isDeleted(model)) {
            // leave the model as not deleted and log the error
            log.error("Could not set model ${model.submissionId} as deleted in solr.")
            return false
        } else {
            model.deleted = true
            model.save(flush: true)
            //quick test to make sure Solr is in sync with the database
            model.refresh()
            boolean db = model.deleted
            boolean solr = searchService.isDeleted(model)
            if (IS_DEBUG_ENABLED) {
                def m = new StringBuilder("Deletion status for ").append(model.submissionId
                ).append(" - db: ").append(db).append(" solr: ").append(solr)
                log.debug(m.toString())
            }
            return db && solr
        }
    }

    /*
     * Convenience method that checks whether a model has any publicly-available revision.
     *
     * @param model the model for which to verify the publication status.
     */
    private boolean hasPublicRevision(Model model) {
        def publicRevisionCriteria = Revision.createCriteria()
        def publicRevisionCriteriaResults = publicRevisionCriteria.list(max: 1) {
            and {
                eq("model", model)
                ne("state", ModelState.UNPUBLISHED)
            }
        }
        [] != publicRevisionCriteriaResults
    }

    /**
    * Restores the deleted @p model.
    *
    * Removes the deleted flag from the model and all its Revisions.
    * @param model The deleted Model to restore
    * @return @c true, whether the state was restored, @c false otherwise.
    * @see ModelService#deleteModel(Model model)
    **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.restoreModel")
    public boolean restoreModel(Model model) {
        if (!model) {
            throw new IllegalArgumentException("Model may not be null")
        }
        if (!Model.exists(model.id)) {
            throw new IllegalArgumentException("Model ${model.properties} absent from database")
        }
        def searchService = grailsApplication.mainContext.searchService
        boolean dbStatus = model.deleted
        boolean solrStatus = searchService.isDeleted model
        boolean modelIsDeleted = dbStatus && solrStatus
        if (!modelIsDeleted) {
            return false
        }
        searchService.setDeleted(model, false)
        if (searchService.isDeleted(model)) {
            log.error "Could not restore model ${model.submissionId} in Solr"
            return false
        } else {
            model.deleted = false
            model.save(flush: true)
            model.refresh()
            return !(model.deleted || searchService.isDeleted(model))
        }
    }

    /**
     * Deletes the @p revision of the model if it is the latest Revision of the model.
     *
     * This is no real deletion, but only a flag which is set on the Revision. Due to
     * technical constraints of the underlying version control system a real deletion
     * is not possible.
     * @param revision The Revision to delete
     * @return @c true if revision was deleted, @c false otherwise
     */
    @PreAuthorize("hasPermission(#revision, delete) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.DELETION)
    @Profiled(tag="modelService.deleteRevision")
    public boolean deleteRevision(Revision revision) {
        if (!revision) {
            throw new IllegalArgumentException("Revision may not be null")
        }
        if (revision.deleted) {
            // revision is already deleted
            return false
        }
        // check if the revision is the latest non-deleted method
        if (revision.id != revision.model.revisions.findAll { !it.deleted }.sort { it.revisionNumber }.last().id) {
            // TODO: maybe better throw an exception
            return false
        }
        if (revision.model.revisions.findAll { !it.deleted }.size() == 1) {
            // only one revision, delete the Model
            // first check the ACL, has to be manual as Spring would not intercept the direct method call
            if (aclUtilService.hasPermission(springSecurityService.authentication, revision.model,
                        BasePermission.DELETE) || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
                deleteModel(revision.model)
            } else {
                throw new AccessDeniedException("No permission to delete Model ${revision.model.id}")
            }
        }
        // TODO: delete the model if the revision is the first revision of the model
        revision.deleted = true
        revision.save(flush: true)
        return true
    }

    /**
     * Tests if the user can publish this revision
     * Only a Curator or an Administrator are allowed to call this
     * method.
     * @param revision The Revision to be published
     */
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.canPublish")
    public boolean canPublish(Revision revision) {
        if (!revision) {
            return false
        }
        if (revision.deleted) {
            return false
        }
        if (revision.model.deleted) {
            return false
        }
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN,ROLE_CURATOR")) {
            return true
        }
        return false
    }

    /**
     * Tests if the user can submit this revision for publication
     * Only a User or an Administrator or other Curators are allowed to call this
     * method.
     * @param revision The Revision to be published
     */
    @PostLogging(LoggingEventType.SUBMIT_FOR_PUBLICATION)
    @Profiled(tag="modelService.canSubmitForPublication")
    public boolean canSubmitForPublication(Revision revision) {
        if (!revision) {
            return false
        }
        if (revision.deleted) {
            return false
        }
        if (revision.model.deleted) {
            return false
        }
        if (!SpringSecurityUtils.ifAnyGranted("ROLE_CURATOR")) {
            return true
        }
        return false
    }

    /**
         * Tests if the user can validate this revision
         * Only a Curator with write permission on the Revision or an Administrator are allowed to call this
         * method.
         * @param revision The Revision to be validated
         */
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.canValidate")
    public boolean canValidate(Revision revision) {
        if (!revision) {
            return false
        }
        if (revision.deleted) {
            return false
        }
        if (revision.model.deleted) {
            return false
        }
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            return true
        }
        if (SpringSecurityUtils.ifAnyGranted("ROLE_CURATOR")) {
            return canAddRevision(revision.model)
        }
        return false
    }

    /**
     * Makes a Model Revision publicly available.
     * This means that ROLE_USER and ROLE_ANONYMOUS gain read access to the Revision and by that also to
     * the Model.
     *
     * Only a Curator with write permission on the Revision or an Administrator are allowed to call this
     * method.
     * @param revision The Revision to be published
     */
    @PreAuthorize("hasRole('ROLE_CURATOR') or hasRole('ROLE_ADMIN')") //used to be: (hasRole('ROLE_CURATOR') and hasPermission(#revision, admin))
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.publishModelRevision")
    public PublishContext publishModelRevision(Revision revision) {
        if (!SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            if (!aclUtilService.hasPermission(springSecurityService.authentication, revision,
                        BasePermission.ADMINISTRATION)) {
                throw new AccessDeniedException("You cannot publish this model.");
            }
        }
        if (!revision) {
            throw new IllegalArgumentException("Revision may not be null")
        }
        if (revision.deleted) {
            throw new IllegalArgumentException("Revision may not be deleted")
        }
        Model model = revision.model

        //validating publish process
        if(!revision.validationLevel.equals(ValidationState.APPROVED)){
            throw new PublishException("You cannot publish this model. Please check the annotations.")
        }

        Qualifier qualifier = Qualifier.findByUri("http://www.ddmore.org/ontologies/webannotationtool#model-implementation-conforms-to-literature-controlled")
        def stmtsWithQualifier = revision.annotations*.statement.findAll { it.qualifier == qualifier }
        def qualifierXrefs = stmtsWithQualifier.collect { Statement s -> s.object }
        ResourceReference resourceReference = qualifierXrefs.first()
        boolean originalModel = true
        if(resourceReference.name.toLowerCase().equals("no")){
            originalModel = false
        }
        PublishInfo pubInfo = new PublishInfo(originalModel)
        revision.repoFiles.each {
            String description = null;
            if (it.mainFile) {
                description = it.revision.description
            }else {
                description = it.description
            }
            if (description == null || description.empty) {
                throw new PublishException("Please provide a description for the file: " + it.path)
            }

            pubInfo.addToFileSet(it.path, description);
        }

        if(!pubInfo.validModelAccomodation()){
            throw new PublishException("Model is not compliance with original publication. Please provide a Model_Accommodations.txt file.")
        }

        def scenario = publishValidator.validatePublish(pubInfo)
        if(!scenario) {
            throw new PublishException("Submission did not match any of the scenarios. Please upload all required files")
        }

        if (MAKE_PUBLICATION_ID) {
            model.publicationId = model.publicationId ?: publicationIdGenerator.generate()
        }
        model.firstPublished = new Date()
        aclUtilService.addPermission(revision, "ROLE_USER", BasePermission.READ)
        aclUtilService.addPermission(revision, "ROLE_ANONYMOUS", BasePermission.READ)
        revision.state = ModelState.PUBLISHED
        if (!model.save(flush: true)) {
            throw new ModelException(
                    "Cannot publish model ${model.submissionId}:${b.errors.allErrors.inspect()}")
        }

        return publishValidator.generatePublishContext(scenario)
    }

    /**
     * Makes a Model Revision unpublished
     * This means that ROLE_USER and ROLE_ANONYMOUS lose read access to the Revision and by that also to
     * the Model, if they had it.
     *
     * Only a Curator with write permission on the Revision or an Administrator are allowed to call this
     * method.
     * @param revision The Revision to be published
     */
    @PreAuthorize("(hasRole('ROLE_CURATOR') and hasPermission(#revision, admin)) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.unpublishModelRevision")
    public void unpublishModelRevision(Revision revision) {
        if (!revision) {
            throw new IllegalArgumentException("Revision may not be null")
        }
        if (revision.deleted) {
            throw new IllegalArgumentException("Revision may not be deleted")
        }
        aclUtilService.deletePermission(revision, "ROLE_USER", BasePermission.READ)
        aclUtilService.deletePermission(revision, "ROLE_ANONYMOUS", BasePermission.READ)
        revision.state=ModelState.UNPUBLISHED
        revision.save(flush:true)
    }

    /**
     * Makes a Model Revision publicly available.
     * This means that ROLE_USER and ROLE_ANONYMOUS gain read access to the Revision and by that also to
     * the Model.
     *
     * Only a Curator with write permission on the Revision or an Administrator are allowed to call this
     * method.
     * @param revision The Revision to be published
     */
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')") //used to be: (hasRole('ROLE_USER') and hasPermission(#revision, admin))
    @PostLogging(LoggingEventType.SUBMIT_FOR_PUBLICATION)
    @Profiled(tag="modelService.submitModelRevisionForPublication")
    public void submitModelRevisionForPublication(Revision revision) {
        if (!revision) {
            throw new IllegalArgumentException("Revision may not be null")
        }
        if (revision.deleted) {
            throw new IllegalArgumentException("Revision may not be deleted")
        }
        Model model = revision.model
        // grant read access this model revision to all existing curators
        List<User> curators = userService.getUsersByRole("ROLE_CURATOR")
        curators.each { curator ->
            grantReadAccess(model, curator)
        }
        // grant read access and administrative privilege to future curators
        aclUtilService.addPermission(revision, "ROLE_CURATOR", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(revision, "ROLE_CURATOR", BasePermission.READ)
        revision.state = ModelState.UNDER_CURATION
        revision.save(flush: true)
    }

    /**
     * Create a model audit item
     * @param cmd The ModelAuditTransportCommand to be saved in the database
     */
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.createAuditItem")
    long createAuditItem(ModelAuditTransportCommand cmd) {
        User user = null
        if (cmd.username != "anonymousUser") {
            user = User.findByUsername(cmd.username)
        }
        def modelId = cmd.model?.id
        if (Model.exists(modelId)) {
            def model = Model.load(modelId)
            ModelAudit audit = new ModelAudit(model: model,
                    user: user,
                    format: cmd.format,
                    type: cmd.type,
                    changesMade: cmd.changesMade,
                    success: cmd.success)
            audit.save()
            return audit.id
        }
        return -1
    }

    /**
     * Update the success field in a model audit item
     * The model audit item is initialised with false success from the
     * before interceptor. This function is called to update the success
     * from the after interceptor if no exception was thrown.
     * @param cmd The ModelAuditTransportCommand to be saved in the database
     */
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.updateAuditSuccess")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void updateAuditSuccess(Long itemId, boolean success) {
        if (itemId != -1) {
            ModelAudit audit = ModelAudit.get(itemId)
            audit.success = success
            if (audit.isDirty('success')) {
                if (!audit.save(flush: true)) {
                    log.error("""\
Failed to update audit $itemId to $success: ${audit.errors.allErrors.inspect()}""")
                }
            }
        }
    }

    /**
     * Retrieves the pub med annotations of the @p model.
     * @param model The model of which the pub med annotation are to be retrieved
     * @return The retrieved pub med annotations or @c null
     * @throws JummpException
     */
    protected List<String> getPubMedAnnotation(Model model) throws JummpException {
        if (!model) {
            return null
        }
        Revision revision = getLatestRevision(model, false)
        if (!revision) {
            return null
        }
        return modelFileFormatService.getPubMedAnnotation(revision)
    }
}
