/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.plugins.matlab

import net.biomodels.jummp.core.model.FileFormatService
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import org.apache.tika.mime.MimeTypes

//@groovy.transform.CompileStatic
class MatlabService implements FileFormatService {
    static transactional = false
    final Log log = LogFactory.getLog(getClass())
    final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    final String FORMAT_VERSION = '*'
    final static Set<String> TARGET_MIME_TYPES = ["application/x-matlab",
                                           "application/matlab",
                                           "text/x-matlab",
                                           "text/matlab"]

    def modelFileFormatService

    boolean areFilesThisFormat(List<File> mainFiles) {
        if (!mainFiles) {
            return false
        }
        File result = mainFiles.find { File f ->
            isMatlabFile f
        }
        if (IS_DEBUG_ENABLED) {
            def names = mainFiles.collect {it.name}
            if (result) {
                log.debug "Treating ${names} as a Matlab submission"
            } else {
                log.debug "Submission ${names} does not contain Matlab scripts."
            }
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

    boolean validate(List<File> files, List<String> errors) {
        true
    }

    String extractName(List<File> files) {
        null
    }

    String extractDescription(List<File> files) {
        null
    }

    boolean updateName(RevisionTransportCommand revisionCmd, String name) {
        true
    }

    boolean updateDescription(RevisionTransportCommand revisionCmd, String desc) {
        true
    }

    List<String> getAllAnnotationURNs(RevisionTransportCommand revision) {
        []
    }

    List getPubMedAnnotation(RevisionTransportCommand revision) {
        []
    }

    boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand newRevision) {
        true
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
                return mime in TARGET_MIME_TYPES
            } catch (IOException e) {
                String n = f.name
                log.error("Could not probe $n for MIME type detection.", e)
            }
        }
        result
    }
}
