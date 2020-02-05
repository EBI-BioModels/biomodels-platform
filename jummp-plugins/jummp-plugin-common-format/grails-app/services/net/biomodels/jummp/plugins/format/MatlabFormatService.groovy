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
 **/

package net.biomodels.jummp.plugins.format

import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand

/**
 * <p>Individual class for handling detection of Matlab format</p>
 *
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *     <li><a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a></li>
 *     <li><a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glonț</a></li>
 * </ul>
 */
@CompileStatic
class MatlabFormatService extends AbstractFormatDetectionService {
    static transactional = false
    final String FORMAT_VERSION = '*'
    static final Set<String> MATLAB_MIME_TYPES = ["application/x-matlab",
                                                  "application/matlab",
                                                  "text/x-matlab",
                                                  "text/matlab"] as Set<String>

    boolean areFilesThisFormat(final List<File> files) {
        Set<String> mimeTypes = CommonFormat.MATLAB.acceptedMimeTypes
        areTheseFilesInThisFormat(mimeTypes, files)
    }

    String getFormatVersion(RevisionTransportCommand revision) {
        if (!revision || !isRevisionFormatSupported(revision)) {
            return null
        }
        FORMAT_VERSION
    }

    Set<File> getMatlabFilesFromRevision(RevisionTransportCommand r) {
        Set<File> files = new LinkedHashSet<>()
        List<RepositoryFileTransportCommand> repoFiles = r?.files
        for (RepositoryFileTransportCommand rf: repoFiles) {
            def f = new File(rf.path)
            if (isMatlabFile(f)) {
                files.add(f)
            }
        }
        files
    }

    private static boolean isRevisionFormatSupported(RevisionTransportCommand revisionCmd) {
        ModelFormatTransportCommand fmt = revisionCmd?.format
        return fmt?.identifier == "matlab"
    }

    private boolean isMatlabFile(File f) {
        checkMatlabFormat([f])
    }

    private boolean checkMatlabFormat(List<File> mainFiles) {
        areTheseFilesInThisFormat(CommonFormat.MATLAB.acceptedMimeTypes, mainFiles)
    }
}
