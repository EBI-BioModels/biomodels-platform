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

import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.ModellingApproach
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata

/**
 * <p>Individual class for handling detection of Matlab format</p>
 *
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *     <li><a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a></li>
 *     <li><a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glonț</a></li>
 * </ul>
 */
class MatlabFormatService extends AbstractFormatDetectionService {
    static transactional = false
    final String FORMAT_VERSION = '*'
    final static Set<String> MATLAB_MIME_TYPES = ["application/x-matlab",
                                                  "application/matlab",
                                                  "text/x-matlab",
                                                  "text/matlab"]

    boolean areFilesThisFormat(List<File> mainFiles) {
        if (!mainFiles) {
            return false
        }
        File result = mainFiles.find { File f ->
            isMatlabFile f
        }
        def names = mainFiles.collect {it.name}
        if (result) {
            logger.info "Treating ${names} as a Matlab submission"
        } else {
            logger.info "Submission ${names} does not contain Matlab scripts."
        }

        null != result
    }

    String getFormatVersion(RevisionTransportCommand revision) {
        if (!revision || !isRevisionFormatSupported(revision)) {
            return null
        }
        FORMAT_VERSION
    }

    Set<File> getMatlabFilesFromRevision(RevisionTransportCommand r) {
        def files = new LinkedHashSet()
        r?.files?.each { rf ->
            def f = new File(rf.path)
            if (isMatlabFile(f)) {
                files.add(f)
            }
        }
        files
    }

    private boolean isRevisionFormatSupported(RevisionTransportCommand revisionCmd) {
        ModelFormatTransportCommand fmt = revisionCmd?.format
        return fmt?.identifier == "matlab"
    }

    private boolean isMatlabFile(File f) {
        def mimeDetector = new DefaultDetector()
        def metadata = new Metadata()
        metadata.set(Metadata.RESOURCE_NAME_KEY, f.name)
        boolean result = f.withInputStream { InputStream stream ->
            try {
                String mime = mimeDetector.detect(stream, metadata)?.toString()
                log.debug "File $f has media type $mime"
                return mime in MATLAB_MIME_TYPES
            } catch (IOException e) {
                String n = f.name
                log.error("Could not probe $n for MIME type detection.", e)
            }
        }
        result
    }
}
