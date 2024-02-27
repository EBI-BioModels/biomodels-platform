/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 * <p style="font-weight: bold">Description:</p>
 * <p>Individual class for handling the detection and manipulation of
 * BioModels Metadata Submission (BMS) format.</p>
 * <br/>
 * <p>Currently, we only give a minimal support for this format.</p>
 * <br/>
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *     <li><a href="mailto:nvntung@gmail.com">Tung Nguyen</a></li>
 * </ul>
 */
class BmsFormatService extends AbstractFormatDetectionService {
    static transactional = false

    BmsFormatService(CommonFormat expectedFormat = CommonFormat.ONNX) {
        super(expectedFormat)
    }

    boolean areFilesThisFormat(List<File> files) {
        boolean result = files.any { File file ->
            hasExt(file, "bms")
        }
        result
    }
}
