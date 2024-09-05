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

import grails.plugins.rest.client.RestBuilder
import grails.transaction.Transactional
import net.biomodels.jummp.core.ModelException
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.RepositoryFile
import net.biomodels.jummp.model.Revision
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * This class enables to handle services for manipulating repository files such as updating repository files,
 * retrieving these files from the file system, etc.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class RepositoryFileService implements GrailsConfigurationAware {
    static scope = "prototype"
    private static final Logger logger = LoggerFactory.getLogger(RepositoryFileService.class)
    def configurationService
    def modelService
    def vcsService
    def grailsApplication

    private String modelCacheDir

    private String srvFtpDataMover

    /**
     * Populate the model cache directory
     */
    void setConfiguration(ConfigObject co) {
        modelCacheDir = co.jummp.model.cache.dir
        modelCacheDir = modelCacheDir.trim()
        if (!modelCacheDir) {
            String message = "The configuration file is missing the property of jummp.model.cache.dir"
            logger.debug(message)
        }

        // the hostname of the server running FTP Data Mover service
        srvFtpDataMover = co.jummp.revision.ftpdatamover.srv
        logger.info("Finished the bean initialisation")
    }

    /**
     * Gets the model cache directory
     *
     * @return A string denoting the model cache directory
     */
    String getModelCacheDir() {
        return modelCacheDir
    }

    /**
     * Sets the given string in the argument as the model cache directory
     *
     * @param location A string denoting the directory of the model cache
     */
    void setModelCacheDir(final String location) {
        modelCacheDir = location
    }

    String getSrvFtpDataMover() {
        return srvFtpDataMover
    }

    void setSrvFtpDataMover(String srvFtpDataMover) {
        this.srvFtpDataMover = srvFtpDataMover
    }

    /**
     * This method aims at updating the description of a given repository file.
     *
     * @param repoFileId The identifier of the repository file
     * @param revisionId The identifier of the revision which the repository file is gone with
     * @param path The string represents the place where the repository file is hosting
     * @param description The text denotes the description of the repository file
     * @return value        A logical value indicates whether the process is success or failed
     */
    @Transactional
    Boolean updateDescription(Long repoFileId, Long revisionId, String path, String description) {
        RepositoryFile rf = RepositoryFile.findById(repoFileId, [lock: true])
        // or RepositoryFile rf = RepositoryFile.lock(repoFileId)
        if (rf) {
            rf.description = description
            if (rf.save(flush: true)) {
                logger.debug("""The repository file which is associated with the file $path \
has been updated its description: $description""")
                return Boolean.TRUE
            } else {
                logger.debug("""There is an error when trying to update the description: \
$description --- of the repository file $path""")
                return Boolean.FALSE
            }
        }
        return Boolean.FALSE
    }

    /**
     * This method aims at converting a list of the physical files to the responding repository file objects
     *
     * @param files The list of physical files
     * @return a list  The list of repository file objects
     */
    static List<RFTC> asRFTCList(List<File> files) {
        List<RFTC> results = new LinkedList<>()
        // work out MIME type
        def sherlock = new DefaultDetector()
        files.each { File file ->
            def is = new BufferedInputStream(new FileInputStream(file))
            String mimeType = sherlock.detect(is, new Metadata()).toString()
            RFTC command = new RFTC(
                path: file.name,
                description: file.name,
                mimeType: mimeType
            )
            is.close();
            results.add(command)
        }
        results
    }

    List retrieveFiles(final Revision revision) {
        // look in the cache and either serve what's there, or fetch from VCS and update the cache
        List files
        try {
            files = get(revision)
        } catch (ModelException e) {
            files = vcsService.retrieveFiles(revision)
            // log the result
            String modelId = "${revision.model.submissionId}.${revision.revisionNumber}"
            String message = """Retrieving the revision ${modelId} from the local model cache directory failed. \
The revision has been checked out from VCS instead."""
            logger.debug(message)
            if (files?.size()) {
                // update the cache directory of this revision
                doUpdateModelRevisionCacheDirectory(revision)
            } else {
                message = """The revision ${modelId} (commit id:${revision.vcsId}) has no files"""
                logger.error(message)
            }
        }
        return files
    }

    List<File> get(long revisionId) throws ModelException {
        get(Revision.get(revisionId))
    }

    List<File> get(Revision revision) throws ModelException {
        get(revision.model.submissionId, revision.revisionNumber)
    }

    List<File> get(String modelId, int revisionNumber) throws ModelException {
        File modelDirectory = new File(modelCacheDir, modelId)
        File revisionDirectory
        List returnedFiles = new LinkedList<File>()
        try {
            revisionDirectory = new File(modelDirectory, revisionNumber.toString())
            if (!revisionDirectory.exists()) {
                throw new FileNotFoundException()
            } else {
                returnedFiles = hideSomeFileTypes(revisionDirectory, 'indexData.json')
            }
            if (returnedFiles?.isEmpty()) {
                String message = """The cache directory of this model ${modelId} revision ${revisionNumber} is empty. \
 The model cache builder will be launched again."""
                throwModelException(modelId, message)
            }
        } catch (FileNotFoundException me) {
            String message = """The files associated with this model ${modelId}, \
revision ${revisionNumber} hasn't been cached yet"""
            throwModelException(modelId, message)
        }
        return returnedFiles
    }

    boolean updateModelRevisionCache(final Revision revision) {
        String modelId = revision.model.submissionId
        String revNum = revision.revisionNumber.toString()
        logger.debug("""Copying the files associated with the revision ${revision.vcsId} \
(${revision.id}): ${modelId}.${revNum}""")
        File modelRevDir = Paths.get(modelCacheDir, modelId, revNum).toFile()
        boolean created = modelRevDir.mkdirs()
        if (!created) {
            if (!modelRevDir.exists()) {
                String message = "We were unable to create the revision directory '${modelRevDir.absolutePath}'"
                logger.debug(message)
                return false
            } else {
                logger.debug("The directory '${modelRevDir.absolutePath}' exists")
            }
        }
        boolean result = false
        try {
            List<File> files = vcsService.retrieveFiles(revision)
            files = hideSomeFileTypes(files, 'indexData.json')
            for (File it : files) {
                String fileName = it.getName()
                logger.debug("File ${fileName} is being copied")
                Files.copy(it.toPath(),
                    new File(modelRevDir, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            result = true
        } catch (VcsException e) {
            logger.error("""There have been errors with VCS manager for the model \
$modelId, revision $revNum: ${e.message}""")
            result = false
        } catch (IOException e) {
            logger.error("""IO exception encountered for model $modelId (revision $revNum): $e""")
            result = false
        }
        return result
    }

    List<RFTC> getRepositoryFilesForRevision(final Revision revision) {
        List<RFTC> repFiles = new LinkedList<RFTC>()
        List<File> files = retrieveFiles(revision)
        revision.repoFiles.each { rf ->
            File tmpFile = files.find { it.getName() == (new File(rf.path)).getName() }
            if (tmpFile != null) {
                long size = tmpFile.length()
                long configPreviewSize = grailsApplication.config.jummp.web.file.preview
                boolean showPreview = size < configPreviewSize
                RFTC rftc = new RFTC(
                    id: rf.id,
                    path: tmpFile.absolutePath,
                    filename: rf.path,
                    size: size,
                    showPreview: showPreview,
                    description: rf.description,
                    hidden: rf.hidden,
                    mainFile: rf.mainFile,
                    userSubmitted: rf.userSubmitted,
                    mimeType: rf.mimeType)
                repFiles.add(rftc)
            }
        }
        return repFiles
    }

    List<File> getFilesFromRF(List<RFTC> files) {
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
     * Creates validated RepositoryFile objects from the corresponding RepositoryFileTransportCommands.
     *
     * This method is used to complement the validation mechanism available for domain
     * classes because the latter is applied even for operations that don't change repository file
     * objects such as deletion or publishing of models.
     *
     * This method throws ModelException if
     *      there is at least one entry in the supplied list with an undefined or non-existent path,
     *      there is at least one empty file, or
     *      there are no main files.
     *
     * @param repoFileCmds a list of RepositoryFileTransportCommand objects to validate and convert into
     *                      domain objects.
     * @param revision a Revision object
     * @return a list of RepositoryFile domain objects
     */
    List<RepositoryFile> convertRFTCToRF(List<RFTC> repoFileCmds,
                                         Revision revision) {
        List<RepositoryFile> results = []
        boolean foundValidMainFile = false
        ModelAdapter adapter = new ModelAdapter(model: revision.model, latest: revision)
        final def m = adapter.toCommandObject(false)
        for (rf in repoFileCmds) {
            // validate
            String filePath = rf.path
            if (!filePath) {
                logger.error("Missing path for RepositoryFile ${rf.dump()} from ${repoFileCmds.dump()}")
                throw new ModelException(m, "We lost track of one of the files you provided for this revision.")
            }
            File f = new File(filePath)
            boolean fileExists = f.exists()
            if (!fileExists) {
                logger.error("Non-existent path for RepositoryFile ${rf.dump()} from ${repoFileCmds.dump()}")
                throw new ModelException(m, "There was a problem saving file ${f.name} for this revision.".toString())
            }
            boolean fileIsEmpty = !f.length()
            if (fileIsEmpty) {
                logger.debug("Empty file ${f.name} included in ${repoFileCmds}")
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
                def msg = new StringBuffer("Invalid file ${rf.properties} uploaded for model ${m.properties}.")
                msg.append("The file failed due to ${domain.errors.allErrors.inspect()}")
                logger.error(msg.toString())
                msg = """Your submission appears to contain invalid file ${fileName}. Please review it and try again."""
                throw new ModelException(m, msg)
            } else {
                results.add(domain)
            }
        }
        if (!foundValidMainFile) {
            def msg = """Can't persist repository files ${repoFileCmds.dump()} \
for revision ${revision.dump()} without main file"""
            logger.error(msg)
            throw new ModelException(m, "Missing main file for the new model revision ${revision.name}")
        }
        results
    }

    String copyRevisionFilesToFtp(final Revision revision) {
        Proxy proxy = configurationService.verifyHttpProxy()
        RestBuilder rest
        if (proxy) {
            rest = new RestBuilder(connectTimeout: 10000, readTimeout: 100000, proxy: proxy)
        } else {
            rest = new RestBuilder(connectTimeout: 10000, readTimeout: 100000)
        }

        String SRV_URL = getSrvFtpDataMover()
        String vcsId = revision.model.vcsIdentifier
        String parentFolderName = vcsId.take(3)
        String submissionId = revision.model.submissionId
        int revisionNumber = revision.revisionNumber
        String site = "prod"
        String serverURL = grailsApplication.config.grails.serverURL as String
        if (serverURL.contains("wwwdev")) {
            site = "dev"
        }
        String queryURL = "${SRV_URL}/model/publish/${site}/${parentFolderName}/${submissionId}/${revisionNumber}"
        def response = rest.get(queryURL) {
            accept("application/json")
            contentType("application/json;charset=UTF-8")
        }
        logger.debug(response?.text)
        response?.text
    }

    private void doUpdateModelRevisionCacheDirectory(final Revision revision) {
        boolean updated = updateModelRevisionCache(revision)
        String modelId = revision.model.submissionId
        String message = ""
        if (updated) {
            message = """The model ${modelId} revision ${revision.revisionNumber} \
has been populated them to the cache successfully"""
            logger.debug(message)
        } else {
            message = """There have been errors when updating the cache directory for \
the model ${modelId} revision ${revision.revisionNumber}"""
            logger.error(message)
        }
    }

    private void throwModelException(final String modelId, final String message) throws ModelException {
        Model model = modelService?.getModel(modelId)
        if (!model) {
            String errMsg = "The model ${modelId} does not exist"
            throw new ModelException(errMsg)
        }
        boolean saveHistory = false
        ModelTransportCommand modelTC = new ModelAdapter(model: model).toCommandObject(saveHistory)
        logger.error(message)
        throw new ModelException(modelTC, message)
    }

    private static List<File> hideSomeFileTypes(final File revisionDirectory, String filenameToBeFiltered /*String fileExtension*/) {
        List<File> returnedFiles = revisionDirectory.listFiles().toList()
        returnedFiles = hideSomeFileTypes(returnedFiles, filenameToBeFiltered)
        return returnedFiles
    }

    private static List<File> hideSomeFileTypes(final List<File> listOfFiles, String filenameToBeFiltered /*String fileExtension*/) {
        List<File> returnedFiles = listOfFiles.findAll {
            String filename = it?.name
            //String extension = filename?.substring(filename?.lastIndexOf(".") + 1)
            //extension != fileExtension
            filenameToBeFiltered != filename
        }
        return returnedFiles
    }
}
