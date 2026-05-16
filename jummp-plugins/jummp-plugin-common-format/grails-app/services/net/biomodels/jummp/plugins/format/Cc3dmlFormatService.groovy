/**
 * Copyright (C) 2010-2023 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
/**
 * <p>Individual class for handling detection and manipulation of CC3DML format.</p>
 * <br/>
 * <p>Based on built-in XML parsers, we simply look up the Simulation tag as the root node to guess the format.
 * Regarding the extraction of the title and description, it will be looked the tags which the texts are Title and
 * Details of the node texted Description respectively.</p>
 * <br/>
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *     <li><a href="mailto:nvntung@gmail.com">Tung Nguyen</a></li>
 * </ul>
 */
class Cc3dmlFormatService extends AbstractFormatDetectionService {
    static transactional = false

    Cc3dmlFormatService(CommonFormat expectedFormat = CommonFormat.CC3DML) {
        super(expectedFormat)
    }

    boolean areFilesThisFormat(List<File> files) {
        boolean result = files.any { File file ->
            hasRoot(file, "Simulation") && hasExt(file, "cc3d")
        }
        result
    }
}
