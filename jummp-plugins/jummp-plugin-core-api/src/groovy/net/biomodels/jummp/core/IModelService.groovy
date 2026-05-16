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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Spring Security (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Spring Security used as well as
 * that of the covered work.}
 **/


package net.biomodels.jummp.core

import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.core.model.ModelTransportCommand as ModelTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import org.springframework.security.access.AccessDeniedException as ADEx

/**
 * @short Service Interface for accessing the Model Service from a Remote Adapter.
 *
 * The Service defined by this interface can be injected into any Remote Adapter such as
 * JMS to communicate with JUMMP core.
 *
 * All methods may throw AccessDeniedExceptions as JUMMP core is ACL protected. This interface
 * defines methods only with the TransportCommand wrappers - direct access to the underlying
 * data objects is not possible.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
interface IModelService {
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
     * @return List of Models
     */
    List<ModelTC> getAllModels(int offset, int count, boolean sortOrder, ModelListSorting sortColumn)

    /**
     * Convenient method for sorting by the id column.
     *
     * @return List of Models sorted ascending
     * @see getAllModels(intoffset,intcount,booleansortOrder)
     */
    List<ModelTC> getAllModels(int offset, int count, boolean sortOrder)

    /**
     * Convenient method for ascending sorting.
     *
     * @return List of Models sorted ascending by @p sortColumn
     * @see getAllModels(intoffset,intcount,booleansortOrder)
     */
    List<ModelTC> getAllModels(int offset, int count, ModelListSorting sortColumn)

    /**
     * Convenient method for ascending sorting by id.
     *
     * @return List of Models sorted ascending by id
     * @see getAllModels(intoffset,intcount,booleansortOrder)
     */
    List<ModelTC> getAllModels(int offset, int count)

    /**
     * Convenient method for ascending sorting of first ten models.
     *
     * @param sortColumn the column which should be sorted
     * @return List of first 10 Models sorted ascending by @p sortColumn
     * @see getAllModels(intoffset,intcount,booleansortOrder)
     */
    List<ModelTC> getAllModels(ModelListSorting sortColumn)

    /**
     * Convenient method for ascending sorting of first ten models by id.
     *
     * @return List of first 10 Models sorted ascending by id
     * @see getAllModels(intoffset,intcount,booleansortOrder)
     */
    List<ModelTC> getAllModels()

    /**
     * Returns the number of Models the user has access to.
     *
     * @see getAllModels
     */
    Integer getModelCount()

    /**
     * Returns the Model identified by perennial identifier @p modelId
     * @param modelId The Model to be returned
     * @return The Model if available
     */
    ModelTC getModel(String modelId)

    /**
     * Returns the first and last revision of the given model without checking
     * ACLs as well as permissions. This method will be called by admins or
     * any privileges. In other words, admins use this method to manage data.
     * @param model {@link Model} instance
     * @return a pair of two {@link net.biomodels.jummp.model.Revision} instances
     */
    Revision[] getFirstAndLastRevision(Model model)

    /**
     * Queries the model for the latest available revision the user has read access to.
     * @param modelId The id of the Model for which the latest revision should be retrieved.
     * @return Latest Revision the current user has read access to. If there is no such revision null is returned
     */
    RevisionTC getLatestRevision(String modelId)

    /**
     * Queries the oldest/first revision of a model. Because of the deletion, the first revision couldn't be the one
     * which revision number is greater than 1. Thus, the first or oldest revision will be the one which revision
     * number is the smallest.
     *
     * @param modelId indicating the model
     * @return the revision transport command
     */
    RevisionTC getOldestRevision(String modelId)

    /**
     * @see getOldestRevision(StringmodelId)
     *
     * @param revisionTC
     * @return
     */
    RevisionTC getOldestRevision(RevisionTC revisionTC)

    /**
     * Queries the model for all revisions the user has read access to.
     * The returned list is ordered by revision number of the model.
     * @param modelId The id of the Model for which all revisions should be retrieved
     * @return List of Revisions ordered by revision numbers of underlying VCS. If the user has no access to any
     * revision an empty list is returned
     * @todo: add paginated version with offset and count. Problem: filter
     */
    List<RevisionTC> getAllRevisions(String modelId)

    /**
     * Retrieves the Revision for the Model identified by @p modelId and @p revisionNumber
     * @param modelId The Id of the model
     * @param revisionNumber The revision in context of the Model
     * @return The Revision or @c null if there is no such Revision
     */
    RevisionTC getRevision(String modelId, int revisionNumber)

    /**
     * Returns the reference publication of this model.
     * @param modelId The if of the Model for which the reference publication should be returned.
     * @return The reference publication
     * @throws IllegalArgumentException if @p model is null
     * @throws ADEx if the current user is not allowed to access at least one Model Revision
     */
    PubTC getPublication(final String modelId) throws ADEx, IllegalArgumentException

    /**
     * Creates a new Model and stores it in the VCS.
     *
     * Stores the @p modelFile as a new file in the VCS and creates a Model for it.
     * The Model will have one Revision attached to it. The MetaInformation for this
     * Model is taken from @p meta. The user who uploads the Model becomes the owner of
     * this Model. The new Model is not visible to anyone except the owner.
     * @param modelFile The model file to be stored in the VCS.
     * @param meta Meta Information to be added to the model
     * @return The new created Model, or null if the model could not be created
     * @throws ModelException If Model File is not valid or the Model could not be stored in VCS
     */
    ModelTC uploadModel(final List<File> modelFiles, ModelTC meta) throws ModelException

    /**
     * Adds a new Revision to the model.
     *
     * The provided @p file will be stored in the VCS as an update to an existing file of the same model.
     * A new Revision will be created and appended to the list of Revisions of the @p model.
     * @param modelId The id of the Model the revision should be added
     * @param file The model file to be stored in the VCS as a new revision
     * @param format The format of the model file
     * @param comment The commit message for the new revision
     * @return The new added Revision. In case an error occurred while accessing the VCS @c null will be returned.
     * @throws ModelException If either @p model, @p file or @p comment are null or if
     * the file does not exists or is a directory
     */
    RevisionTC addRevision(final String modelId,
                           final File file,
                           final MFTC format,
                           final String comment) throws ModelException

    /**
     * @short Adds a new Revision to the model, to be used by SubmissionService
     *
     * The provided @p modelFiles will be stored in the VCS as an update to the existing files of the same @p model.
     * A new Revision will be created and appended to the list of Revisions of the @p model.
     * The revision will not be validated, as the checks are assumed to have been already conducted. The revision
     * will be also indexed if it is successfully added to the model.
     *
     * @param model The Model the revision should be added
     * @param modelFiles The model files to be stored in the VCS as a new revision
     * @param format The format of the model files
     * @param comment The commit message for the new revision
     *
     * @return The newly-added Revision. In case an error occurred while accessing the VCS @c null will be returned.
     * @throws ModelException If either @p model, @p modelFiles or @p comment are null or if the files do not exist
     * or are directories.
     */
    RevisionTC addRevision(final List<RFTC> repoFiles,
                           final List<RFTC> deleteFiles,
                           final RevisionTC rev) throws ModelException

    /**
     * Returns whether the current user has the right to add a revision to the model.
     * @param modelId The id of the model to check
     * @return @c true if the user has write permission on the revision or is an admin user, @c false otherwise.
     */
    Boolean canAddRevision(final String modelId)

    /**
     * Retrieves the model file for the @p revision.
     * @param revision The Model Revision for which the file should be retrieved.
     * @return Byte Array of the content of the Model file for the revision.
     * @throws ModelException In case retrieving from VCS fails.
     */
    List<RFTC> retrieveModelFiles(final RevisionTC revision) throws ModelException

    /**
     * Retrieves the model file for the latest revision of the model.
     * @param modelId The id of the Model for which the file should be retrieved
     * @return Byte Array of the content of the Model file.
     * @throws ModelException In case retrieving from VCS fails.
     */
    List<RFTC> retrieveModelFiles(final String modelId) throws ModelException

    /**
     * Grants read access for model to @p collaborator.
     *
     * The @p collaborator receives the right to read all future revisions of the model
     * as well as read access to all revisions the current user has read access to.
     * The current user can only grant read access in case he has read access on the model
     * himself and the right to grant read access.
     *
     * @param modelId The id of the Model for which read access should be granted
     * @param collaborator The user who should receive read access
     * @todo Might be better in a CollaborationService?
     */

    void grantReadAccess(String modelId, User collaborator)
    /**
     * Grants write access for model to @p collaborator.
     *
     * The @p collaborator receives the right to add new revisions to the model.
     * The current user can only grant write access in case he has write access on the model
     * himself and the right to grant write access.
     *
     * @param modelId The id of the Model for which write access should be granted
     * @param collaborator The user who should receive write access
     * @todo Might be better in a CollaborationService?
     */

    void grantWriteAccess(String modelId, User collaborator)
    /**
     * Revokes read access for model from @p collaborator.
     *
     * The @p collaborator gets the right to read future revisions to the model revoked.
     * Read access to existing revisions is not revoked.
     * Write access to the model (that is uploading new revisions) is also revoked.
     * The current user can only revoke the right if he has the right to read future revisions
     * himself and has the right to grant/revoke read rights on the model. The right is not revoked
     * if the user is an administrator of the model.
     * @param modelId The id of the Model for which read access should be revoked
     * @param collaborator The User whose read access should be revoked
     * @return @c true if the right has been revoked, @c false otherwise
     * @todo Might be better in a CollaborationService?
     */

    boolean revokeReadAccess(String modelId, User collaborator)
    /**
     * Revokes write access for model from @p collaborator.
     *
     * The @p collaborator gets the right to add revisions to the model revoked.
     * The current user can only revoke the right if he has the right to add revisions
     * himself and has the right to grant/revoke write rights on the model
     * @param modelId The id of the Model for which write access should be revoked
     * @param collaborator The User whose write access should be revoked
     * @return @c true if the right has been revoked, @c false otherwise
     * @todo Might be better in a CollaborationService?
     */
    boolean revokeWriteAccess(String modelId, User collaborator)

    /**
     * Transfers the ownership of the model to @p collaborator.
     *
     * The ownership can only be transferred from a user having the right to grant
     * read/write access and the @p model is not yet under curation or published.
     * The @p collaborator has to have read access to future revisions of the model.
     *
     * All Model specific rights are revoked from the owner and granted to the @p collaborator.
     * This includes:
     * @li Write access to the model
     * @li Read access to future revisions of the @p model
     * @li Start of curation
     * @li Grant/Revoke read/write access to the @p model
     * @param model The id of the Model for which the ownership should be transferred.
     * @param collaborator The User who becomes the new owner
     * @todo Might be better in a CollaborationService?
     */
    void transferOwnerShip(String modelId, User collaborator)

    /**
     * Deletes the model including all Revisions.
     *
     * Flags the model and all its revisions as deleted. A deletion from VCS is for
     * technical reasons not possible and because of that a deletion of the Model object
     * is not possible.
     *
     * Deletion of model is only possible if the model is neither under curation nor published.
     * @param modelId The id of the Model to be deleted
     * @return @c true in case the Model has been deleted, @c false otherwise.
     * @see restoreModel
     */
    boolean deleteModel(String modelId)

    /**
     * Restores the deleted model.
     *
     * Removes the deleted flag from the model and all its Revisions.
     * @param modelId The id of the deleted Model to restore
     * @return @c true, whether the state was restored, @c false otherwise.
     * @see deleteModel* @todo might belong in an administration service?
     */
    boolean restoreModel(String modelId)

    boolean deleteRevision(RevisionTC revision)

    RevisionTC publishModelRevision(RevisionTC revision)

//    public void validateModelRevision(RevisionTC revision)

    /**
     * Finds the model with the specified perennial identifier.
     *
     * @p identifier the perennial identifier against which to perform the search.
     * @return @c null if there was no match, @c the ModelTC of the corresponding
     * model otherwise.
     */
    ModelTC findByPerennialIdentifier(String identifier)

    /**
     * Maps a list of perennial identifiers to their corresponding primary key.
     * @param identifiers a List of strings corresponding to perennial model identifiers
     * @return a map with keys containing the primary key for a model and the value being the given
     *         perennial identifier
     */
    Map<Long, String> findModelsByPerennialId(List<String> identifiers)

    /**
     * Finds the Revision corresponding to the supplied identifier arguments.
     *
     * @param model a perennial model identifier which may include the revision identifier or not.
     * @param revision the specific revision number of the model in question. If this argument is null,
     * the latest revision of the model should be retrieved.
     * @return a RevisionTC representation of the requested model revision.
     */
    RevisionTC getRevisionFromParams(final String model, final String revision)

    /**
     * @short Specifies whether there are one or more kinds of perennial identifiers defined.
     *
     * In effect, this method returns false if the model identifier settings only cover
     * submission, and true if there are multiple identifier schemes defined - e.g. submission,
     * publication etc.
     */

    String getVcsIdentifier(final String perennialIdentifier)

    Map getRevisionsState(final String modelId)

    Map getRevisionsState(final Model model)

    boolean haveMultiplePerennialIdentifierTypes()

    /**
     * @short Creates a model audit object to update the history of activities on a specific model given by its identifier
     * @param modelId a String denoting the model identifier
     * @param user a String denoting the username
     * @param accessType a String denoting the type of accession, {@see AccessType}
     * @param formatType a String denoting the type of format. It is defined from ["json", "html"]
     * @param changesMade a String referring to comments what have been changed
     * @param success true/false
     * @return an integer value indicating the id of ModelAudit record created successfully
     */
    int updateHistory(String modelId, String user, String accessType,
                      String formatType, String changesMade, boolean success)

    int updateHistory(ModelTC model, String user, String accessType,
                      String formatType, String changesMade, boolean success)
}
