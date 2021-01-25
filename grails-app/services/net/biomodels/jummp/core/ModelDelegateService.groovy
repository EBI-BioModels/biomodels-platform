/**
* Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import com.google.common.io.Files
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.adapters.PublicationAdapter
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.model.audit.AccessFormat
import net.biomodels.jummp.core.model.audit.AccessType
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.vcs.VcsFileDetails
import net.biomodels.jummp.model.Flag
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.transaction.support.TransactionSynchronizationManager

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
@Transactional
class ModelDelegateService implements IModelService {
    private static final Log log = LogFactory.getLog(ModelDelegateService.class)

    def modelService
    def modelFileFormatService
    def qcInfoDelegateService
    def modelFlagService
    def referenceTracker

    @NotTransactional
    String getPluginForFormat(ModelFormatTransportCommand format) {
        return modelFileFormatService.getPluginForFormat(format)
    }

    List<ModelTransportCommand> getAllModels(int offset, int count, boolean sortOrder, ModelListSorting sortColumn) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(offset, count, sortOrder, sortColumn).each {
            models << new ModelAdapter(model: it).toCommandObject(false)
        }
        return models
    }

    List<ModelTransportCommand> getAllModels(int offset, int count, boolean sortOrder) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(offset, count, sortOrder).each {
            models << new ModelAdapter(model: it).toCommandObject(false)
        }
        return models
    }

    List<ModelTransportCommand> getAllModels(int offset, int count, ModelListSorting sortColumn) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(offset, count, sortColumn).each {
            models << new ModelAdapter(model: it).toCommandObject(false)
        }
        return models
    }

    List<ModelTransportCommand> getAllModels(int offset, int count) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(offset, count).each {
            models << new ModelAdapter(model: it).toCommandObject(false)
        }
        return models
    }

    List<ModelTransportCommand> getAllModels(ModelListSorting sortColumn) {
        List<ModelTransportCommand> models = []
        modelService.getAllModels(sortColumn).each {
            models << new ModelAdapter(model: it).toCommandObject(false)
        }
        return models
    }

    List<ModelTransportCommand> getAllModels() {
        List<ModelTransportCommand> models = []
        modelService.getAllModels().each {
            models << new ModelAdapter(model: it).toCommandObject(false)
        }
        return models
    }

    @NotTransactional
    Integer getModelCount() {
        return modelService.getModelCount()
    }

    @NotTransactional
    long createAuditItem(ModelAuditTransportCommand cmd) {
        return modelService.createAuditItem(cmd)
    }

    @NotTransactional
    void updateAuditSuccess(Long itemId, boolean success) {
        modelService.updateAuditSuccess(itemId, success)
    }

    @Transactional(readOnly = true)
    List<VcsFileDetails> getFileDetails(long revID, String filename) {
        return modelService.getFileDetails(Revision.get(revID), filename)
    }

    ModelTransportCommand getModel(String modelId) {
        return new ModelAdapter(model: modelService.getModel(modelId)).toCommandObject()
    }

    RevisionTransportCommand getLatestRevision(String modelId, boolean addToHistory = true) {
        Model model = modelService.findByPerennialIdentifier(modelId)
        if (!model) {
            throw new AccessDeniedException("No access to any revision of Model ${modelId}")
        }
        Revision rev = modelService.getLatestRevision(model, addToHistory)
        if (rev) {
            return new RevisionAdapter(revision: rev).toCommandObject()
        } else {
            throw new AccessDeniedException("No access to any revision of Model ${modelId}")
        }
    }

    List<RevisionTransportCommand> getAllRevisions(String modelId) {
        def model = modelService.findByPerennialIdentifier(modelId)
        def revs = modelService.getAllRevisions(model)
        def msg = """Fetching revisions ${revs*.id} for $modelId. Attachment to current session: ${revs*.isAttached()}
transactionStatus: ${transactionStatus /* injected by org.codehaus.groovy.grails.transaction.transform.TransactionalTransform*/} ;
session: ${TransactionSynchronizationManager.getResource(grails.util.Holders.applicationContext.sessionFactory)
            .session.persistenceContext.entitiesByKey.collect {
            def instance = it.value
            "{${instance.class.name} ${instance.hasProperty('id') ? instance.id : instance.toString() }}" }.toString()}
"""
        log.info(msg.toString())

        List<RevisionTransportCommand> revisions = []
        revs.each {
            revisions << new RevisionAdapter(revision: it).toCommandObject()
        }
        log.info("All Revision Transport Command Objects: ${revisions.dump()}")
        return revisions
    }

    RevisionTransportCommand getRevision(String identifier) {
        return new RevisionAdapter(revision: modelService.getRevision(identifier)).toCommandObject()
    }

    RevisionTransportCommand getRevision(String modelId, int revisionNumber) {
        return new RevisionAdapter(revision: modelService.getRevision(
                    modelService.findByPerennialIdentifier(modelId), revisionNumber)).toCommandObject()
    }

    PublicationTransportCommand getPublication(String modelId) throws AccessDeniedException,
                IllegalArgumentException {
        def publication = modelService.getPublication(
                               modelService.findByPerennialIdentifier(modelId))
        if (publication) {
            return new PublicationAdapter(publication: publication).toCommandObject()
        }
        return null
    }

    ModelTransportCommand uploadModel(List<File> modelFiles, ModelTransportCommand meta) throws
                ModelException {
        return new ModelAdapter(model: modelService.uploadModelAsList(modelFiles, meta)).toCommandObject()
    }

    RevisionTransportCommand addRevision(String modelId, File file,
                ModelFormatTransportCommand format, String comment) throws ModelException {
        Model model = modelService.findByPerennialIdentifier(modelId)
        ModelFormat modelFormat = ModelFormat.findByIdentifierAndFormatVersion(format.identifier,
            format.formatVersion)
        Revision revision = modelService.addRevisionAsFile(model, file, modelFormat, comment)
        return new RevisionAdapter(revision: revision).toCommandObject()
    }

    RevisionTransportCommand addRevision(final List<RepositoryFileTransportCommand> repoFiles,
                                         final List<RepositoryFileTransportCommand> deleteFiles,
                                         final RevisionTransportCommand rev) throws ModelException {
        Revision revision = modelService.addRevision(repoFiles, deleteFiles, rev)
        RevisionTransportCommand revisionTC = new RevisionAdapter(revision: revision).toCommandObject()
        return revisionTC
    }

    @NotTransactional
    Byte[] serveModelFilesAsZip(Map<String, RepositoryFileTransportCommand> files) {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()
        ZipOutputStream zipFile = new ZipOutputStream(byteBuffer)
        files.each { String modelId, RepositoryFileTransportCommand cmd ->
            File file = new File(cmd.path)
            String extension = Files.getFileExtension(file.getName())
            ZipEntry entry = new ZipEntry("${modelId}.$extension")
            zipFile.putNextEntry(entry)
            byte[] fileData = file.getBytes()
            zipFile.write(fileData, 0, fileData.length)
            zipFile.closeEntry()
        }
        zipFile.close()
        byte[] response = byteBuffer.toByteArray()
        response
    }

    @NotTransactional
    Byte[] serveModelFilesAsZip(String[] modelIDs) {
        Map<String, RepositoryFileTransportCommand> files = modelService.fetchMainFileForModels(modelIDs)
        if (files) {
            return serveModelFilesAsZip(files)
        } else
        return null
    }

    List<FlagTransportCommand> getFlags(final String modelId) {
        Model model = modelService.findByPerennialIdentifier(modelId)
        List<FlagTransportCommand> results = new ArrayList<FlagTransportCommand>()
        if (model != null) {
            List<Flag> flags = modelFlagService.getFlags(model)
            if (!flags.empty) {
                use(FlagCategory) {
                    results = flags.collect { Flag flag ->
                        flag.toCommandObject()
                    }
                }
                return results
            }
        }
        return Collections.emptyList()
    }

    @NotTransactional
    Boolean canAddRevision(String modelId) {
        return modelService.canAddRevision(modelService.findByPerennialIdentifier(modelId))
    }

    @NotTransactional
    Boolean canDelete(String modelId) {
        return modelService.canDelete(modelService.findByPerennialIdentifier(modelId))
    }

    @NotTransactional
    Boolean canShare(String modelId) {
        return modelService.canShare(modelService.findByPerennialIdentifier(modelId))
    }

    @NotTransactional
    Boolean canPublish(RevisionTransportCommand revision) {
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

    @NotTransactional
    Boolean canPublish(String modelId) {
        def revision = getLatestRevision(modelId)
        canPublish(revision)
    }

    @NotTransactional
    Boolean canCertify(RevisionTransportCommand revision) {
        if (revision.qcInfo) return false
        qcInfoDelegateService.canCertify(revision.modelIdentifier())
    }

    @NotTransactional
    Boolean canCertify(String modelId) {
        def revision = getLatestRevision(modelId)
        canCertify(revision)
    }

    @NotTransactional
    Boolean canCheckConsistency(RevisionTransportCommand revision) {
        Revision actualRevision = Revision.get(revision.id)
        modelService.canCheckConsistency(actualRevision)
    }

    @NotTransactional
    Boolean canSubmitForPublication(RevisionTransportCommand revision) {
        if ((revision.state == ModelState.UNPUBLISHED)) {
            try {
                return modelService.canSubmitForPublication(Revision.get(revision.id))
            } catch (Exception e) {
                return false
            }
        }
        return false
    }

    @NotTransactional
    Boolean canSubmitForPublication(String modelId) {
        def revision = getLatestRevision(modelId)
        canSubmitForPublication(revision)
    }

    List<RepositoryFileTransportCommand> retrieveModelFiles(RevisionTransportCommand revision)
            throws ModelException {
        Revision theRevision = Revision.get(revision.id)
        List<RepositoryFileTransportCommand> files = modelService.retrieveModelFiles(theRevision)
        if (!files?.isEmpty()) {
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
        return modelService.retrieveModelFiles(modelService.findByPerennialIdentifier(modelId))
    }

    void grantReadAccess(String modelId, User collaborator) {
        modelService.grantReadAccess(modelService.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    void grantWriteAccess(String modelId, User collaborator) {
        modelService.grantWriteAccess(modelService.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    boolean revokeReadAccess(String modelId, User collaborator) {
        return modelService.revokeReadAccess(modelService.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    boolean revokeWriteAccess(String modelId, User collaborator) {
        return modelService.revokeWriteAccess(modelService.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    void transferOwnerShip(String modelId, User collaborator) {
        modelService.transferOwnerShip(modelService.findByPerennialIdentifier(modelId),
                    User.get(collaborator.id))
    }

    boolean deleteModel(String modelId) {
        def model = modelService.findByPerennialIdentifier(modelId)
        modelService.deleteModel(model)
    }

    boolean restoreModel(String modelId) {
        return modelService.restoreModel(modelService.findByPerennialIdentifier(modelId))
    }

    boolean deleteRevision(RevisionTransportCommand revision) {
        return modelService.deleteRevision(Revision.get(revision.id))
    }

    Collection<PermissionTransportCommand> getPermissionsMap(String modelId, boolean authenticated = true) {
        return modelService.getPermissionsMap(modelService.findByPerennialIdentifier(modelId), authenticated)
    }

    void setPermissions(String modelId, List<PermissionTransportCommand> permissions) {
        modelService.setPermissions(modelService.findByPerennialIdentifier(modelId), permissions)
    }

    RevisionTransportCommand getRevisionDetails(RevisionTransportCommand skeleton) {
        assert skeleton.id
        final String REV_ID = skeleton.id
        final Revision REV = Revision.get(REV_ID)
        if (!REV) {
            throw new IllegalArgumentException("Revision with id $REV_ID does not exist")
        }
        return new ModelAdapter(model: REV).toCommandObject()
    }

    RevisionTransportCommand publishModelRevision(RevisionTransportCommand cmd) {
        Revision revision = Revision.get(cmd.id)
        Revision published = modelService.publishModelRevision(revision)
        new RevisionAdapter(revision: published).toCommandObject()
    }

    void unpublishModelRevision(RevisionTransportCommand revision) {
        modelService.unpublishModelRevision(Revision.get(revision.id))
    }

    void submitModelRevisionForPublication(RevisionTransportCommand revision) {
        modelService.submitModelRevisionForPublication(Revision.get(revision.id))
    }

    ModelTransportCommand findByPerennialIdentifier(String perennialId) {
        def model = modelService.findByPerennialIdentifier(perennialId)
        if (model) {
            return new ModelAdapter(model: model).toCommandObject()
        }
        return null
    }

    /**
     * Update curation status of specific model revision
     * @param modelId: submissionId of model
     * @param revisionNumber: revision number
     * @param curationState: curation status
     */
    RevisionTransportCommand updateCurationStateRevision(String modelId, int revisionNumber,
            CurationState curationState) {
        Revision revision = modelService.getRevision(
            modelService.findByPerennialIdentifier(modelId), revisionNumber)
        revision = modelService.updateRevisionCurationState(revision, curationState)
        new RevisionAdapter(revision: revision).toCommandObject()
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

    @NotTransactional
    boolean haveMultiplePerennialIdentifierTypes() {
        def publicationIdGenerator = modelService.publicationIdGenerator
        final boolean HAVE_PERENNIAL_PUBLICATION_ID = !(publicationIdGenerator instanceof
                    NullModelIdentifierGenerator)

        final Set<String> ID_TYPES = modelService.getPerennialIdentifierTypes()
        final boolean MANY_IDENTIFIERS = HAVE_PERENNIAL_PUBLICATION_ID || ID_TYPES.size() >= 2
        return MANY_IDENTIFIERS
    }

    Map<Long, String> findModelsByPerennialId(List<String> identifiers) {
        Map results = [:]
        for (String id : identifiers) {
            Model model = modelService.findByPerennialIdentifier(id)
            if (model) {
                results[model.id] = id
            }
        }
        results
    }

    int updateHistory(String modelId, String user, String accessType,
                      String formatType, String changesMade, boolean success = false) {
        ModelTransportCommand model = findByPerennialIdentifier(modelId)
        updateHistory(model, user, accessType, formatType, changesMade, success)
    }

    int updateHistory(ModelTransportCommand model, String user, String accessType,
                      String formatType, String changesMade, boolean success = false) {
        accessType = accessType.replace("/model/","")
        AccessFormat format = AccessFormat.HTML
        try {
            format = AccessFormat.valueOf(formatType.toUpperCase())
        } catch(Exception ignore) {

        }
        ModelAuditTransportCommand audit = new ModelAuditTransportCommand(
            model: model,
            username: user,
            format: format,
            type: AccessType.fromAction(accessType),
            changesMade: changesMade,
            success: success)
        return createAuditItem(audit)
    }
}
