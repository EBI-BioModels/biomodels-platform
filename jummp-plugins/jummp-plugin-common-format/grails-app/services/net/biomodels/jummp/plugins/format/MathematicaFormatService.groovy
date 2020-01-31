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
    /**
     * <p>This implementation does not invoke the corresponding method of the parent class as its sibling classes.
     * The reason is that Tika library seemingly detects such files incorrectly where it always returns 'text/plain'.
     * We have raised an <a href="https://issues.apache.org/jira/browse/TIKA-3034">issue</a> on the
     * <a href="https://issues.apache.org/jira/projects/TIKA/issues/TIKA-3034?filter=allopenissues">Tika issue tracker</a></p>
     *
     * <p>While waiting for Tika's developer team considering this issue, we simplify this implementation by checking
     * the file extension and will be improve once the issue is fixed. According to the <a href="https://reference
     * .wolfram.com/language/guide/WolframLanguageFileFormats.html">documentation</a> in Wolfram website, the
     * extension of Mathematica files can be one of these:  .wl, .m, .nb, .ma, .wxf, .wdx, .mx, .wlnet. </p>
     * <p style="font-weight: bold">Parameters:</p>
     * <ul>
     *   <li>files: the list of files to detect format</li>
     * </ul>
     * <p><span style="font-weight: bold">Returns:</span> a boolean value denoting there is at least one file which
     * extension belongs to the set of defined formats above</p>
     */
    @Override
    boolean areFilesThisFormat(List<File> files) {
        final Set<String> SUPPORT_FORMATS = ["wl", "m", "nb", "ma", "wxf", "wdx", "mx", "wlnet"] as HashSet<String>
        boolean result = files.any { File file ->
            String ext = FilenameUtils.getExtension(file.name)
            ext in SUPPORT_FORMATS
        }
        return result
    }
}
