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

import org.apache.commons.io.FilenameUtils

/**
 * <p>Individual class for handling detection of Mathematica format</p>
 *
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *     <li><a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a></li>
 *     <li><a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glonț</a></li>
 * </ul>
 */
class MathematicaFormatService extends AbstractFormatDetectionService {
    static final String[] SUPPORT_EXTENSIONS =
            ["wl", "nb", "ma", "wxf", "wdx", "mx", "wlnet"] as String[]
    static transactional = false

    static {
        // so that we can use binary search inside isSupportedExtension(String)
        Arrays.sort(SUPPORT_EXTENSIONS)
    }

    MathematicaFormatService() {
        super(CommonFormat.MATHEMATICA)
    }

    @Override
    // TODO remove this once a fix for https://issues.apache.org/jira/browse/TIKA-3034 is released
    boolean areFilesThisFormat(List<File> files) {
        boolean result = files.any { File file ->
            String ext = FilenameUtils.getExtension(file.name)
            isSupportedExtension(ext)
        }
        result
    }

    private static boolean isSupportedExtension(String ext) {
        Arrays.binarySearch(SUPPORT_EXTENSIONS, ext) >= 0
    }
}
