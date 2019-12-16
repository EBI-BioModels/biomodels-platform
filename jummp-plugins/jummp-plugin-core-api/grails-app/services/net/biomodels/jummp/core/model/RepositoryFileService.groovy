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
 */





package net.biomodels.jummp.core.model

import grails.transaction.Transactional
import net.biomodels.jummp.core.ModelException
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.RepositoryFile
import net.biomodels.jummp.model.Revision
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware
import org.eclipse.jgit.api.errors.NoHeadException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * This class enables to handle services for manipulating repository files such as updating repository files,
 * retrieving these files from the file system, etc.
 *
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class RepositoryFileService implements GrailsConfigurationAware {
    static scope = "prototype"

    private static final Logger logger = LoggerFactory.getLogger(RepositoryFileService.class)

    def modelService

    def vcsService

    def grailsApplication

    String MODEL_CACHE_DIR

    /**
     * Populate the model cache directory
     */
    @Override
    void setConfiguration(ConfigObject co) {
        MODEL_CACHE_DIR = grailsApplication.config.jummp.model.cache.dir
        if (!MODEL_CACHE_DIR) {
            String message = """\
The configuration file is missing the property of jummp.model.cache.dir"""
            logger.debug(message)
        }
    }

    /**
     * This method aims at updating the description of a given repository file.
     *
     * @param repoFileId    The identifier of the repository file
     * @param revisionId    The identifier of the revision which the repository file is gone with
     * @param path          The string represents the place where the repository file is hosting
     * @param description   The text denotes the description of the repository file
     * @return value        A logical value indicates whether the process is success or failed
     */
    @Transactional
    Boolean updateDescription(Long repoFileId, Long revisionId, String path, String description) {
        RepositoryFile rf = RepositoryFile.findById(repoFileId, [lock: true])
        // or RepositoryFile rf = RepositoryFile.lock(repoFileId)
        if (rf) {
            rf.description = description
            if (rf.save(flush: true)) {
                logger.debug("""\
The repository file which is associated with the file $path has been updated its description: $description""")
                return Boolean.TRUE
            } else {
                logger.debug("""\
There is an error when trying to update the description: $description --- of the repository file $path""")
                return Boolean.FALSE
            }
        }
        return Boolean.FALSE
    }

    /**
     * This method aims at converting a list of the physical files to the responding repository file objects
     *
     * @param   files   The list of physical files
     * @return  a list  The list of repository file objects
     */
    static List<RepositoryFileTransportCommand> asRFTCList(List<File> files) {
        List<RepositoryFileTransportCommand> results = new LinkedList<>()
        // work out MIME type
        def sherlock = new DefaultDetector()
        files.each { File file ->
            def is = new BufferedInputStream(new FileInputStream(file))
            String mimeType = sherlock.detect(is, new Metadata()).toString()
            RepositoryFileTransportCommand command = new RepositoryFileTransportCommand(
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
            String message = """\
Retrieving the revision ${revision.vcsId} for Model ${revision.model.submissionId} from the local model 
cache directory failed. The revision has been checked out from VCS instead."""
            logger.debug(message)
            // update the cache
            String modelId = revision.model.submissionId
            boolean updated = updateModelRevisionCache(modelId, revision.revisionNumber)
            if (updated) {
                message = """\
The model ${modelId} revision ${revision.revisionNumber} has been populated them to the  cache successfully"""
            } else {
                message = """\
The model ${modelId} revision ${revision.revisionNumber} has been failed when updating them to the  cache"""
            }
            logger.debug(message)
        }
        return files
    }

    List<File> get(long revisionId) {
        get(Revision.get(revisionId))
    }

    List<File> get(Revision revision) {
        get(revision.model.submissionId, revision.revisionNumber)
    }

    List<File> get(String modelId, int revisionNumber) throws FileNotFoundException {
        File modelDirectory = new File(MODEL_CACHE_DIR, modelId)
        File revisionDirectory
        List returnedFiles = new LinkedList<File>()
        try {
            revisionDirectory = new File(modelDirectory, revisionNumber.toString())
            returnedFiles = revisionDirectory.listFiles()?.toList()
        } catch (FileNotFoundException me) {
            Model model = modelService.getModel(modelId)
            boolean saveHistory = false
            ModelTransportCommand modelTC = new ModelAdapter(model: model).toCommandObject(saveHistory)
            String message = "The files associated with this model ${modelId}, revision ${revisionNumber} do not exist"
            throw new ModelException(modelTC, message)
        }
        return returnedFiles
    }

    boolean updateModelRevisionCache(final Revision revision) throws RuntimeException {
        String modelId = revision.model.submissionId
        String revNum = revision.revisionNumber.toString()
        logger.info("""\
Copying the files associated with the revision ${revision.vcsId} (${revision.id}): ${modelId}.${revNum}""")
        File modelRevDir = Paths.get(MODEL_CACHE_DIR, modelId, revNum).toFile()
        boolean created = modelRevDir.mkdirs()
        if (!created) {
            if (!modelRevDir.exists()) {
                String message = """\
we were unable to create the revision directory '${modelRevDir.absolutePath}'"""
                logger.warn(message)
                return false
            } else {
                logger.info("The directory '${modelRevDir.absolutePath}' exists")
            }
        }
        try {
            try {
                List<File> files = vcsService.retrieveFiles(revision)
                files.each {
                    Files.copy(it.toPath(),
                        new File(modelRevDir, it.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                return true
            } catch (NoHeadException | VcsException e) {
                logger.error("""\
There have been errors with VCS manager for the model $modelId, revision $revNum: $e.message""")
                return false
            }
        } catch (VcsException e) {
            logger.error("""\
Encountered VCS errors for model ${modelId}, revision number ${revNum} (${revision.vcsId})""")
            return false
        }
    }
}
