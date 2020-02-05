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

import net.biomodels.jummp.core.model.FileFormatServiceAdapter
import org.apache.tika.Tika
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * <p>Abstract class for handling multiple format services sharing many common methods</p>
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *   <li>Mihai Glon&#x021b;&nbsp;<a href="mailto:mihai.glont@ebi.ac.uk">mihai.glont@ebi.ac.uk</a></li>
 *   <li>Tung Nguyen&nbsp;<a href="mailto:tung.nguyen@ebi.ac.uk">tung.nguyen@ebi.ac.uk</a></li>
 *  </ul>
 */
abstract class AbstractFormatDetectionService extends FileFormatServiceAdapter {
    static transactional = false
    public static final String EXPECTED_FORMAT_REQUIRED = "Please set expectedFormat before calling this method"
    protected final Logger logger = LoggerFactory.getLogger(this.getClass())
    protected final CommonFormat expectedFormat

    AbstractFormatDetectionService(CommonFormat expectedFormat) {
        this.expectedFormat = expectedFormat
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
            isWellKnownFile(f, mimeTypes)
        }
        return result
    }

    protected boolean isWellKnownFile(final File f, Set<String> mimeTypes) {
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
}
