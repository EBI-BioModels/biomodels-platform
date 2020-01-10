/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core.model

import grails.util.Holders
import net.biomodels.jummp.core.ModelException
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGenerator
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.RepositoryFile
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import org.perf4j.StopWatch
import org.perf4j.log4j.Log4JStopWatch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.acls.domain.BasePermission

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock

/**
 * Builds a ModelUpload object from a RevisionTransportCommand object and the list
 * of RepositoryFile ones
 *
 * This class provides us with an alternative way to construct complex Model objects.
 * It is called in ModelService.doUploadValidatedModel to build a Model from a defined
 * RevisionTransportCommand object and the uploading files representing in the list of
 * RepositoryFile objects.
 *
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author  Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class ModelBuilder {
    protected Logger logger = LoggerFactory.getLogger(getClass())
    def grailsApplication = Holders.grailsApplication.mainContext.getBean('grailsApplication')
    def aclUtilService = Holders.grailsApplication.mainContext.getBean('aclUtilService')
    def fileSystemService = Holders.grailsApplication.mainContext.getBean('fileSystemService')
    def publicationService = Holders.grailsApplication.mainContext.getBean('publicationService')
    def repositoryFileService = Holders.grailsApplication.mainContext.getBean('repositoryFileService')
    def springSecurityService = Holders.grailsApplication.mainContext.getBean('springSecurityService')
    def vcsService = Holders.grailsApplication.mainContext.getBean('vcsService')

    /**
     * Guard insertion of ACL entries from concurrent access.
     */
    final ReentrantLock aclInsertionLock = new ReentrantLock()

    private Model model
    private Revision revision
    private RevisionTransportCommand revisionTC
    private List<RepositoryFileTransportCommand> repoFiles

    /**
     * Provides a prototype-scoped submission id generator bean.
     */
    ModelIdentifierGenerator getSubmissionIdGenerator() {
        grailsApplication?.mainContext?.submissionIdGenerator
    }

    Model getModel() {
        return model
    }

    ModelBuilder setModel(Model model) {
        this.model = model
        return this
    }

    Revision getRevision() {
        return revision
    }

    ModelBuilder setRevision(Revision revision) {
        this.revision = revision
        return this
    }

    RevisionTransportCommand getRevisionTC() {
        return revisionTC
    }

    ModelBuilder setRevisionTC(RevisionTransportCommand revision) {
        this.revisionTC = revision
        return this
    }

    List<RepositoryFileTransportCommand> getRepoFiles() {
        this.repoFiles
    }

    ModelBuilder setRepoFiles(List<RepositoryFileTransportCommand> repoFiles) {
        this.repoFiles = repoFiles
        return this
    }

    ModelBuilder(List<RepositoryFileTransportCommand> repoFiles,
                 RevisionTransportCommand revisionTC) {
        this.model = new Model()
        this.model.revisions = new HashSet<>()
        this.revisionTC = revisionTC
        this.repoFiles = repoFiles
    }

    ModelBuilder build() {
        ModelBuilder modelBuilder = this
            .generateModelIdentifier()
            .createVcsIdentifier()
            .addModelInfo()
            .addPublication()
            .createFirstRevision()
            .uploadModelFiles()
        modelBuilder
    }

    void discard() {
        this.deleteModelWorkingDirectory()
        this.revision.discard()
        this.model.discard()
        for (RepositoryFile rf : this.revision.repoFiles) {
            rf.discard()
        }
        def msg = new StringBuffer("New Model ${this.revisionTC.name} does not validate:\n")
        msg.append("${model.errors.allErrors.inspect()}\n")
        msg.append("${revision.errors.allErrors.inspect()}\n")
        logger.error(msg.toString())
        throw new ModelException(new ModelAdapter(model: this.model).toCommandObject(), "New model does not validate")
    }

    void persist() {
        this.model.save(flush: true)
        for (RepositoryFile rf : this.revision.repoFiles) {
            if (!rf.isAttached()) {
                rf.attach()
            }
            String path = rf.path
            String sep = File.separator == ("/") ? "/" : "\\\\"
            if (path.contains(sep)) {
                String fileName = path.split(sep).last()
                rf.path = fileName
            }
            rf.save()
        }
        addRequiredRights()
        logger.debug("Model ${this.model.submissionId} stored with id ${this.model.id}")
    }

    private ModelBuilder generateModelIdentifier() {
        String submissionId = getSubmissionIdGenerator().generate()
        this.model.submissionId = submissionId
        return this
    }

    private ModelBuilder addModelInfo() {
        this.model.modellingApproach = this.revisionTC.model.modellingApproach
        this.model.otherInfo = this.revisionTC.model.otherInfo
        return this
    }

    private ModelBuilder addPublication() {
        if (this.revisionTC.model.publication) {
            this.model.publication = publicationService.fromCommandObject(this.revisionTC.model.publication)
        }
        return this
    }

    private ModelBuilder createVcsIdentifier() {
        // vcs identifier is container name + upload date + submissionId
        // this should by all means be unique
        String timestamp = new Date().format("yyyy-MM-dd'T'HH-mm-ss-SSS")
        final String submissionId = this.model.submissionId
        String modelPath = new StringBuilder(timestamp).append("_").append(submissionId).
            append(File.separator).toString()
        String container = fileSystemService.findCurrentModelContainer()
        String containerName = new File(container).name
        File modelFolder = new File(container, modelPath)
        boolean success = modelFolder.mkdirs()
        if (!success) {
            def err = "Cannot create the directory where the ${rev.name} should be stored"
            logger.error(err)
            throw new ModelException(this.revisionTC.model, err.toString())
        }
        this.model.vcsIdentifier = new StringBuilder(containerName).append(File.separator).
            append(modelPath).toString()

        logger.debug("The new model will be stored in $modelPath")
        return this
    }

    private ModelBuilder createFirstRevision() {
        this.model.revisions = new HashSet<>()
        ModelFormat format = ModelFormat.
            findByIdentifierAndFormatVersion(this.revisionTC.format.identifier,
                this.revisionTC.format.formatVersion)
        this.revision = new Revision(model: this.model,
            revisionNumber: 1,
            owner: User.findByUsername(springSecurityService.authentication.name),
            minorRevision: false,
            validated: this.revisionTC.validated,
            name: this.revisionTC.name,
            description: this.revisionTC.description,
            comment: this.revisionTC.comment,
            uploadDate: new Date(),
            format: format)
        this.model.revisions.add(this.revision)
        return this
    }

    private ModelBuilder uploadModelFiles() throws ModelException {
        StopWatch stopWatch = new Log4JStopWatch("modelService.uploadValidatedModel")
        // keep a list of RFs close by, as we may need to discard all of them
        List<RepositoryFile> domainObjects =
            repositoryFileService.convertRFTCToRF(this.repoFiles, this.revision)
        stopWatch.lap("Finished preparing what to store in the VCS.")
        stopWatch.setTag("modelService.uploadValidatedModel.doVcsStorage")
        List<File> modelFiles = repositoryFileService.getFilesFromRF(repoFiles)
        stopWatch.lap("Finished adding RepositoryFiles to the Model")
        stopWatch.setTag("modelService.uploadValidatedModel.prepareVcsStorage")
        try {
            String vcsId = vcsService.importModel(model, modelFiles)
            this.revision.vcsId = vcsId
            logger.debug "First commit for ${revision.model.vcsIdentifier} is $vcsId"
            stopWatch.lap("Finished importing the model into the VCS.")
            stopWatch.setTag("modelService.uploadValidatedModel.gormValidation")
            domainObjects.each {
                this.revision.addToRepoFiles(it)
            }
        } catch (VcsException e) {
            this.discard()
            def errMsg = new StringBuffer("Exception occurred while storing new Model ")
            errMsg.append("${model.errors.allErrors.inspect()}\n")
            errMsg.append("${this.revision.errors.allErrors.inspect()}\n")
            logger.error(errMsg.toString())
            ModelTransportCommand m = new ModelAdapter(model: this.model).toCommandObject()
            throw new ModelException(m, "Could not store new Model ${m.properties} in VCS", e)
            // TODO: store the exception into a class state instead of throwing it here
        }
        stopWatch.stop()
        return this
    }

    private void deleteModelWorkingDirectory() throws IOException {
        String workingDirectory = grailsApplication.config.jummp.vcs.workingDirectory
        String modelDirectory = this.model.vcsIdentifier
        Path absModelDir = Paths.get(workingDirectory, modelDirectory)
        fileSystemService.deleteDirectory(absModelDir)
    }

    private void addRequiredRights() {
        // let's add the required rights
        final String username = this.revision.owner.username
        aclInsertionLock.lock()
        try {
            aclUtilService.addPermission(model, username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(model, username, BasePermission.DELETE)
            aclUtilService.addPermission(model, username, BasePermission.READ)
            aclUtilService.addPermission(model, username, BasePermission.WRITE)
            aclUtilService.addPermission(revision, username, BasePermission.ADMINISTRATION)
            aclUtilService.addPermission(revision, username, BasePermission.DELETE)
            aclUtilService.addPermission(revision, username, BasePermission.READ)
        } catch (Throwable e) {
            logger.error("failed to insert permissions for $model and $revision", e)
            // TODO: store the exception into a class state instead of throwing it here
        } finally {
            aclInsertionLock.unlock()
        }
    }
}
