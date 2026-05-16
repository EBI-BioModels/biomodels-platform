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

package net.biomodels.jummp.plugins.format

import com.google.common.io.Files
import net.biomodels.jummp.core.model.FileFormatServiceAdapter
import org.apache.tika.Tika
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

/**
 * <p>Abstract class for handling multiple format services sharing many common methods</p>
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *   <li>Mihai Glon&#x021b;&nbsp;<a href="mailto:mihai.glont@ebi.ac.uk">mihai.glont@ebi.ac.uk</a></li>
 *   <li>Tung Nguyen&nbsp;<a href="mailto:tung.nguyen@ebi.ac.uk">tung.nguyen@ebi.ac.uk</a></li>
 *  </ul>
 */
abstract class AbstractFormatDetectionService extends FileFormatServiceAdapter implements InitializingBean {
    static transactional = false
    public static final String EXPECTED_FORMAT_REQUIRED = "Please set expectedFormat before calling this method"
    protected final Logger logger = LoggerFactory.getLogger(this.getClass())
    protected final CommonFormat expectedFormat

    AbstractFormatDetectionService(CommonFormat expectedFormat) {
        this.expectedFormat = expectedFormat
    }

    @Override
    void afterPropertiesSet() throws Exception {
        logger.info("Finished the bean initialisation")
    }

    /**
     * <p>Check whether any of the supplied files matches the mime types of the expected format.</p>
     *
     * <p>This method returns true if at least one of the supplied files has a MIME type registered for
     * {@code expectedFormat} and false otherwise.</p>
     *
     * @param files A List of {@linkplain File}s (typically, included in a certain submission)
     * @return  true/false
     * @throws NullPointerException if {@code expectedFormat} has not been initialised.
     */
    @Override
    boolean areFilesThisFormat(final List<File> files) {
        Set<String> mimeTypes = Objects.requireNonNull(expectedFormat, EXPECTED_FORMAT_REQUIRED)
            .acceptedMimeTypes
        def result = files.any { File f ->
            fileMimeTypeMatches(f, mimeTypes)
        }
        return result
    }

    static boolean hasExt(final File file, final String ext) {
        String fileExtension = Files.getFileExtension(file.name)
        boolean hasThisExt = fileExtension.equalsIgnoreCase(ext)
        hasThisExt
    }

    static boolean hasRoot(final File xmlBasedFile, final String root) {
        if (!hasExt(xmlBasedFile, "xml")) {
            return false
        }
        def parsedDoc = new XmlSlurper().parse(xmlBasedFile)
        parsedDoc.name().toLowerCase() == root?.toLowerCase()
    }

    /**
     * <p>Utility method for checking whether a file's MIME type falls in a given set.</p>
     *
     * @param f The file whose MIME type should be detected.
     * @param mimeTypes A non-null set of registered MIME types
     * @return true if the file's MIME type is any of the ones provided, false otherwise
     */
    protected boolean fileMimeTypeMatches(final File f, Set<String> mimeTypes) {
        try {
            String detectedMime = new Tika().detect(f)
            logger.debug("File $f has media type $detectedMime")
            boolean result = detectedMime in mimeTypes
            return result
        } catch (IOException e) {
            String n = f.name
            logger.error("Could not probe $n for MIME type detection.", e)
        }
        return false
    }

    boolean validate(final List<File> model, final List<String> errors) {
        areFilesThisFormat(model)
    }
}
