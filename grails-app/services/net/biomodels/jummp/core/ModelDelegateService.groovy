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

import eu.ddmore.publish.service.PublishContext
import net.biomodels.jummp.core.adapters.DomainAdapter
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.model.ModelAuditTransportCommand
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PermissionTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.model.ValidationState
import net.biomodels.jummp.core.vcs.VcsFileDetails
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.core.model.identifier.generator.AbstractModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.qcinfo.QcInfo
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.security.access.AccessDeniedException
/**
 * @short Service delegating methods to ModelService.
 *
 * This service implements the IModelService interface and gets injected
 * into the remote adapters. The main purpose of this service is to translate
 * the CommandObjects to their respective DOM classes and vice versa. This
 * service should not be used internally in the core. In the core the
 * ModelService should be used directly.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Sarala Wimalaratne <sarala@ebi.ac.uk>
 */
class ModelDelegateService implements IModelService {
    static transactional = false
    private static final Log log = LogFactory.getLog(this)

    def modelService
    def modelFileFormatService
    def qcInfoDelegateService
    def referenceTracker
    def publicationIdGenerator

    String getPluginForFormat(ModelFormatTransportCommand format) {
        return modelFileFormatService.getPluginForFormat(format)
    }

    List<ModelTransportCommand> getAllModels(int offset, int count, boolean sortOrder, ModelListSorting sortColumn) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(offset, count, sortOrder, sortColumn).each {
            models << DomainAdapter.getAdapter(it).toCommandObject()
        }
        return models
    }

    List<ModelTransportCommand> getAllModels(int offset, int count, boolean sortOrder) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(offset, count, sortOrder).each {
            models << DomainAdapter.getAdapter(it).toCommandObject()
        }
        return models
    }

    List<ModelTransportCommand> getAllModels(int offset, int count, ModelListSorting sortColumn) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(offset, count, sortColumn).each {
            models << DomainAdapter.getAdapter(it).toCommandObject()
        }
        return models
    }

    List<ModelTransportCommand> getAllModels(int offset, int count) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(offset, count).each {
            models << DomainAdapter.getAdapter(it).toCommandObject()
        }
        return models
    }

    List<ModelTransportCommand> getAllModels(ModelListSorting sortColumn) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(sortColumn).each {
            models << DomainAdapter.getAdapter(it).toCommandObject()
        }
        return models
    }

    List<ModelTransportCommand> getAllModels() {
        List<ModelTransportCommand> models = []
        modelService.getAllModels().each {
            models << DomainAdapter.getAdapter(it).toCommandObject()
        }
        return models
    }

    Integer getModelCount() {
        return modelService.getModelCount()
    }

    long createAuditItem(ModelAuditTransportCommand cmd) {
        return modelService.createAuditItem(cmd)
    }

    void updateAuditSuccess(Long itemId, boolean success) {
        modelService.updateAuditSuccess(itemId, success)
    }

    List<VcsFileDetails> getFileDetails(long revID, String filename) {
        return modelService.getFileDetails(Revision.get(revID), filename)
    }

    ModelTransportCommand getModel(String modelId) {
        return DomainAdapter.getAdapter(modelService.getModel(modelId)).toCommandObject()
    }

    RevisionTransportCommand getLatestRevision(String modelId, boolean addToHistory = true) {
        Model model = ModelAdapter.findByPerennialIdentifier(modelId)
        if (!model) {
            throw new AccessDeniedException("No access to any revision of Model ${modelId}")
        }
        Revision rev = modelService.getLatestRevision(model, addToHistory)
        if (rev) {
            return DomainAdapter.getAdapter(rev).toCommandObject()
        } else {
            throw new AccessDeniedException("No access to any revision of Model ${modelId}")
        }
    }

    List<RevisionTransportCommand> getAllRevisions(String modelId) {
        List<RevisionTransportCommand> revisions = []
        modelService.getAllRevisions(ModelAdapter.findByPerennialIdentifier(modelId)).each {
            revisions << DomainAdapter.getAdapter(it).toCommandObject()
        }
        return revisions
    }

    RevisionTransportCommand getRevision(String identifier) {
        return DomainAdapter.getAdapter(modelService.getRevision(identifier)).toCommandObject()
    }

    RevisionTransportCommand getRevision(String modelId, int revisionNumber) {
        return DomainAdapter.getAdapter(modelService.getRevision(
                    ModelAdapter.findByPerennialIdentifier(modelId), revisionNumber)).toCommandObject()
    }

    PublicationTransportCommand getPublication(String modelId) throws AccessDeniedException,
                IllegalArgumentException {
        def publication = modelService.getPublication(
                               ModelAdapter.findByPerennialIdentifier(modelId))
        if (publication) {
            return DomainAdapter.getAdapter(publication).toCommandObject()
        }
        return null
    }

    ModelTransportCommand uploadModel(List<File> modelFiles, ModelTransportCommand meta) throws
                ModelException {
        return DomainAdapter.getAdapter(modelService.uploadModelAsList(modelFiles, meta)).toCommandObject()
    }

    RevisionTransportCommand addRevision(String modelId, File file,
                ModelFormatTransportCommand format, String comment) throws ModelException {
        return DomainAdapter.getAdapter(modelService.addRevision(ModelAdapter.findByPerennialIdentifier(modelId), file,
                    ModelFormat.findByIdentifierAndFormatVersion(format.identifier,
                                    format.formatVersion), comment)).toCommandObject()
    }

    Boolean canAddRevision(String modelId) {
        return modelService.canAddRevision(ModelAdapter.findByPerennialIdentifier(modelId))
    }

    Boolean canDelete(String modelId) {
        return modelService.canDelete(ModelAdapter.findByPerennialIdentifier(modelId))
    }

    Boolean canShare(String modelId) {
        return modelService.canShare(ModelAdapter.findByPerennialIdentifier(modelId))
    }

    Boolean canPublish(String modelId) {
        def revision = getLatestRevision(modelId)
        if (revision.state == ModelState.UNPUBLISHED) {
            try {
                return modelService.canPublish(Revision.get(revision.id))
            }
            catch(Exception e) {
                return false
            }
        }
        return false
    }

    Boolean canCertify(String modelId) {
        def revision = getLatestRevision(modelId)
        if(!revision.qcInfo)
            return qcInfoDelegateService.canCertify(ModelAdapter.findByPerennialIdentifier(modelId))
        else
            return false
    }

    Boolean canSubmitForPublication(String modelId) {
        def revision = getLatestRevision(modelId)
        if ((revision.state == ModelState.UNPUBLISHED) && (revision.state != ModelState.UNDER_CURATION)) {
            try {
                return modelService.canSubmitForPublication(Revision.get(revision.id))
            } catch (Exception e) {
                return false
            }
        }
        return false
    }

    List<RepositoryFileTransportCommand> retrieveModelFiles(RevisionTransportCommand revision)
            throws ModelException {
        Revision theRevision = Revision.get(revision.id)
        List<RepositoryFileTransportCommand> files = modelService.retrieveModelFiles(theRevision)
        if (files && !files.isEmpty()) {
            files.each { it.revision = revision }
            /*
             * Add revision to the weak reference data structures, so its files are released
             * from disk.
             */
            referenceTracker.addReference(revision, files.first().path)
        }
        return files
    }

    List<RepositoryFileTransportCommand> retrieveModelFiles(String modelId) {
        return modelService.retrieveModelFiles(ModelAdapter.findByPerennialIdentifier(modelId))
    }

    void grantReadAccess(String modelId, User collaborator) {
        modelService.grantReadAccess(ModelAdapter.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    void grantWriteAccess(String modelId, User collaborator) {
        modelService.grantWriteAccess(ModelAdapter.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    boolean revokeReadAccess(String modelId, User collaborator) {
        return modelService.revokeReadAccess(ModelAdapter.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    boolean revokeWriteAccess(String modelId, User collaborator) {
        return modelService.revokeWriteAccess(ModelAdapter.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    void transferOwnerShip(String modelId, User collaborator) {
        modelService.transferOwnerShip(ModelAdapter.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    boolean deleteModel(String modelId) {
        def model = ModelAdapter.findByPerennialIdentifier(modelId)
        modelService.deleteModel(model)
    }

    boolean restoreModel(String modelId) {
        return modelService.restoreModel(ModelAdapter.findByPerennialIdentifier(modelId))
    }

    boolean deleteRevision(RevisionTransportCommand revision) {
        return modelService.deleteRevision(Revision.get(revision.id))
    }

    Collection<PermissionTransportCommand> getPermissionsMap(String modelId, boolean authenticated = true) {
        return modelService.getPermissionsMap(ModelAdapter.findByPerennialIdentifier(modelId), authenticated)
    }

    void setPermissions(String modelId, List<PermissionTransportCommand> permissions) {
        modelService.setPermissions(ModelAdapter.findByPerennialIdentifier(modelId), permissions)
    }

    RevisionTransportCommand getRevisionDetails(RevisionTransportCommand skeleton) {
        assert skeleton.id
        final String REV_ID = skeleton.id
        final Revision REV = Revision.get(REV_ID)
        if (!REV) {
            throw new IllegalArgumentException("Revision with id $REV_ID does not exist")
        }
        return DomainAdapter.getAdapter(REV).toCommandObject()
    }

    PublishContext publishModelRevision(RevisionTransportCommand revision) {
        return modelService.publishModelRevision(Revision.get(revision.id))
    }

    void unpublishModelRevision(RevisionTransportCommand revision) {
        modelService.unpublishModelRevision(Revision.get(revision.id))
    }

    void submitModelRevisionForPublication(RevisionTransportCommand revision) {
        modelService.submitModelRevisionForPublication(Revision.get(revision.id))
    }

    ModelTransportCommand findByPerennialIdentifier(String perennialId) {
        def model = ModelAdapter.findByPerennialIdentifier(perennialId)
        if (model) {
            return DomainAdapter.getAdapter(model).toCommandObject()
        }
        return null
    }

    RevisionTransportCommand getRevisionFromParams(final String MODEL, String REVISION = null) {
        String sanitisedModelId
        String sanitisedRevisionId
        final RevisionTransportCommand REV
        final boolean MODEL_ID_HAS_DOT = MODEL.contains('.')
        if (MODEL_ID_HAS_DOT) {
            String[] parts = MODEL.split("\\.")
            sanitisedModelId = parts[0]
            sanitisedRevisionId = parts[1]
        } else {
            sanitisedModelId = MODEL
        }
        final boolean PARSE_REVISION_ID = REVISION != null && sanitisedRevisionId == null
        if (PARSE_REVISION_ID) {
            // if revision is not an integer, then UrlMappings will error out.
            final int REVISION_ID = Integer.parseInt(REVISION)
            REV = getRevision(sanitisedModelId, REVISION_ID)
        } else if (sanitisedRevisionId) {
            final int REVISION_ID = Integer.parseInt(sanitisedRevisionId)
            REV = getRevision(sanitisedModelId, REVISION_ID)
        } else { // no revision was specified - pull the latest one.
            REV = getRevision(sanitisedModelId)
        }
        return REV
    }

    Set<String> getPerennialIdentifierTypes() {
        return ModelAdapter.PERENNIAL_IDENTIFIER_TYPES
    }

    boolean haveMultiplePerennialIdentifierTypes() {
        final boolean HAVE_PERENNIAL_PUBLICATION_ID = publicationIdGenerator instanceof
                    AbstractModelIdentifierGenerator && !(publicationIdGenerator instanceof
                    NullModelIdentifierGenerator)

        final Set<String> ID_TYPES = getPerennialIdentifierTypes()
        final boolean MANY_IDENTIFIERS = HAVE_PERENNIAL_PUBLICATION_ID || ID_TYPES.size() >= 2
        return MANY_IDENTIFIERS
    }
}
