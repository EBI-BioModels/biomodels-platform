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
 **/





package net.biomodels.jummp.core.model

import grails.transaction.Transactional
import net.biomodels.jummp.model.RepositoryFile
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata

/**
 * This class enables to handle services for manipulating repository files such as updating repository files,
 * retrieving these files from the file system, etc.
 *
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class RepositoryFileService {
    private static final Log log = LogFactory.getLog(RepositoryFileService.class)

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
                log.debug("""\
The repository file which is associated with the file $path has been updated its description: $description""")
                return Boolean.TRUE
            } else {
                log.debug("""\
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
}