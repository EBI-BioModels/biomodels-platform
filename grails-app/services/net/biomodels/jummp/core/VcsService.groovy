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
* Perf4j, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Perf4j, Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.core.vcs.VcsFileDetails
import net.biomodels.jummp.core.vcs.VcsManager
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.perf4j.aop.Profiled
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.access.prepost.PreAuthorize

/**
 * @short Service providing access to the version control system.
 *
 * This service allows the core to access the version control system provided by a plugin.
 * The service is only for internal use in the core by other services and should not be
 * made part of the external API.
 * @internal
 * @see VcsManager
 * @author  Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class VcsService implements InitializingBean {
    @SuppressWarnings('GrailsStatelessService')
    VcsManager vcsManager
    @SuppressWarnings('GrailsStatelessService')
    def grailsApplication
    def fileSystemService
    String modelContainerRoot

    void afterPropertiesSet() {
        modelContainerRoot = fileSystemService.root.canonicalPath
    }

    /**
     * Checks whether the Version Control System is configured properly
     * @return @c true if the vcs system is configured properly, @c false otherwise
     */
    boolean isValid() {
        return (vcsManager != null)
    }

    /**
    * Updates a Model file previously imported to the VCS.
    * Copies @p file into the working copy of the VCS and updates the existing file in the
    * VCS and the remote location of the VCS.
    * Use this method if the file had been imported previously.
    * @param model The Model representing the file in the VCS.
    * @param file The file to update
    * @param commitMessage The commit message to be used for the update.
    * @return Revision number of updated file.
    * @throws VcsException passes along the VcsException thrown by VcsManager
    **/
    @PreAuthorize("hasPermission(#model, write) or hasRole('ROLE_ADMIN')")
    @Profiled(tag = "vcsService.updateModel")
    String updateModel(final Model model, final List<File> files, final List<File> deleted, final String commitMessage) throws VcsException {
        if (!isValid()) {
            throw new VcsException("Version Control System is not valid")
        }

        String modelFolderPath = new StringBuffer(modelContainerRoot).append(
                    File.separator).append(model.vcsIdentifier).toString()
        final File MODEL_FOLDER = new File(modelFolderPath)
        if (commitMessage == null || commitMessage.isEmpty()) {
            return vcsManager.updateModel(MODEL_FOLDER, files, deleted, "Updated at ${new Date().toGMTString()}")
        } else {
            return vcsManager.updateModel(MODEL_FOLDER, files, deleted, commitMessage)
        }
    }

    @PreAuthorize("hasPermission(#model, write) or hasRole('ROLE_ADMIN')")
    @Profiled(tag = "vcsService.updateModel")
    String updateModel(final Model model, final File file, final String commitMessage) throws VcsException {
        return updateModel(model, [file], commitMessage);
    }
    /**
     * Imports a new Model file into the VCS.
     * Copies @p file into the working copy of the VCS and performs an initial import.
     * Use this method if the file has not been imported previously.
     * @param model The Model representing the new file in the VCS
     * @param file The file to import
     * @return Revision number of imported file.
     * @throws VcsException passes along the VcsException thrown by VcsManager
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @Profiled(tag = "vcsService.importModel")
    String importModel(final Model model, final List<File> files) throws VcsException {
        if (!isValid()) {
            throw new VcsException("Version Control System is not valid")
        }

        String root = fileSystemService.root
        final File MODEL_FOLDER = new File(root, model.vcsIdentifier)
        return vcsManager.updateModel(MODEL_FOLDER, files, null, "Imported model at ${new Date().toGMTString()}")
    }

    /**
     * Imports a new Model file into the VCS.
     * Copies @p file into the working copy of the VCS and performs an initial import.
     * Use this method if the file has not been imported previously.
     * @param model The Model representing the new file in the VCS
     * @param file The file to import
     * @return Revision number of imported file.
     * @throws VcsException passes along the VcsException thrown by VcsManager
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @Profiled(tag = "vcsService.importModel")
    String importModel(final Model model, final File file) throws VcsException {
        return importModel(model, [file])
    }

    /**
     * Retrieves Model File from VCS.
     * @param revision The Revision for which the Model file should be retrieved
     * @return File Handler to the retrieved file in the exchange directory.
     * It's the responsibility of the caller to delete the file when it is not needed any more.
     * @throws VcsException passes along the VcsException thrown by VcsManager
     */
    @PreAuthorize("hasPermission(#revision, read) or hasRole('ROLE_ADMIN')")
    @Profiled(tag = "vcsService.retrieveFiles")
    List<File> retrieveFiles(final Revision revision) throws VcsException {
        if (!isValid()) {
            throw new VcsException("Version Control System is not valid")
        }
        def latestRevId = Revision.withCriteria(uniqueResult: true) {
            eq("model.id", revision.model.id)
            projections {
                max("revisionNumber")
            }
        }
        final File MODEL_FOLDER = new File(modelContainerRoot, revision.model.vcsIdentifier)
        if (revision.revisionNumber == latestRevId) {
            return vcsManager.retrieveModel(MODEL_FOLDER)
        }

        return vcsManager.retrieveModel(MODEL_FOLDER, revision.vcsId)
    }

    public List<VcsFileDetails> getFileDetails(final Revision revision, String path)
                throws VcsException {
        if (!isValid()) {
            throw new VcsException("Version Control System is not valid")
        }
        final File MODEL_FOLDER = new File(modelContainerRoot, revision.model.vcsIdentifier)
        return vcsManager.getFileDetails(MODEL_FOLDER, path)
    }
}
