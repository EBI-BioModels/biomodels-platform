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
* Apache Tika, Apache Commons, Perf4j (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Tika, Apache Commons, Perf4j used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.omex

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import net.biomodels.jummp.core.model.FileFormatService
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType

/**
 * Provides methods to handle the COMBINE archive format.
 * @see net.biomodels.jummp.core.model.FileFormatService
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class OmexService implements FileFormatService {
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()

    @Profiled(tag="omexService.validate")
    public boolean validate(final List<File> model, final List<String> errors) {
        //TODO delegate the validation to libCombineArchive API
        return areFilesThisFormat(model)
    }

    @Profiled(tag="omexService.extractName")
    public String extractName(final List<File> model) {
        return ""
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Profiled(tag="omexService.updateName")
    public boolean updateName(RevisionTransportCommand revision, final String name) {
        if (revision && name.trim()) {
            revision.name = name.trim()
            return true
        }
        return false
    }

    @Profiled(tag="omexService.extractDescription")
    public String extractDescription(final List<File> model) {
        return ""
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Profiled(tag="omexService.updateDescription")
    public boolean updateDescription(RevisionTransportCommand revision, final String DESC) {
        if (revision && DESC.trim()) {
            revision.description = DESC.trim()
            return true
        }
        return false
    }

    @Profiled(tag="omexService.getAllAnnotationURNs")
    public List<String> getAllAnnotationURNs(RevisionTransportCommand revision) {
        return []
    }

    @Profiled(tag="omexService.getPubMedAnnotation")
    public List<String> getPubMedAnnotation(RevisionTransportCommand revision) {
        return []
    }

    /**
     * Detects whether the supplied files are in the format supported by this Service
     * @param files a list of files that should be checked
     * @return true if all files are supported, false otherwise.
     * @see net.biomodels.jummp.core.model.FileFormatService#areFilesThisFormat(List files)
     */
    @Profiled(tag="omexService.areFilesThisFormat")
    public boolean areFilesThisFormat(final List<File> files) {
        if (!files) {
            return false
        }
        boolean allGood = true
        def iFiles = files.iterator()
        while (iFiles.hasNext() && allGood) {
            final File theFile = iFiles.next()
            if (!isOmexFormat(theFile)) {
                allGood = false
            }
        }
        return allGood
    }

    @Profiled(tag="omexService.getFormatVersion")
    public String getFormatVersion(RevisionTransportCommand revision) {
        return revision ? "0.1" : "*"
    }

    /*
     * Helper method that checks if a file appears to be a valid COMBINE archive.
     * @param f the file in question
     * @return true if the supplied file is a COMBINE archive, false otherwise
     */
    private boolean isOmexFormat(final File f) {
        if (!f || !f.canRead()) {
            return false
        }
        if (IS_INFO_ENABLED) {
            log.info "Validating ${f.properties}"
        }
        def path = f.toPath()
        //TODO if null, we need a custom FileTypeDetector
        def sherlock = new DefaultDetector()
        String properType = sherlock.detect(new BufferedInputStream(
                        new FileInputStream(f)), new Metadata()).toString()
                
        boolean correctMIME = "application/zip".equals(properType)
        if (!correctMIME) {
            if (IS_INFO_ENABLED) {
                log.info "Not treating ${f.name} as COMBINE archive because of incorrect content type. ${properType}"
            }
            return false
        }
        boolean correctExtension = path.toString().endsWith(".omex")
        if (!correctExtension) {
            if (IS_INFO_ENABLED) {
                log.info "Not treating ${f.name} as COMBINE archive because of incorrect file extension."
            }
            return false
        }
        FileSystem fs
        boolean containsManifest
        try {
            fs = FileSystems.newFileSystem(path, null)
            final String MANIFEST_LOCATION =  "manifest.xml"
            Path manifestPath = fs.getPath(MANIFEST_LOCATION)
            containsManifest = Files.exists(manifestPath) &&
                        Files.isReadable(manifestPath) && Files.isRegularFile(manifestPath)
        } finally {
            fs?.close()
        }

            if (IS_INFO_ENABLED) {
                StringBuilder msg = new StringBuilder("File ")
                msg.append(f.name).append(" is")
                msg.append(containsManifest ? "" : " not").append(" a COMBINE archive.")
                msg.append(containsManifest ?: " The manifest file is missing.")
                log.info(msg.toString())
            }
        return containsManifest
    }

    boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand rev) {
        return true
    }
}
