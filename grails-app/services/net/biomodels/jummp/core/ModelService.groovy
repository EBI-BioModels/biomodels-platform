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

import grails.plugin.cache.Cacheable
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.authentication.GrailsAnonymousAuthenticationToken
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import groovy.transform.CompileStatic
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.core.constants.Redis
import net.biomodels.jummp.core.events.*
import net.biomodels.jummp.core.events.RevisionCreatedEvent as RevisionCE
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.model.ModelTransportCommand as ModelTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC
import net.biomodels.jummp.core.model.identifier.ModelIdentifierGeneratorRegistryService
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.util.JummpHttpService
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.core.vcs.VcsFileDetails
import net.biomodels.jummp.model.*
import net.biomodels.jummp.model.ContributionDetails as CD
import net.biomodels.jummp.model.ContributionRole as CR
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import org.codehaus.groovy.grails.web.json.JSONObject
import org.perf4j.StopWatch
import org.perf4j.aop.Profiled
import org.perf4j.log4j.Log4JStopWatch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.ObjectFactory
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.AccessControlEntry
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.core.Authentication
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock

/**
 * Service class for managing Models.
 *
 * <p>This service provides the high-level API to access models and their revisions.
 * It is recommended to use this service instead of accessing Models and Revisions
 * directly through GORM. The service methods respect the ACL on the objects and by
 * that ensures that no user can perform actions he is not allowed to.</p>

 * <p>ModelService is a singleton, but the model ID generators are prototypes.
 * We cannot directly inject the id generator beans because Spring caches a singleton's
 * dependencies after creation, meaning modelService.submissionIdGenerator.lastUsedValue would
 * always return the identifier generated before the modelService bean got instantiated (e.g.
 * when the application started).</p>
 *
 * <p>The getters {@code getSubmissionIdGenerator} and {@code getPublicationIdGenerator} solve
 * this reference problem by ensuring that the generators are always retrieved from the application
 * context. Since these beans have prototype scope, every time we call these getters, or their
 * Groovy short-hand equivalents ({@code submissionIdGenerator} and {@code publicationIdGenerator}
 * respectively), we get a new instance of these generators that are initialised with the most
 * recent identifier values generated in the past. See
 * <a href='https://www.baeldung.com/spring-inject-prototype-bean-into-singleton'>this article<a>
 * for a more in-depth discussion of the problem.</p>
 *
 * @see Model
 * @see Revision
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Sarala Wimalaratne <sarala@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 *
 * @date 20181025
 */
@SuppressWarnings("GroovyUnusedCatchParameter")
@Transactional
class ModelService implements ApplicationListener<ModelOperationEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ModelService.class)
    def springSecurityService
    def aclUtilService
    def vcsService
    def modelFileFormatService
    def grailsApplication
    def publicationService
    def modelHistoryService
    def fileSystemService
    def userService
    //def publishValidator
    def modelConversionService
    def repositoryFileService
    def redisService
    def contributorService

    ObjectFactory<ModelIdentifierGeneratorRegistryService> idGeneratorRegistryFactoryBean

    final boolean MAKE_PUBLICATION_ID = !(publicationIdGenerator instanceof NullModelIdentifierGenerator)

    /**
     * Guard insertion of ACL entries from concurrent access.
     */
    final ReentrantLock aclInsertionLock = new ReentrantLock()

    /**
     * Provides a prototype-scoped submission id generator bean.
     */
    ModelIdentifierGenerator getSubmissionIdGenerator() {
        grailsApplication?.mainContext?.submissionIdGenerator
    }

    /**
     * Provides a prototype-scoped publication id generator bean.
     */
    ModelIdentifierGenerator getPublicationIdGenerator() {
        grailsApplication?.mainContext?.publicationIdGenerator
    }

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
    List<Model> getAllModels(int offset, int count, boolean sortOrder, ModelListSorting sortColumn,
                                    String filter = null, boolean deletedOnly = false) {
        Map metaParams
        if (offset < 0 || count <= 0) {
            // safety check
            metaParams = [:]
        } else {
            metaParams = [
                max: count, offset: offset
            ]
        }

        String sortingDirection = sortOrder ? 'asc' : 'desc'

        boolean filterIsValid = filterValid(filter)
        String type
        Map namedParams = [:]
        // use object IDs here to minimise the number SQL queries and JOINS Hibernate uses.
        List filteredFormats, filteredUsers
        if (filterIsValid) {
            boolean isTypeQuery = filter?.substring(0,4)?.equals("type")
            if (!isTypeQuery) {
                if (filter.take(6) == "Format") {
                    String formatId = filter.drop(7)
                    filteredFormats = ModelFormat.executeQuery(
                        "SELECT id FROM ModelFormat WHERE identifier = :p", [p: formatId]
                    )
                    namedParams.put("formats", filteredFormats)
                }
                if (filter.take(9) == "Submitter") {
                    String personName = filter.drop(10)
                    filteredUsers = User.executeQuery(
                        "SELECT u.id FROM User u JOIN u.person p WHERE p.userRealName = :n",
                        [n: personName]
                    )
                    namedParams.put("users", filteredUsers)
                }
            } else {
                // type := < private | shared | public >
                type = filter.drop(5).toLowerCase()
            }
        }
        String query
        // for Admin - sees all (not deleted) models
        boolean isAdmin = SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")
        query = getQueryStringForUser(sortColumn, deletedOnly, filteredFormats, filteredUsers,
            sortingDirection, isAdmin)
        if (!isAdmin) {
            List permissions = new ArrayList([BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()])
            Set<String> roles = getSpringDatabaseRoles()
            namedParams += [
                className  : Revision.class.getName(),
                permissions: permissions,
                roles      : roles
            ]
        }
        def results = []
        try {
            List queryResultSet = Model.executeQuery(query, namedParams, metaParams)
            // result set consists of [model, <sortColumnValue>, revisionNumber] and we can't avoid it
            // extract the first column before the result set gets used downstream
            List<Long> modelIds = queryResultSet.collect { it[0] }
            results = Model.getAll(modelIds)
        } catch (Exception e) {
            logger.error("Exception $e while executing $query with '$namedParams' (page '$metaParams')")
        }
        results
    }

    private String getQueryStringForUser(ModelListSorting sortColumn, boolean deletedOnly,
                                         List filteredFormats, List filteredUsers,
                                         String sortingDirection, boolean isAdmin = false) {
        String orderByColumn = getSortColumnAsString(sortColumn)
        // we have to include $orderByColumn and revisionNumber in the selected columns
        // in order for the query to be valid SQL
        String query = """\
SELECT m.id, ${orderByColumn}, r.revisionNumber
FROM Revision AS r RIGHT OUTER JOIN r.model AS m
WHERE
 r.deleted = false
 AND m.deleted = ${deletedOnly}
 ${return filteredFormats ? "AND r.format.id IN (:formats)" : ""}
 ${return filteredUsers ? "AND r.owner.id IN (:users)" : ""}
"""
        if (isAdmin) {
            query = """$query AND r.revisionNumber=(SELECT MAX(r2.revisionNumber) from Revision r2 where r.model=r2.model)"""
        } else {
            query = """\
$query AND r.revisionNumber=(SELECT MAX(r2.revisionNumber) from Revision r2, AclEntry ace
  WHERE r.model = r2.model
    AND r2.id = ace.aclObjectIdentity.objectId
    AND ace.aclObjectIdentity.aclClass.className = :className
    AND ace.sid.sid IN (:roles)
    AND ace.mask IN (:permissions))"""
        }

        query = """$query ORDER BY ${orderByColumn} ${sortingDirection}, r.revisionNumber desc"""
        return query
    }

    /**
     * Returns list of Models with other essential information the user has access to.
     *
     * Searches for all Models the current user has access to, then return the model, the model name and
     * the date when the model was uploaded
     * @param deletedOnly   false by default
     * @return List of composite objects
     **/
    @Profiled(tag = "modelService.getAllModelWithDetails")
    List getAllModelWithDetails(boolean deletedOnly = false) {
        String query = """\
SELECT m, r.name, r.uploadDate
FROM Revision AS r RIGHT OUTER JOIN r.model AS m
WHERE
    r.deleted = false
    AND m.deleted = ${deletedOnly}
    AND r.revisionNumber=(SELECT MAX(r2.revisionNumber) from Revision r2, AclEntry ace
                            WHERE r.model = r2.model
                                AND r2.id = ace.aclObjectIdentity.objectId
                                AND ace.aclObjectIdentity.aclClass.className = :className
                                AND ace.sid.sid IN (:roles)
                                AND ace.mask IN (:permissions))"""

        List permissions = new ArrayList([BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()])
        Set<String> roles = getSpringDatabaseRoles()
        Map namedParams = [
            className  : Revision.class.getName(),
            permissions: permissions,
            roles      : roles
        ]
        Model.executeQuery(query, namedParams, [:])
    }

    /**
     * Short method to get the SQL-name of the {@Link ModelListSorting} enum
     * @param sortColumn
     * @return
     */
    // ToDo: this should really be enum-properties in the ModelListSorting enum itself.
    private String getSortColumnAsString(ModelListSorting sortColumn) {
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
    * @return List of {@link Model}s sorted ascending
    * @see ModelService#getAllModels(int offset, int count, boolean sortOrder)
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getAllModels")
    List<Model> getAllModels(int offset, int count, boolean sortOrder) {
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
    List<Model> getAllModels(int offset, int count, ModelListSorting sortColumn) {
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
    List<Model> getAllModels(int offset, int count) {
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
    List<Model> getAllModels(ModelListSorting sortColumn) {
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
    List<Model> getAllModels() {
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
    Integer getModelCount(String filter = null, boolean deletedOnly = false) {
        ModelListSorting sorting
        List<Model> resultSet = getAllModels(-1, 0, false, sorting, filter, false)
        resultSet.size()
    }

    /**
     * Returns the list of Models the user has access to. These models only include private and shared ones.
     *
     * @param filter Optional filter for search
     * @see ModelService#getAllModels()
     **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getMyModels")
    List<Model> getMyModels(int offset, int count, boolean sortOrder, ModelListSorting sortColumn,
                            String filter = null, boolean deletedOnly = false) {
        String query = """\
SELECT distinct m.id, m.submissionId
FROM Revision AS r JOIN r.model AS m
WHERE
    r.deleted = false
    AND m.deleted = false
    AND r.owner.id = :ownerId
"""
        User u = springSecurityService.currentUser
        String username = u.username
        Long userId = u.id
        String message = "User $userId : $username is accessing their models at ${new Date()}"
        log.debug(message)

        boolean filterIsValid = filterValid(filter)
        Map namedParams = ["ownerId": userId]
        String type
        // use object IDs here to minimise the number SQL queries and JOINS Hibernate uses.
        if (filterIsValid) {
            boolean isTypeQuery = filter?.substring(0,4)?.equals("type")
            if (!isTypeQuery) {
                if (filter.take(6) == "format") {
                    String formatId = filter.drop(7)
                    List filteredFormats = new ArrayList()
                    filteredFormats = ModelFormat.executeQuery(
                        "SELECT id FROM ModelFormat WHERE identifier = :p", [p: formatId]
                    )
                    //namedParams.put("formats", filteredFormats)
                    String strFormatIds = filteredFormats.join(", ")
                    query ="$query AND r.format.id IN ($strFormatIds)"
                }
                if (filter.take(9) == "submitter") {
                    String personName = filter.drop(10)
                    List filteredUsers = new ArrayList()
                    filteredUsers = User.executeQuery(
                        "SELECT u.id FROM User u JOIN u.person p WHERE p.userRealName = :n",
                        [n: personName]
                    )
                    if (filteredUsers?.size()) {
                        String strOwnerIds = filteredUsers.join(",")
                        query = """\
$query AND m.id IN (SELECT r2.model.id FROM Revision AS r2 \
WHERE r2.owner.id in ($strOwnerIds)  AND r2.model.id = m.id)"""
                    }
                }
            } else {
                // type := < private | shared | public >
                type = filter.drop(5).toLowerCase()
                switch(type?.toLowerCase()) {
                    case "private":
                        query = """\
$query AND r.state = '${ModelState.UNPUBLISHED}' \
AND r.revisionNumber = (SELECT MAX(r2.revisionNumber) FROM Revision As r2 WHERE r2.model.id = m.id)"""
                        break
                    case "shared":
                        query = """\
$query AND m.id IN (SELECT r2.model.id FROM Revision AS r2 WHERE r2.owner.id != :ownerId AND r2.model.id = m.id)"""
                        break
                    case "public":
                        query = """\
$query  AND r.state = '${ModelState.PUBLISHED}' \
AND r.revisionNumber = (SELECT MAX(r2.revisionNumber) FROM Revision As r2 WHERE r2.model.id = m.id)"""
                        break
                    default:
                        if (type) {
                            log.warn("Ignoring unsupported permission level '$type'.")
                        } else  {
                            query = "$query AND r.state = '${ModelState.UNPUBLISHED}'"
                        }
                        break
                }
            }
        } else {
            log.debug("You have provided an invalid filter $filter")
        }

        Map metaParams
        if (offset < 0 || count <= 0) {
            // safety check
            metaParams = [:]
        } else {
            metaParams = [
                max: count, offset: offset
            ]
        }

        query ="$query ORDER BY m.id desc, r.revisionNumber, r.owner.username desc"
        List models = Model.getAll(Model.executeQuery(query, namedParams, metaParams))
        return models
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getModelCount")
    Integer countMyModels(String filter = null, boolean deletedOnly = false) {
        List models = getMyModels(-1, 0, true, ModelListSorting.ID, filter)
        models?.size()
    }

    /** convenience method to check if our filter is OK */
    private boolean filterValid(String filter) {
        return filter && filter.length() >= 3
    }

    /**
     * Returns the Model identified by perennial identifier @p id.
     * @param id The perennial id of the model.
     * @return The Model
     */
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getModel")
    Model getModel(String id) {
        Model model = findByPerennialIdentifier(id)
        if (model) {
            if (!getLatestRevision(model)) {
                throw new AccessDeniedException("No access to the versions of the model with id ${id}".toString())
            }
        } else {
            throw new AccessDeniedException("No access to Model with Id ${id}".toString())
        }

        return model
    }

    Model getModelBySubmissionId(String submissionId) {
        Model.findBySubmissionId(submissionId)
    }


    /**
     * Convenience method for finding a model based on its externally-defined identifiers.
     *
     * @param perennialId The externally-defined ID by which to look up the model.
     * @return  the model corresponding to the given id, or null if there was no match
     */
    @Cacheable('perennialModelIdentifier')
    Model findByPerennialIdentifier(String perennialId) {
        if (!perennialId) {
            return null
        }
        int dot = perennialId.indexOf('.')
        perennialId = -1 == dot ? perennialId : perennialId.substring(0, dot)

        Set<String> idFields = getPerennialIdentifierTypes()
        Model model = Model.withCriteria(uniqueResult: true) {
            or {
                for (String f : idFields) {
                    eq(f, perennialId)
                }
            }
            cache true
        } as Model
        return model
    }

    /**
     * Indicates the kinds of perennial identifiers present in the runtime configuration.
     *
     * @return the set of identifier types that are defined, e.g. submissionId, publicationId.
     */
    @Cacheable('perennialIdentifierTypes')
    @CompileStatic
    @NotTransactional
    Set<String> getPerennialIdentifierTypes() {
        ModelIdentifierGeneratorRegistryService registry = idGeneratorRegistryFactoryBean.object
        registry.generatorTypes
    }

    /**
    * Queries the @p model for the latest available revision the user has read access to.
    * @param model The Model for which the latest revision should be retrieved.
    * @param addToHistory Optional field to allow history not to be modified - e.g. if called from modelhistoryService
    * @return Latest Revision the current user has read access to. If there is no such revision null is returned
    **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getLatestRevision")
    Revision getLatestRevision(Model model, boolean addToHistory = true) {
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
                HAVING rev.revisionNumber = max(revisions.revisionNumber)''', [model: model]) as List<Long>
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
                permissions: [BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()] ]) as List<Long>
        if (!result) {
            return null
        }
        if (addToHistory) {
            modelHistoryService.addModelToHistory(model)
        }
        //ModelPublishedEvent event = new ModelPublishedEvent()
        return Revision.get(result[0])
    }

    /** deduplication method for getting Spring's authorities as Database-Role strings */
    private Set<String> getSpringDatabaseRoles() {
        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add(userService.username)
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
    List<Revision> getAllRevisions(Model model) {
        /*if (model.deleted) {
            return []
        }*/
        // exclude deleted revisions
        modelHistoryService.addModelToHistory(model)
        List<Revision> revisions = model.revisions.toList().findAll { !it.deleted }.sort {it.revisionNumber}
        return revisions
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
    Revision getRevision(String identifier) {
        String[] parts = identifier.split("\\.")
        String modelId = parts[0]
        Model model = findByPerennialIdentifier(modelId)
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
    Revision getRevision(Model model, int revisionNumber) {
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
    Publication getPublication(final Model model) throws AccessDeniedException, IllegalArgumentException {
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
    Model uploadModelAsFile(final RFTC repoFile, ModelTC meta)
            throws ModelException {
        if (repoFile) {
           return uploadModelAsList([repoFile], meta)
        }
        logger.error("No file provided during the update of ${meta.properties}")
        throw new ModelException(meta, "The new version of the model does not have any files.")
    }

    /**
     * @short Adds a new Revision to the model, to be used by SubmissionService
     *
     * The provided @p modelFiles will be stored in the VCS as an update to the existing files of the same @p model.
     * A new Revision will be created and appended to the list of Revisions of the @p model.
     * The revision will not be validated, as the checks are assumed to have been already conducted. The revision
     * will be also indexed if it is successfully added to the model.
     *
     * @param repoFiles     The model files to be stored in the VCS as a new revision
     * @param deleteFiles   The list of files to be deleted from the VCS
     * @param rev           The revision to be added the model which the revision is being associated with
     *
     * @return The newly-added Revision. In case an error occurred while accessing the VCS @c null will be returned.
     * @throws ModelException If either @p model, @p modelFiles or @p comment are null or if the files do not exist
     * or are directories.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.addRevision")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    Revision addRevision(final List<RFTC> repoFiles,
                         final List<RFTC> deleteFiles,
                         final RevisionTC rev,
                         final Map working = null) throws ModelException {
        Revision revision = null
        def txDefinition = [
            // this tx will use a different session than the current one
            propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW
        ]
        Revision.withTransaction(txDefinition) {
            // the returned revision is detached from the Hibernate session

            revision = doPersistRevision(repoFiles, deleteFiles, rev)
        }
        Revision attachedRevision = doPostPersistRevision(revision, working)
        return attachedRevision ?: revision
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.amendRevision")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    Revision amendRevision(final List<RFTC> repoFiles,
                           final List<RFTC> deleteFiles,
                           final RevisionTC rev,
                           final Map working = null) throws ModelException {
        logger.debug("Amending the revision: ${rev.dump()}")
        Revision revision = null
        def txDefinition = [
            // this tx will use a different session than the current one
            propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW
        ]
        Revision.withTransaction(txDefinition) {
            // the returned revision is detached from the Hibernate session
            revision = doAmendRevision(repoFiles, deleteFiles, rev)
        }
        Revision attachedRevision = doPostPersistRevision(revision, working)
        return attachedRevision ?: revision
    }
    /**
     * Persists a new model revision in the database and upload the repository files in VCS.
     *
     * This unit of work is performed in a dedicated transaction.so as to ensure that it is
     * committed before the [synchronous] indexing process tries to load the revision from the
     * database.
     * @param repoFiles         the files to be associated with the revision
     * @param deleteFiles       the files to be deleted and compared to the previous revision
     * @param rev               the transport command from which to construct the new revision
     *
     * @return the new revision
     * @throws ModelException if there is no model associated with @p rev, if its model has
     * been deleted or if the comment is null.
     */
    Revision doPersistRevision(List<RFTC> repoFiles,
                               List<RFTC> deleteFiles,
                               RevisionTC rev) throws ModelException {
        StopWatch stopWatch = new Log4JStopWatch("modelService.doPersistRevision")
        // TODO: the method should be thread safe, add a lock
        validateModelRevision(rev)
        List<File> modelFiles = repositoryFileService.getFilesFromRF(repoFiles)
        List<File> filesToDelete = repositoryFileService.getFilesFromRF(deleteFiles)
        final User currentUser = User.findByUsername(springSecurityService.authentication.name)
        final String PERENNIAL_ID = (rev.model.publicationId) ?: (rev.model.submissionId)
        final String formatVersion = rev.format.formatVersion ?: modelFileFormatService.getFormatVersion(rev)
        Model model = getModel(PERENNIAL_ID)
        if (rev.format.identifier == "SBML") {
            addModelIdentifiersAsAnnotation(rev)
        }
        // initialise a new revision
        Revision revision = new Revision(model: model, name: rev.name, description: rev.description,
                    comment: rev.comment, uploadDate: new Date(), owner: currentUser,
                    validated: rev.validated, curationState: rev.curationState, minorRevision: rev.minorRevision,
                    format: ModelFormat.findByIdentifierAndFormatVersion(rev.format.identifier, formatVersion),
                    validationReport: rev.validationReport, validationLevel: rev.validationLevel,
                    readmeSubmission: rev.readmeSubmission)
        List<RepositoryFile> domainObjects = repositoryFileService.convertRFTCToRF(repoFiles, revision)

        revision = putFilesUnderVcs(model, revision, domainObjects, modelFiles, filesToDelete, false)

        // calculate the new revision number - accessing the revisions directly to circumvent ACL
        revision.revisionNumber = model.revisions.sort {it.revisionNumber}.last().revisionNumber + 1

        if (revision.validate()) {
            model.addToRevisions(revision)
            doUpdateModelMetadata(model, rev)
            revision.save()
            model.save(flush: true)
            doUpdatePermissions(model, revision, currentUser)

            // !! THIS HAS TO BE IN A SEPARATE METHOD WITH A DEDICATED TRANSACTION CONTEXT !!
            /*grailsApplication.mainContext.publishEvent(new RevisionCreatedEvent(this,
                   new RevisionAdapter(revision: revision).toCommandObject(), vcsService.retrieveFiles(revision)))*/
        } else {
            discardFailedRevision(model, revision, repoFiles)
        }
        stopWatch.stop()
        return revision
    }

    Revision doAmendRevision(List<RFTC> repoFiles,
                             List<RFTC> deleteFiles,
                             RevisionTC rev) throws ModelException {
        StopWatch stopWatch = new Log4JStopWatch("modelService.doAmendRevision")
        validateModelRevision(rev)

        List<File> modelFiles = repositoryFileService.getFilesFromRF(repoFiles)
        List<File> filesToDelete = repositoryFileService.getFilesFromRF(deleteFiles)
        final User currentUser = User.findByUsername(springSecurityService.authentication.name)
        final String PERENNIAL_ID = (rev.model.publicationId) ?: (rev.model.submissionId)
        Model model = getModel(PERENNIAL_ID)
        // fetch the current revision from the database
        Revision revision = Revision.get(rev.id)
        if (rev.format.identifier == "SBML") {
            addModelIdentifiersAsAnnotation(rev)
        }
        // update the revision with the potential updates populated in the rev argument
        doUpdateRevision(revision, rev)
        List<RepositoryFile> domainObjects = repositoryFileService.convertRFTCToRF(repoFiles, revision)

        revision = putFilesUnderVcs(model, revision, domainObjects, modelFiles, filesToDelete, true)

        if (revision.validate()) {
            doUpdateModelMetadata(model, rev)
            revision.save()
            model.save(flush: true)
            doUpdatePermissions(model, revision, currentUser)
        } else {
            discardFailedRevision(model, revision, repoFiles)
            revision = null
        }
        stopWatch.stop()
        revision
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
    @PreAuthorize("permitAll()")
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.getFileDetails")
    List<VcsFileDetails> getFileDetails(Revision rev, String filename) {
        def details = vcsService.getFileDetails(rev, filename)
        def accessibleRevs = getAllRevisions(rev.model)
        final boolean IS_ANON = !springSecurityService.isLoggedIn() &&
            SpringSecurityUtils.ifAllGranted('ROLE_ANONYMOUS')
        final boolean IS_ADMIN = !IS_ANON && SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")
        return details.findAll { detail ->
            boolean retval = false
            accessibleRevs.each { revision ->
                final boolean CAN_READ = IS_ADMIN || aclUtilService.hasPermission(
                    springSecurityService.authentication, revision, BasePermission.READ)
                if (IS_ANON || CAN_READ) {
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
    Model uploadValidatedModel(final List<RFTC> repoFiles,
            RevisionTC rev, final Map working = null) throws ModelException {
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
            doPostPersistRevision(r, working)
            return attachedModel
        }
        model
    }

    @PostLogging(LoggingEventType.CREATION)
    @Profiled(tag="modelService.doUploadValidatedModel")
    Model doUploadValidatedModel(final List<RFTC> repoFiles,
            RevisionTC rev) throws ModelException {
        logger.debug "About to store the following model: ${rev.name}"
        // TODO: to support anonymous submissions, this method has to be changed
        if (Revision.findByName(rev.name)) {
            final String msg = "There is already a Model with name ${rev.name}".toString()
            logger.warn(msg)
        }
        ModelBuilder modelBuilder = new ModelBuilder(repoFiles, rev).build()
        Model model = modelBuilder.model
        Revision revision = modelBuilder.revision
        if (revision.validate()) {
            model.addToRevisions(revision)
            if (!model.validate()) {
                modelBuilder.discard()
            } else {
                modelBuilder.persist()
            }
            // don't broadcast event yet, wait for the current tx to commit
            /*grailsApplication.mainContext.publishEvent(new ModelCreatedEvent(this,
                                new ModelAdapter(model: model).toCommandObject(), modelFiles))*/
        } else {
            modelBuilder.discard()
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
    // TODO: delete the following method and its usages
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostLogging(LoggingEventType.CREATION)
    @Profiled(tag="modelService.uploadModelAsList")
    Model uploadModelAsList(final List<RFTC> repoFiles, ModelTC meta)
            throws ModelException {
        def stopWatch = new Log4JStopWatch("modelService.uploadModelAsList.sanityChecks")
        // TODO: to support anonymous submissions this method has to be changed
        if (!repoFiles || repoFiles.size() == 0) {
            logger.error("No files were provided as part of the submission of model ${meta.properties}")
            throw new ModelException(meta, "A model must contain at least one file.")
        }
        Model model = new Model()
        List<File> modelFiles = []
        for (rf in repoFiles) {
            if (!rf || !rf.path) {
                logger.error("No file was provided as part of the submission of model ${meta.properties}")
                throw new ModelException(meta, "Please supply at least one file for this model.")
            }
            final String path = rf.path
            if (!path || path.isEmpty()) {
                logger.error("Null file encountered while creating ${meta.properties}: ${repoFiles.properties}")
                throw new ModelException(meta,
                    "Sorry, there was a problem with one of the files you submitted. Please refine the files you wish to upload and try again.")
            }
            final def f = new File(path)
            if (!f.exists()) {
                logger.error("Non-existent file detected while uploading a new revision for ${meta.properties}: ${f.properties}")
                throw new ModelException(meta,
                    "Sorry, one of the files you submitted does not appear to exist. Please refine the files you wish to upload and try again")
            }
            if (f.isDirectory()) {
                logger.error("Folder detected while uploading a new revision for ${meta.properties}: ${repoFiles.properties}")
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
            logger.error(err)
       //     throw new ModelException(meta, "Invalid ${meta.format.identifier} submission.")
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
        final String submissionId
        synchronized (this) {
            submissionId = submissionIdGenerator.generate()
        }
        String modelPath = new StringBuilder(timestamp).append("_").append(submissionId).
                append(File.separator).toString()
        File modelFolder = new File(container, modelPath)
        boolean success = modelFolder.mkdirs()
        if (!success) {
            def err = "Cannot create the directory where the ${name} should be stored"
            logger.error(err)
            throw new ModelException(meta, err.toString())
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
        RevisionTC revisionTC = new RevisionAdapter(revision: revision).toCommandObject()

        // keep a list of RFs closely, as we may need to discard all of them
        List<RepositoryFile> domainObjects =
            repositoryFileService.convertRFTCToRF(repoFiles, revision)
        String formatVersion = modelFileFormatService.getFormatVersion(revisionTC)
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
            def m = new ModelAdapter(model: model, latest: revision).toCommandObject()
            errMsg.append("${m.properties} to VCS: ${e.getMessage()}.\n")
            errMsg.append("${model.errors.allErrors.inspect()}\n")
            errMsg.append("${revision.errors.allErrors.inspect()}\n")
            logger.error(errMsg.toString())
            stopWatch.stop()
            throw new ModelException(m,
                "Could not store new Model $m.properties} in VCS".toString(), e)
        }
        stopWatch.lap("Finished importing model in VCS.")
        stopWatch.setTag("modelService.uploadModelAsList.gormValidation")
        domainObjects.each {
            revision.addToRepoFiles(it)
        }
        if (revision.validate()) {
            model.addToRevisions(revision)
            if (meta.publication) {
                model.publication = publicationService.fromCommandObject(meta.publication)
            }
            if (!model.validate()) {
                // TODO: this means we have imported the file into the VCS, but it failed to be saved in the database, which is pretty bad
                revision.discard()
                model.discard()
                def msg  = new StringBuffer("New Model ${name} does not validate:\n")
                msg.append("${model.errors.allErrors.inspect()}\n")
                msg.append("${revision.errors.allErrors.inspect()}\n")
                logger.error(msg.toString())
                stopWatch.stop()
                ModelAdapter adapter = new ModelAdapter(model: model, latest: revision).toCommandObject() as ModelAdapter
                msg = "New model does not validate"
                throw new ModelException(adapter.toCommandObject(), msg)
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
            grailsApplication.mainContext.publishEvent(new ModelCreatedEvent(this,new ModelAdapter(model: model).toCommandObject(), modelFiles))
        } else {
            // TODO: this means we have imported the file into the VCS, but it failed to be saved in the database, which is pretty bad
            revision.discard()
            domainObjects.each {it.discard()}
            model.discard()
            logger.error("New Model ${model.properties} with properties ${meta.properties} does not validate:${revision.errors.allErrors.inspect()}")
            throw new ModelException(new ModelAdapter(model: model).toCommandObject(), "Sorry, but the new Model does not seem to be valid.")
        }
        return model
    }

    /**
    * Adds a new Revision to the model.
    *
    * The provided @p file will be stored in the VCS as an update to an existing file of the same @p model.
    * A new Revision will be created and appended to the list of Revisions of the @p model.
    * @param model The Model the revision should be added
    * @param repoFile The RFTC object corresponding to the file that is to be stored in the VCS as a new revision
    * @param format The format of the model file
    * @param comment The commit message for the new revision
    * @return The new added Revision. In case an error occurred while accessing the VCS @c null will be returned.
    * @throws ModelException If either @p model, @p file or @p comment are null or if the file does not exists or is a directory
    */
    @PreAuthorize("hasPermission(#model, write) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.addRevisionAsFile")
    Revision addRevisionAsFile(Model model, final RFTC repoFile,
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
    Revision addRevisionAsList(Model model, final List<RFTC> repoFiles,
                               final ModelFormat format, final String comment) throws ModelException {
        // TODO: the method should be thread safe, add a lock
        if (!model) {
            throw new ModelException(null, "Model may not be null")
        }
        ModelTC mtc = new ModelAdapter(model: model).toCommandObject(false)
        if (model.deleted) {
            throw new ModelException(mtc, "A new Revision cannot be added to a deleted model")
        }
        if (comment == null) {

            throw new ModelException(mtc, "Comment may not be null, empty comment is allowed")
        }
        if (!repoFiles || repoFiles.size() == 0) {
            logger.error("No files were provided as part of the update of model ${model.properties}")
            throw new ModelException(mtc, "A new version of the model must contain at least one file.")
        }
        List<File> modelFiles = []
        for (rf in repoFiles) {
            if (!rf || !rf.path) {
                logger.error("No file was provided as part of the update of model ${model.properties}")
                throw new ModelException(mtc, "Please supply at least one file for the new version of this model.")
            }
            final String path = rf.path
            if (!path || path.isEmpty()) {
                logger.error("Null file encountered while uploading a new revision for ${model.properties}: ${repoFiles.properties}")
                throw new ModelException(mtc,
                    "Sorry, there was something wrong with one of the files you submitted. Please refine the files you wish to upload and try again.")
            }
            final def f = new File(path)
            if (!f.exists()) {
                logger.error("Non-existent file detected while uploading a new revision for ${model.properties}: ${f.properties}")
                throw new ModelException(mtc,
                    "Sorry, one of the files you submitted does not appear to exist. Please refine the files you wish to upload and try again")
            }
            if (f.isDirectory()) {
                logger.error("Folder detected while uploading a new revision for ${model.properties}: ${repoFiles.properties}")
                throw new ModelException(mtc,
                    "Sorry, we currently do not accept model organised into sub-folders.")
            }
            if (rf.mainFile && f.length() == 0) {
                def err = "File ${f.name} cannot be empty because it is the main file of the submission."
                logger.error err
                throw new ModelException(mtc, err)
            }
            modelFiles.add(f)
        }
        boolean valid = true
        if (!modelFileFormatService.validate(modelFiles, format, [])) {
            logger.warn("""\
New revision of model ${mtc.properties} containing ${modelFiles.inspect()} does not comprise valid ${format
                .identifier}""")
            //throw new ModelException(m, "The file list does not comprise valid ${format.identifier}")
            valid = false
        }

        final User currentUser = User.findByUsername(springSecurityService.authentication.name)
        Revision revision = new Revision(model: model, name: modelFileFormatService.extractName(modelFiles, format),
                        description: modelFileFormatService.extractDescription(modelFiles, format), comment: comment,
                        uploadDate: new Date(), owner: currentUser,
                minorRevision: false, validated:valid)
        RevisionTC revisionTC = new RevisionAdapter(revision: revision).toCommandObject()

        List<RepositoryFile> domainObjects = repositoryFileService.convertRFTCToRF(repoFiles, revision)
        String formatVersion = modelFileFormatService.getFormatVersion(revisionTC)
        revision.format = ModelFormat.findByIdentifierAndFormatVersion(format.identifier, formatVersion)

        // save the new model in the database
        try {
            String vcsId = vcsService.updateModel(model, modelFiles, null, comment)
            revision.vcsId = vcsId
        } catch (VcsException e) {
            revision.discard()
            domainObjects.each{ it.discard() }
            logger.error("Exception occurred during uploading a new Model Revision to VCS: ${e.getMessage()}")
            throw new ModelException(mtc,
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
            RevisionCE revisionCE = new RevisionCE(this, revisionTC, vcsService.retrieveFiles(revision))
            grailsApplication.mainContext.publishEvent(revisionCE)
        } else {
            // TODO: this means we have imported the revision into the VCS, but it failed to be saved in the database, which is pretty bad
            revision.discard()
            logger.error("New Revision containing ${repoFiles.inspect()} for Model ${mtc} with VcsIdentifier ${model.vcsIdentifier} added to VCS, but not stored in database")
            throw new ModelException(mtc, "Revision stored in VCS, but not in database")
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
    Boolean canAddRevision(final Model model) {
        if (model.deleted) {
            return false
        }
        return (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN") ||
                aclUtilService.hasPermission(springSecurityService.authentication, model, BasePermission.WRITE))
    }

    /**
     * Retrieves the model files for the @p revision.
     *
     * This service is currently being used to retrieve the files of the public models.
     * The other use is to retrieve the files of work flows where the authentication was established.
     * Therefore, we do not need to check security before fetching the resources.
     *
     * @param revision The Model Revision for which the files should be retrieved.
     * @return Byte Array of the content of the Model files for the revision.
     * @throws ModelException In case retrieving from VCS fails.
     */
    //@PreAuthorize("hasPermission(#revision, read) or hasRole('ROLE_ADMIN')") Not working. Seems related to: https://bitbucket.org/jummp/jummp/issue/23/spring-security-doesnt-work-as-expected-in
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.retrieveFiles")
    List<File> retrieveFiles(final Revision revision) throws ModelException {
        List<File> files
        try {
            files = repositoryFileService.retrieveFiles(revision)
        } catch (VcsException e) {
            String message = "Retrieving Revision ${revision.vcsId} for Model ${revision.name} from VCS failed."
            logger.error(message, e)
            ModelTC model = new ModelAdapter(model: revision.model,
                latest: revision).toCommandObject()
            throw new ModelException(model, message, e)
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
    List<RFTC> retrieveModelFiles(final Revision revision) throws ModelException {
        if (aclUtilService.hasPermission(springSecurityService.authentication, revision, BasePermission.READ)
                || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
            return repositoryFileService.getRepositoryFilesForRevision(revision)
        } else {
            logger.error "you can't access revision ${revision.id}!"
            throw new AccessDeniedException("Sorry you are not allowed to download this Model")
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
    List<RFTC> retrieveModelFiles(final Model model) throws ModelException {
        final Revision revision = getLatestRevision(model, false)
        if (!revision) {
            logger.error("you cant access model ${model}")
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
    void grantReadAccess(Model model, User collaborator) {
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
                if (canRead || isAdmin) {
                    aclUtilService.addPermission(it, username, BasePermission.READ)
                }
            }
        }
        Map notification = [
                model: new ModelAdapter(model: model).toCommandObject(),
                user: springSecurityService.currentUser,
                grantedTo: collaborator,
                perms: getPermissionsMap(model)]
        sendMessage("seda:model.readAccessGranted", notification)
    }

    private static String getPermissionString(int p) {
        switch (p) {
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
    Collection<PermissionTransportCommand> getPermissionsMap(Model model, boolean authenticated = true) {
        def map = new HashMap<Integer, PermissionTransportCommand>()
        if (!authenticated || aclUtilService.hasPermission(springSecurityService.authentication, model,
                    BasePermission.ADMINISTRATION ) || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
            def permissions = aclUtilService.readAcl(model).getEntries()
            for (AccessControlEntry it: permissions) {
                String permission = getPermissionString(it.getPermission().getMask())
                // TODO refactor permission code to handle both GrantedAuthoritySid and PrincipalSid
                if (it.sid instanceof GrantedAuthoritySid) {
                    continue // skip to the next one
                }
                String principal = it.getSid().principal
                if (permission) {
                    User user = User.findByUsername(principal)
                    if (user) {
                        String userRealName = user.person.userRealName
                        int userId = (int) user.id
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
                        } else {
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
        } else {
            throw new AccessDeniedException("You can't access permissions if you don't have them.")
        }

        map.values()
    }

    /**
     * Grants permissions to a @model given a list of @permissions
     *
     * @param model A {@link Model} object representing the model in question.
     * @param permissions A list of wrapper objects {@link PermissionTransportCommand} which encapsulate a user, a model or revision and granted permissions
     */
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="modelService.setPermissions")
    void setPermissions(Model model, List<PermissionTransportCommand> permissions) {
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
                } else {
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
        } else {
            throw new AccessDeniedException("You can't access the model ${model.submissionId} if you don't have required permissions.")
        }
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
    void grantWriteAccess(Model model, User collaborator) {
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
                if (!hasAdminPermission(it, collaborator.username)) {
                    aclUtilService.addPermission(it, collaborator.username, BasePermission.ADMINISTRATION)
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
    boolean revokeReadAccess(Model model, User collaborator) {
        final String principal = collaborator.username
        if (principal == springSecurityService.authentication.name) {
            // the user cannot revoke his own rights
            return false
        }
        boolean isCurator = userService.isCurator(collaborator)
        Set<Revision> revisions = model.revisions
        try {
            boolean check = hasPermission(model, BasePermission.READ as BasePermission)
            if (check) {
                aclUtilService.deletePermission(model, principal, BasePermission.READ)
            }
            check = hasPermission(model, BasePermission.WRITE as BasePermission)
            if (check) {
                aclUtilService.deletePermission(model, principal, BasePermission.WRITE)
            }
            if (isCurator) {
                aclUtilService.deletePermission(model, principal, BasePermission.ADMINISTRATION)
                revisions.each { Revision r ->
                    aclUtilService.deletePermission(r, principal, BasePermission.ADMINISTRATION)
                    aclUtilService.deletePermission(r, principal, BasePermission.READ)
                }
            } else {
                boolean adminToModel = hasAdminPermission(model, principal)
                if (adminToModel) {
                    check = hasPermission(model, BasePermission.ADMINISTRATION as BasePermission)
                    if (check) {
                        aclUtilService.deletePermission(model, principal, BasePermission.ADMINISTRATION)
                    }
                }
                final boolean isAdmin = SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')
                for (Revision revision in revisions) {
                    boolean canRead = hasPermission(revision, BasePermission.READ as BasePermission)
                    if (canRead || isAdmin) {
                        try {
                            aclUtilService.deletePermission(revision, principal, BasePermission.READ)
                        } catch (Exception e) {
                            logger.error e.message, e
                            return false
                        }
                    }
                }
            }
            return true
        } catch (NotFoundException notFoundException) {
            logger.error("An error has occurred when trying to revoke non-exist permission... The error details could be found below.")
            notFoundException.printStackTrace()
        }
        return false
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
            if (!(ace.sid instanceof PrincipalSid)) {
                return null
            }
            def aceAsPrincipalSid = ace.sid as PrincipalSid
            aceAsPrincipalSid.principal == username &&
                    ace.permission == BasePermission.ADMINISTRATION
        }
    }

    /**
     * Determines whether a user or role has a specific permission on a given model or revision or not
     *
     * @param modelOrRevision   A domain object which is either {@link Revision} or {@link Model}
     * @param usernameOrRole    A string representing the username or authority (i.e. role)
     * @param perm              An object representing a permission which the type is of {@link BasePermission}
     * @return                  true/false
     */
    boolean hasPermission(def modelOrRevision, final String usernameOrRole, final BasePermission perm) {
        Acl acl = aclUtilService.readAcl(modelOrRevision)
        return null != acl.entries.find { ace ->
            if (!(ace.sid instanceof PrincipalSid) || !(ace.sid instanceof GrantedAuthoritySid)) {
                return false
            }
            def sid
            String currentUsernameOrRole = ""
            if (ace.sid instanceof PrincipalSid) {
                sid = ace.sid as PrincipalSid
                currentUsernameOrRole = sid.principal
            } else if (ace.sid instanceof GrantedAuthoritySid) {
                sid = ace.sid as GrantedAuthoritySid
                currentUsernameOrRole = sid.grantedAuthority
            } else {
                sid = null
            }
            boolean value = ace.permission == perm && currentUsernameOrRole == usernameOrRole
            boolean doesItHavePermission = sid == null ? false : value
            return doesItHavePermission
        }
    }

    boolean hasPermission(def domainObject, final BasePermission perm) {
        aclUtilService.hasPermission(springSecurityService.authentication, domainObject, perm)
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
    boolean revokeWriteAccess(Model model, User collaborator) {
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
     * Transfers the ownership of the @p model to @p contributor.
     *
     * The ownership can only be transferred from a user having the right to grant
     * read/write access and the @p model is not yet under curation or published.
     * The @p contributor has to have read access to future revisions of the model.
     *
     * All Model specific rights are revoked from the owner and granted to the @p contributor.
     * This includes:
     * @li Write access to the @p model
     * @li Read access to future revisions of the @p model
     * @li Start of curation
     * @li Grant/Revoke read/write access to the @p model
     * @param model The Model for which the ownership should be transferred.
     * In the first implementation, we suppose that all revisions are submitted by the same submitter/owner.
     *
     * @param contributor The User who becomes the new owner
     */
    @PreAuthorize("hasPermission(#model, admin) or hasRole('ROLE_ADMIN')")
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.transferOwnership")
    void transferOwnership(Model model, User contributor) {
        // this doesn't work: aclUtilService.changeOwner(model, contributor.username)
        Set<Revision> revisions = model.revisions.sort { r1, r2 ->
            r1.revisionNumber <=> r2.revisionNumber
        }
        User currentOwner = revisions.first().owner
        // step 1: replace the current owner with the contributor
        for (Revision revision in revisions) {
            revision.owner = contributor
            revision.save(flush: true)
        }

        // step 2: give the new owner full permission to the model and all revisions
        grantWriteAccess(model, contributor)
        revisions.each { Revision rev ->
            aclUtilService.addPermission(rev, contributor.username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(rev, contributor.username, BasePermission.READ)
            aclUtilService.addPermission(rev, contributor.username, BasePermission.WRITE)
        }

        // step 3: revoke all permissions granted to the former owner
        revokeReadAccess(model, currentOwner)
        revokeWriteAccess(model, currentOwner)
        revisions.each { Revision rev ->
            aclUtilService.deletePermission(rev, currentOwner.username, BasePermission.ADMINISTRATION)
            aclUtilService.deletePermission(rev, currentOwner.username, BasePermission.READ)
            aclUtilService.deletePermission(rev, currentOwner.username, BasePermission.WRITE)
        }

        // step 4: remapping contribution details for the new contributor on the model
        boolean succeeded = changeContributionDetails(revisions, contributor)
        if (succeeded) {
            logger.info("""Remapping contribution details for ${contributor.username} \
on ${model.submissionId} completed successfully.""")
        } else {
            logger.info("""Errors have occurred when remapping contribution details for \
${contributor.username} on ${model.submissionId}. Please check the data and complete the task manually.""")
        }
    }

    private static boolean changeContributionDetails(final Set<Revision> revisions, final User contributor) {
        Integer count = 0
        List<Integer> result = new ArrayList<>()
        for (Revision revision: revisions) {
            List<CD> details = CD.findAllByRevision(revision)
            details.each { CD cd ->
                try {
                    Integer r = CD.executeUpdate("""UPDATE ContributionDetails cd \
SET cd.contributor.id=:newContributorId \
WHERE cd.contributor.id=:oldContributorId
AND cd.revision.id=:oldRevisionId AND cd.role.id=:oldRoleId""",
                        [newContributorId: contributor.id,
                         oldContributorId: cd.contributor.id,
                         oldRevisionId   : cd.revision.id,
                         oldRoleId       : cd.role.id] as Map)
                    if (1 == r) {
                        result.add(r)
                    }
                } catch (Exception exception) {
                    logger.error("""An error has occurred when remapping contribution for ${contributor.username} \
on the revision ${revision.getId()}: ${revision.getName()} caused by:""")
                    exception.printStackTrace()
                } finally {
                    count++
                }
            }
        }
        // check the result
        result?.size() == count
    }

    /**
     * Checks if the model can be deleted.
     *
     * A model can be deleted when the model is not null and has not been deleted and meets one of the following
     * criteria:
     * - the user doing so is an administrator
     * - the model does not have any public revision and the user doing so the right permissions
     *   to delete it
     *
     * @param model The Model to be deleted
     * @return @c true in case the Model can be deleted, @c false otherwise.
     */
    @PostLogging(LoggingEventType.DELETION)
    @Profiled(tag="modelService.canDelete")
    boolean canDelete(Model model) {
        if (!model || model.deleted) {
            return false
        }
        boolean isAdmin = SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")
        boolean publicRev = hasPublicRevision(model)
        boolean hasDeleteRight = aclUtilService.hasPermission(
                springSecurityService.authentication, model, BasePermission.DELETE)
        return isAdmin || (hasDeleteRight && !publicRev)
    }

    /**
    * Checks if the model can be shared
    *
    * @param model The Model to be shared
    * @return @c true in case the Model can be shared, @c false otherwise.
    **/
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="modelService.canShare")
    boolean canShare(Model model) {
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
     * Updated: we relax the constraints of the model deletion in the sense of
     * allowing administrators to archive models even if the model has public revisions
     *
     * Deletion of @p model is only possible if the model is neither under curation nor published.
     * @param model The Model to be deleted
     * @return @c true in case the Model has been deleted, @c false otherwise.
     * @see ModelService#restoreModel(Model model)
     */
    /*@PreAuthorize("hasPermission(#model, delete) or hasRole('ROLE_ADMIN')")*/ //Doesnt work
    @PostLogging(LoggingEventType.DELETION)
    @Profiled(tag="modelService.deleteModel")
    boolean deleteModel(Model model) {
        if (!model) {
            throw new IllegalArgumentException("Cannot delete a null model")
        }
        if (model.deleted) {
            throw new IllegalArgumentException("The model ${model?.submissionId} has been already deleted")
        }
        boolean canDelete = canDelete(model)
        if (!canDelete) {
            throw new IllegalStateException("Cannot delete the model ${model.submissionId}")
        }
        model.deleted = true
        boolean succeed = model.save(flush: true)
        if (succeed) {
            grailsApplication.mainContext.publishEvent(new ModelDeletedEvent(this,
                new ModelAdapter(model: model).toCommandObject()))
        }
        return succeed
    }

    @PostLogging(LoggingEventType.DELETION)
    @Profiled(tag="modelService.undeleteModel")
    boolean undeleteModel(Model model) {
        println model?.dump()
        if (!model) {
            throw new IllegalArgumentException("Cannot undelete a null model")
        }
        /*if (model.deleted) {
            throw new IllegalArgumentException("The model ${model?.submissionId} has been already deleted")
        }
        boolean canDelete = canDelete(model)
        if (!canDelete) {
            throw new IllegalStateException("Cannot delete the model ${model.submissionId}")
        }*/
        model.deleted = false
        boolean succeed = model.save(flush: true)
        if (succeed) {
            logger.info("${model.submissionId} has been deleted (aka. archived).")
            ModelDeletedEvent event = new ModelDeletedEvent(this, new ModelAdapter(model: model).toCommandObject())
            grailsApplication.mainContext.publishEvent(event)
        }
        return succeed
    }

    void deleteModelWorkingDirectory(final Model model) throws IOException {
        String workingDirectory = grailsApplication.config.jummp.vcs.workingDirectory
        String modelDirectory = model.vcsIdentifier
        Path absModelDir = Paths.get(workingDirectory, modelDirectory)
        fileSystemService.deleteDirectory(absModelDir)
    }
    /*
     * Convenience method that checks whether a model has any publicly-available revision.
     *
     * @param model the model for which to verify the publication status.
     */
    private boolean hasPublicRevision(Model model) {
        def publicRevision = Revision.withCriteria(uniqueResult: true) {
            maxResults(1)
            and {
                eq("model", model)
                or {
                    eq("state", ModelState.PUBLISHED)
                    eq("state", ModelState.RELEASED)
                }
            }
        }
        if (!publicRevision) {
            return false
        }
        aclUtilService.hasPermission(createAnonymousAuthToken(), publicRevision, BasePermission.READ)
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
    boolean restoreModel(Model model) {
        if (!model) {
            throw new IllegalArgumentException("Model may not be null")
        }
        if (!Model.exists(model.id)) {
            throw new IllegalArgumentException("Model ${model.properties} absent from database")
        }
        if (!model.deleted) {
            return false
        }

        model.deleted = false
        model.save(flush: true)
        model.refresh()
        grailsApplication.mainContext.publishEvent(new ModelRestoredEvent(this,
            new ModelAdapter(model: model).toCommandObject()))
        return !model.deleted
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
    boolean deleteRevision(Revision revision) {
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
     * Only a Curator or an Administrator are allowed to call this method.
     * @param revision The Revision to be published
     */
    @PostLogging(LoggingEventType.PUBLISH)
    @Profiled(tag="modelService.canPublish")
    boolean canPublish(Revision revision) {
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
     * Tests if the user can unpublish this revision
     * Only a Curator or an Administrator are allowed to call this method.
     * @param revision The Revision to be unpublished/unreleased
     */
    @PostLogging(LoggingEventType.UNPUBLISH)
    @Profiled(tag="modelService.canUnpublish")
    boolean canUnpublish(Revision revision) {
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
    boolean canSubmitForPublication(Revision revision) {
        if (!revision) {
            return false
        }
        if (revision.deleted) {
            return false
        }
        if (revision.model.deleted) {
            return false
        }
        // the condition 1: the currently logged user is neither curator nor reviewer
        boolean cond1 = SpringSecurityUtils.ifAnyGranted("ROLE_CURATOR,ROLE_REVIEWER")
        // the condition 2: the currently logged user is the model owner
        boolean cond2 = revision.owner == springSecurityService.currentUser
        if (!cond1 && cond2) {
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
    boolean canValidate(Revision revision) {
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
     * Check if the current user could do a consistency check. Only logged-in user who has
     * read permission on the revision can check consistency of its content.
     *
     * @param   revision    The revision object is checked consistency
     * @return  logical val true/false will be returned depending on the combined criteria
     */
    boolean canCheckConsistency(Revision revision) {
        final boolean isAnonymous = !springSecurityService.isLoggedIn() &&
                                    SpringSecurityUtils.ifAllGranted('ROLE_ANONYMOUS')
        final boolean isAuthenticated = !isAnonymous
        final boolean isReadable = SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN") ||
            aclUtilService.hasPermission(springSecurityService.authentication, revision, [BasePermission.READ])
        final boolean isAccessible = isAuthenticated && isReadable
        isAccessible
    }

    /**
     * Updates the curation state of a revision.
     *
     * If this is revision is already published and state is CURATED, then we also generate a
     * publicationId that gets written to the model file. In this case, a revision is created
     * and returned.
     *
     * @param revision the revision for which to set the curation state.
     * @param state the new curation state
     * @return a revision with the updated curation state that has been persisted into the database
     */
    Revision updateRevisionCurationState(Revision revision, CurationState state) {
        revision.setCurationState(state)
        String perennialId = revision.model.publicationId
        boolean missingPerennialId = !perennialId || perennialId == ""
        if (CurationState.CURATED == state && isRevisionPublic(revision) && missingPerennialId) {
            Revision updated = doBeforePublishingCuratedRevision(revision)
            if (null != updated && updated != revision) {
                // ask Hibernate to not persist the original revision
                // so that only the updated one is curated and public
                revision.discard()
                // we've just added a new private revision atop of a public one. make HEAD public
                markRevisionAsPublic updated
                return updated.save(flush: true)
            }
        }
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
    @PreAuthorize("hasRole('ROLE_CURATOR') or hasRole('ROLE_ADMIN')") //used to be: (hasRole('ROLE_CURATOR') and hasPermission(#revision, admin))
    @PostLogging(LoggingEventType.PUBLISH)
    @Profiled(tag="modelService.publishModelRevision")
    Revision publishModelRevision(Revision revision) {
        if (!SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            if (!aclUtilService.hasPermission(springSecurityService.authentication, revision,
                        BasePermission.ADMINISTRATION)) {
                throw new AccessDeniedException("You cannot publish this model.")
            }
        }
        if (!revision) {
            throw new IllegalArgumentException("Revision may not be null")
        }
        if (revision.deleted) {
            throw new IllegalArgumentException("Revision may not be deleted")
        }
        Model model = revision.model
        boolean curatedModel = isCurated(revision)
        boolean missingPerennialId = !model.publicationId || model.publicationId == ""
        if (MAKE_PUBLICATION_ID && curatedModel && missingPerennialId) {
            revision = doBeforePublishingCuratedRevision(revision)
        }
        model.firstPublished = new Date()
        markRevisionAsPublic(revision)
        if (!model.save(flush: true)) {
            ModelTC cmd = new ModelAdapter(model: model, latest: revision).toCommandObject(false)
            throw new ModelException(cmd,
                "Cannot publish model ${model.submissionId}:${model.errors.allErrors.inspect()}")
        }
        //ModelPublishedEvent event = new ModelPublishedEvent(new Object(), cmd)
        //grailsApplication.mainContext.publishEvent(event)

        // publish the revision files to EBI's FTP for downloading
        if (grailsApplication.isWarDeployed()) {
            copyRevisionFilesToFtp(revision)
        }

        revision
    }

    /*
     * Sets ACL permissions and the revision state to published.
     * Any user, whether logged in or not, can read public revisions.
     */
    private void markRevisionAsPublic(Revision revision) {
        aclUtilService.addPermission(revision, "ROLE_USER", BasePermission.READ)
        aclUtilService.addPermission(revision, "ROLE_ANONYMOUS", BasePermission.READ)
        revision.state = ModelState.PUBLISHED
    }

    /*
     * Returns true if a revision can be read by anonymous users and has state ModelState.PUBLISHED
     * and false otherwise.
     */
    boolean isRevisionPublic(Revision revision) {
        (revision.state == ModelState.PUBLISHED || revision.state == ModelState.RELEASED) &&
            aclUtilService.hasPermission(createAnonymousAuthToken(), revision, BasePermission.READ)
    }

    /*
     * Convenience method for constructing anonymous authentication tokens.
     *
     * Useful when wishing to check whether anonymous users have permissions for a model/revision.
     * See
     *      https://github.com/spring-projects/spring-security/blob/3.2.9.RELEASE/core/src/main/java/org/springframework/security/authentication/AnonymousAuthenticationToken.java#L42
     *      https://github.com/grails-plugins/grails-spring-security-core/blob/2.x/src/java/grails/plugin/springsecurity/authentication/GrailsAnonymousAuthenticationToken.java
     *      https://github.com/grails-plugins/grails-spring-security-core/blob/2.x/grails-app/conf/DefaultSecurityConfig.groovy#L145
     */
    private static Authentication createAnonymousAuthToken() {
        String key ='foo' // same as grailsApplication.config.grails.plugin.springsecurity.anon.key
        new GrailsAnonymousAuthenticationToken(key, null)
    }

    private Revision doBeforePublishingCuratedRevision(Revision revision) throws ModelException {
        String publicationId
        if (null == revision.model.publicationId) {
            synchronized (this) {
                revision.model.publicationId = getPublicationIdGenerator().generate()
            }
        }
        publicationId = revision.model.publicationId

        // TODO move out of here and invoke via e.g. grailsApplication.mainContext.publishEvent()
        String format = revision.format.identifier
        if ("SBML" == format) {
            RevisionTC revisionTC = new RevisionAdapter(revision: revision).toCommandObject()
            // TODO externalise generation of canonical model URIs?
            String[] idXRefs = [revision.model.submissionId, publicationId].collect { String id ->
                "https://identifiers.org/biomodels.db:$id".toString()
            } as String[]
            boolean revisionUpdated = addModelIdentifiersAsAnnotation(revisionTC, idXRefs)

            if (!revisionUpdated) {
                return revision // nothing else to do
            }
            revisionTC.minorRevision = true
            revisionTC.comment = "Automatically added model identifier $publicationId"

            Revision toPublish = doPersistRevision(revisionTC.files, [], revisionTC)
            RevisionTC toPublishTC = new RevisionAdapter(revision: toPublish).toCommandObject()
            indexModelRevision(toPublishTC)
            shareRevision2FellowCurators(toPublishTC)
            return toPublish
        } else {
            logger.warn("""We are publishing $revision encoded in $format, but won't be able to add \
the perennial publication identifier to the model file.""")
            RevisionTC toPublishTC = new RevisionAdapter(revision: revision).toCommandObject()
            indexModelRevision(toPublishTC)
        }

        return revision
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
    @PostLogging(LoggingEventType.UNPUBLISH)
    @Profiled(tag="modelService.unpublishModelRevision")
    void unpublishModelRevision(Revision revision) {
        if (!revision) {
            throw new IllegalArgumentException("Revision may not be null")
        }
        if (revision.deleted) {
            throw new IllegalArgumentException("Revision may not be deleted")
        }
        aclUtilService.deletePermission(revision, "ROLE_USER", BasePermission.READ)
        aclUtilService.deletePermission(revision, "ROLE_ANONYMOUS", BasePermission.READ)
        revision.state = ModelState.UNPUBLISHED
        // preserve two following properties for history and tracking because the perennial id was already issued
        // also, the first published date is also retained to know which date was the model published first
        // revision.model.firstPublished = null
        // revision.model.publicationId = null
        if (!revision.save(flush: true)) {
            logger.error("Revision ${revision.id} cannot be turned private due to: ${revision.errors.allErrors.toString()}")
        }
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
    void submitModelRevisionForPublication(Revision revision) {
        if (!revision) {
            throw new IllegalArgumentException("Revision may not be null")
        }
        if (revision.deleted) {
            throw new IllegalArgumentException("Revision may not be deleted")
        }
        Model model = revision.model
        // grant permissions to ROLE_CURATOR
        aclUtilService.addPermission(model, "ROLE_CURATOR", BasePermission.READ)
        aclUtilService.addPermission(model, "ROLE_CURATOR", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(model, "ROLE_CURATOR", BasePermission.WRITE)

        model.revisions.each { Revision rev ->
            aclUtilService.addPermission(rev, "ROLE_CURATOR", BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(rev, "ROLE_CURATOR", BasePermission.READ)
        }
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
            if (!audit.save(flush: true)) {
                logger.error("""\
While trying to update an audit record, there is an error: ${audit.getErrors().allErrors.inspect()}""")
                return -1
            } else {
                return audit.id
            }
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
                    logger.error("""\
Failed to update audit $itemId to $success: ${audit.errors.allErrors.inspect()}""")
                }
            }
        }
    }

    /**
     * Retrieves the main file of the models given in a list of their identifiers
     *
     * This service is currently used to fetch the requested main files to
     * @{ModelDelegateService.serveModelFilesAsZip(modelIds)} which is being participated to
     * download a bulk of the model main files chosen from the search results.
     * The search result always contains downloadable public models, therefore,
     * these chain of methods do not need to check ACLs
     *
     * @param modelIDs The list of model identities being retrieved
     * @return either the list of RFTC objects or null
     *         if there is no model files available
     */
    Map<String, RFTC> fetchMainFileForModels(String[] modelIDs) {
        Map<String, RFTC> results = [:]
        List mids = modelIDs.toList()
        String query = """
SELECT
    r1
FROM
Revision AS r1
JOIN r1.model AS model
WHERE
    r1.revisionNumber = (select max(r2.revisionNumber) from Revision AS r2
                        where r2.model = model and r2.state = '${ModelState.PUBLISHED}')
    AND (model.submissionId IN (:mids) OR model.publicationId IN (:mids))
    AND r1.id IN (
        SELECT aoi.objectId
        FROM
            AclEntry AS ace
            JOIN ace.aclObjectIdentity AS aoi
            JOIN aoi.aclClass AS aclClass
            JOIN ace.sid AS sid
        WHERE
            aclClass.className = 'net.biomodels.jummp.model.Revision'
            AND sid.sid = 'ROLE_ANONYMOUS'
            AND ace.mask = 1)"""
        List revisions = Revision.executeQuery(query, [mids: mids])

        revisions.each { Revision revision ->
            List<File> files = retrieveFiles(revision)
            RepositoryFile rf = revision.repoFiles.find { RepositoryFile rf -> rf.mainFile }
            File f = files.find { it.name == rf.path }
            if (!f) {
                logger.error "Cannot find main file for revision {}", revision.id
                return
            }
            String filename = revision.model.publicationId ?: revision.model.submissionId
            RFTC rftc = new RFTC(
                id: rf.id,
                path: f.getCanonicalPath(),
                description: rf.description,
                hidden: rf.hidden,
                mainFile: rf.mainFile,
                userSubmitted: rf.userSubmitted,
                mimeType: rf.mimeType)
            results.put(filename, rftc)
        }

	    return results
    }

    private void addContributors(final Revision revision, final Map working) {
        final String username = revision.owner.username
        String roleName = working["contributorRole"] as String
        List CONTRIBUTOR_ROLES = CR.all.collect { it.name }
        if (!CONTRIBUTOR_ROLES.contains(roleName)) {
            logger.debug("""Cannot find the right contributor role for the role name `${roleName}` provided \
during the submission or update process. Hence, we have added `$username` as a modeller by default.""")
            roleName = "Modeller"
        }
        String msg
        aclInsertionLock.lock()
        try {
            boolean isUpdate = working["isUpdate"] as boolean
            if (isUpdate) {
                List<CD> cds = CD.findAllByContributorAndRevision(revision.owner, revision, [locked: true])
                cds.each { CD cd ->
                    CD.where {
                        revision == cd.revision && contributor == cd.contributor
                    }.deleteAll()
                }
            }
            CD.withTransaction {
                contributorService.findOrSaveContributionDetails(revision, roleName)
            }
        } catch (Exception e) {
            msg = "Failed to add $username as a(n) ${roleName} for the revision ${revision.id} due to ${e.toString()}."
            logger.error(msg)
        } finally {
            aclInsertionLock.unlock()
        }
    }

    /**
     * Invoking updateIndex method of search service
     *
     * @param   cmd The representation of revision whereby search service will be updated its indexes
     * @return
     */
    private indexModelRevision(RevisionTC cmd) {
        // can't inject searchService -- cyclic dependency
        def searchService = grailsApplication.mainContext.searchService
        searchService.updateIndex(cmd)
    }

    private convertModelToOtherFormats(RevisionTC cmd) {
        logger.info("""\
Try to connect with Conversion service to export the model ${cmd.model.submissionId} under the other formats""")
        modelConversionService.generateExports(cmd)
    }

    /**
     * Checking the curation status of a given revision
     *
     * @param   a revision  A given revision for checking its curation status
     * @return  a boolean   true if the revision was marked CURATED
     */
    private boolean isCurated(Revision revision) {
        revision.curationState == CurationState.CURATED
    }

    /**
     * Adds a {@link ModellingApproach} considered as an annotation to a specific revision.
     * @param revisionTC    a {@link RevisionTC} object representing the revision.
     * @param approach      a {@link ModellingApproach} object representing the modelling approach.
     * @throws ModelException
     */
    void addModellingApproachAsAnnotation(RevisionTC revisionTC, ModellingApproach approach) throws
            ModelException {
        def sbmlService = grailsApplication.mainContext.getBean("sbmlService", ISbmlService.class)
        boolean result = sbmlService.addModellingApproachAsAnnotation(revisionTC, approach)
        if (!result) {
            logger.error("""\
There has been error while adding $approach to the model ${revisionTC.identifier()}""")
        }
    }

    boolean addModelIdentifiersAsAnnotation(RevisionTC revisionTC, String... xRefs = null) {
        def sbmlService = grailsApplication.mainContext.getBean("sbmlService", ISbmlService.class)
        if (!xRefs) {
            xRefs = ["https://identifiers.org/biomodels.db:${revisionTC.model.submissionId}"] as String[]
        }
        sbmlService.addModelIdentifiersAsAnnotation(revisionTC, xRefs)
    }

    /**
     * Adds a publication identifier {@link Publication} as an annotation to the SBML file of the given model revision
     * @param revisionTC   A {@link RevisionTC} instance indicating the model revision
     * @param pubTC A {@link PublicationTransportCommand} instance indicating the publication details
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void addPublicationAsAnnotation(RevisionTC revisionTC, PublicationTransportCommand pubTC) throws
        ModelException {
        if (pubTC) {
            def sbmlService = grailsApplication.mainContext.getBean("sbmlService", ISbmlService.class)
            boolean result = sbmlService.addPublicationAsAnnotation(revisionTC, pubTC)
            if (!result) {
                logger.error("""\
There has been error while adding ${pubTC.link} (${pubTC.linkProvider.linkType}) to the model main file of \
the ${revisionTC.identifier()}. Otherwise, BioModels only supports to add a PubMed or DOI publication as \
an annotation to SBML document.""")
            }
        }
    }

    /**
     * Publishes an event to a specific subscriber. The method checks criteria to publish a
     * {@link RevisionCreatedEvent} so that the subscriber
     * {@link net.biomodels.jummp.core.subscribers.ShareRevisionToFellowCurators}
     * can detect and share the newly created revision to the fellow curators with the writable permission.
     *
     * @param command {@link RevisionTC} object
     */
    private void shareRevision2FellowCurators(RevisionTC command) {
        StopWatch stopWatch = new Log4JStopWatch("modelService.shareRevision2FellowCurators")
        Revision revision = Revision.get(command.id)
        // Check authorities
        User owner = revision.owner
        Set roles = owner.authorities
        Role curaRole = Role.findByAuthority("ROLE_CURATOR")
        boolean shouldShare = curaRole in roles
        if (shouldShare) {
            logger.debug("Publishing the event to share the revision ${command.identifier()}")
            grailsApplication.mainContext.publishEvent(new RevisionCreatedEvent(this, command))
        } else {
            logger.error("cannot share the model ${command.identifier()} to fellow curators")
        }
        stopWatch.stop()
    }

    private Revision doPostPersistRevision(final Revision revision, final Map working = null) {
        StopWatch stopWatch = new Log4JStopWatch("modelService.doPostPersistRevision")
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

            if (!attachedRevision) {
                logger.debug("${revision.model.submissionId}.${revision.id} cannot be fetched from the database. Please run all post processes for this revision manually.")
                return null
            }
            def revisionAdapter = new RevisionAdapter(revision: attachedRevision, latest: true)
            RevisionTC cmd = revisionAdapter.toCommandObject()
            try {
                // TODO: catch exceptions of each post-submission processes to report to the submitter and BioModels cura
                addContributors(attachedRevision, working)
                indexModelRevision(cmd)
                //convertModelToOtherFormats(cmd)
                shareRevision2FellowCurators(cmd)
                updateModelCache(attachedRevision)
            } catch (Exception e) {
                e.printStackTrace()
                logger.error(e.toString())
                println(e.toString())
            }
            return attachedRevision
        }
        stopWatch.stop()
        return null
    }

    private void doUpdatePermissions(final Model model, final Revision revision, final User user) {
        StopWatch stopWatch = new Log4JStopWatch("modelService.doUpdatePermissions")
        aclInsertionLock.lock()
        try {
            aclUtilService.addPermission(revision, user.username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(revision, user.username, BasePermission.READ)
            aclUtilService.addPermission(revision, user.username, BasePermission.DELETE)

            // grant admin rights to the owner of the model
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
        } catch (Exception e) {
            logger.error(e.getMessage()) // TODO: here?
        } finally {
            aclInsertionLock.unlock()
        }
        stopWatch.stop()
    }

    private void validateModelRevision(final RevisionTC rev) throws ModelException {
        StopWatch stopWatch = new Log4JStopWatch("modelService.validateModelRevision")
        if (!rev.model) {
            throw new ModelException(null, "Model may not be null")
        }
        if (rev.model.deleted) {
            throw new ModelException(rev.model, "A new Revision cannot be added to a deleted model")
        }
        if (rev.comment == null) {
            throw new ModelException(rev.model, "Comment may not be null, empty comment is allowed")
        }
        stopWatch.stop()
    }

    private Model doUpdateModelMetadata(final Model model, final RevisionTC revision) {
        StopWatch stopWatch = new Log4JStopWatch("modelService.doUpdateModelMetadata")
        model.modellingApproach = revision.model.modellingApproach
        model.otherInfo = revision.model.otherInfo
        model.isMetadataSubmission = revision.model.isMetadataSubmission
        PublicationTransportCommand publicationTC = revision.model.publication
        if (!publicationTC && model.publication) {
            // delete db association if corresponding publication was removed in the UI
            model.publication = null
        } else if (publicationTC) {
            // update db association with the value from the UI
            try {
                model.publication = publicationService.fromCommandObject(publicationTC)
            } catch(Exception e) {
                logger.error("Unable to record publication for ${revision.model}: ${e.message}", e)
            }
        }
        stopWatch.stop()

        model
    }

    private Revision putFilesUnderVcs(final Model model, final Revision revision,
                                      final List domainObjects, final List modelFiles,
                                      final List filesToDelete, final boolean isAmend) {
        StopWatch stopWatch = new Log4JStopWatch("modelService.putFilesUnderVcs")
        try {
            String vcsId = vcsService.updateModel(model, modelFiles, filesToDelete, revision.comment, isAmend)
            revision.vcsId = vcsId
        } catch (VcsException e) {
            revision.discard()
            domainObjects.each { it.discard() }
            logger.error("Exception occurred during uploading a new Model Revision to VCS: ${e.getMessage()}")
            stopWatch.stop()
            throw new ModelException(new ModelAdapter(model: model, latest: revision).toCommandObject(),
                "Could not store new Model Revision for Model ${model.id} with VcsIdentifier ${model.vcsIdentifier} in VCS", e)
        }
        domainObjects.each {
            revision.addToRepoFiles(it)
        }
        stopWatch.stop()

        revision
    }

    private void discardFailedRevision(final Model model, final Revision revision, final List repoFiles) {
        // TODO: this means we have imported the revision into the VCS, but it failed to be saved in the database, which is pretty bad
        StopWatch stopWatch = new Log4JStopWatch("modelService.discardFailedRevision")
        revision.errors.allErrors.each {
            logger.error(it.toString())
        }
        revision.discard()
        final def m = new ModelAdapter(model: model, latest: revision).toCommandObject()
        logger.error("""New Revision containing ${repoFiles.inspect()} for Model ${m} with VcsIdentifier \
${model.vcsIdentifier} added to VCS, but not stored in database""")
        stopWatch.stop()
        throw new ModelException(m, "Revision stored in VCS, but not in database")
    }

    private Revision doUpdateRevision(Revision revision, RevisionTC rev) {
        StopWatch stopWatch = new Log4JStopWatch("modelService.doUpdateRevision")
        final User currentUser = User.findByUsername(springSecurityService.authentication.name)
        final String formatVersion = rev.format.formatVersion ?: modelFileFormatService.getFormatVersion(rev)
        revision.with {
            name = rev.name
            description = rev.description
            comment = rev.comment
            uploadDate = new Date()
            owner = currentUser
            validated = rev.validated
            curationState = rev.curationState
            minorRevision = rev.minorRevision
            format = ModelFormat.findByIdentifierAndFormatVersion(rev.format.identifier, formatVersion)
            validationReport = rev.validationReport
            validationLevel = rev.validationLevel
            readmeSubmission = rev.readmeSubmission
            repoFiles = null
        }
        // delete the previous repository files associating with the revision
        RepositoryFile.where { revision == revision }.deleteAll()
        stopWatch.stop()

        revision
    }

    void updateModelCache(final Revision revision) {
        repositoryFileService.updateModelRevisionCache(revision)
        // do not copy files in the local development and with a private model
        if (grailsApplication.isWarDeployed() && revision.state == ModelState.PUBLISHED) {
            copyRevisionFilesToFtp(revision)
        }
    }

    void copyRevisionFilesToFtp(final Revision revision) {
        String message = repositoryFileService.copyRevisionFilesToFtp(revision)
        logger.debug("sent a request to HPC to copy the revision files to FTP: $message")
    }

    /**
     * Gets all identifiers of the public models from the individual model domain
     * @return a list of string objects as the model identifiers
     */
    List<String> getAllModelIdentifiers() {
        List<String> identifiers
        String strIdentifiers = redisService.doRedisGet(Redis.REDIS_KEY_ALL_MODEL_IDS)
        if (strIdentifiers) {
            logger.debug("Retrieving all model identifiers from Redis cache.")
            identifiers = strIdentifiers.split(",").toList()
        } else {
            logger.debug("Extracting all model identifiers from EBI Search and caching them on Redis cache.")
            identifiers = extractAndCacheModelIdentifiers()

        }
        identifiers
    }

    /**
     * Extracts all model identifiers by fetching all models from EBI Search server.
     * Then updates the cached identifiers or caches them on Redis Server if they do not exist.
     * @return a list of string objects as the model identifiers
     */
    List<String> extractAndCacheModelIdentifiers(boolean updateCache = true) {
        List<String> identifiers
        final String query = "query=*:*&isprivate:false&fields=id,name,isprivate&format=json"
        String BM = "${BioModels.EBI_PROD_WS_REST_BM_URL}?$query"
        identifiers = extractIdentifiersBasedEbiSearchJson(BM)

        if (updateCache && !identifiers.isEmpty()) {
            String strIdentifiers = identifiers.join(",")
            logger.info("Updating the cached model identifiers on Redis Server")
            redisService.doRedisSet(Redis.REDIS_KEY_ALL_MODEL_IDS, strIdentifiers)
        }

        identifiers
    }

    /**
     * Gets all identifiers of the public models from the autogenerated model domain
     * @return a list of string objects as the model identifiers
     */
    List<String> getAutogeneratedModelIdentifiers() {
        final String BM_AUTO = "https://www.ebi.ac.uk/ebisearch/ws/rest/biomodels_autogen"
        String query = "query=*:*&isprivate:false&fields=id,name,isprivate&format=json"
        query = "${BM_AUTO}?$query"
        return extractIdentifiersBasedEbiSearchJson(query)
    }

    private static List<String> extractIdentifiersBasedEbiSearchJson(final String query) {
        List<String> identifiers = new ArrayList<>()
        String jsonString = JummpHttpService.jsonGetRequest(query)
        JSONObject json = new JSONObject(jsonString)
        Integer nbModels = 0
        if (json.has("hitCount")) {
            String nbMatches = json.get("hitCount")
            nbModels = nbMatches.toInteger()
        }
        if (nbModels > 0) {
            Integer fetchSize = 100
            String searchAllUrl = "$query&size=$fetchSize"
            Integer times = (Integer) Math.ceil((double) nbModels / fetchSize)
            Long offset = 0
            for (int index = 1; index <= times; ++index) {
                String searchUrl = "$searchAllUrl&start=$offset"
                jsonString = JummpHttpService.jsonGetRequest(searchUrl)
                json = new JSONObject(jsonString)
                List ids = extractAllModelIdentifiers(json)
                identifiers.addAll(ids)
                offset = index * fetchSize
            }
        }
        if (!identifiers.isEmpty()) {
            identifiers.sort()
        }

        identifiers
    }

    private static List<String> extractAllModelIdentifiers(JSONObject json) {
        List<String> results = []
        if (json.has("entries")) {
            json.get("entries").each {
                results.push(it.get("id") as String)
            }
        }
        results
    }

    @Override
    void onApplicationEvent(ModelOperationEvent event) {
        if (event instanceof ModelPublishedEvent) {
            // Both publish and unpublish actions are using the same event {@link ModelPublishedEvent}
            // Therefore, we define a source of "username;published|unpublished" when firing up a ModelPublishedEvent
            // event when a model is published or unpublished in ModelController.
            String[] parts = event.source.split(";")
            String whoDid = parts[0]
            String whichService = parts[1]
            ModelTC model = event.model
            final String msg = "${whoDid} has ${whichService} the model revision ${model.submissionId}: ${model.name}."
            println(msg)
            logger.info(msg)
            // email to or notify biomodels-developers@ebi.ac.uk

        } else if (event instanceof ModelDeletedEvent) {
            logger.info(event.source.dump() + "\t" + event.model.dump())
        }
    }
}
