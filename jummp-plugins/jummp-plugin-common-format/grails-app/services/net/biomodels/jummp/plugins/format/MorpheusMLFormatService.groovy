/**
 * Copyright (C) 2010-2021 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
import net.biomodels.jummp.plugins.format.utils.XmlBasedHandler

/**
 * <p>Individual class for handling detection and manipulation of MorpheusML format.</p>
 * <br/>
 * <p>Based on built-in XML parsers, we simply look up the MorpheusModel tag as the root node to guess the format.
 * Regarding the extraction of the title and description, it will be looked the tags which the texts are Title and
 * Details of the node texted Description respectively.</p>
 * <br/>
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *     <li><a href="mailto:nvntung@gmail.com">Tung Nguyen</a></li>
 * </ul>
 */
class MorpheusMLFormatService extends AbstractFormatDetectionService {
    static transactional = false

    MorpheusMLFormatService() {
        super(CommonFormat.MORPHEUSML)
    }

    boolean areFilesThisFormat(List<File> files) {
        boolean result = files.any { File file ->
            hasMorpheusMLTag(file)
        }
        result
    }

    String extractDescription(final List<File> model) {
        // Presumably the submission has a single main file
        File mainFile = model.first()
        def parsedDoc = new XmlSlurper().parse(mainFile)
        def descNode = parsedDoc.childNodes().find { "description" == it.name().toLowerCase() }
        def details = descNode.children.find { "details" == it.name().toLowerCase() }
        String description = details.text()
        description
    }

    String extractName(final List<File> model) {
        // Presumably the submission has a single main file
        File mainFile = model.first()
        def parsedDoc = new XmlSlurper().parse(mainFile)
        def descNode = parsedDoc.childNodes().find { "description" == it.name().toLowerCase() }
        def title = descNode.children.find { "title" == it.name().toLowerCase() }
        String name = title.text()
        name
    }

    private boolean hasMorpheusMLTag(final File modelFile) {
        if (!hasExt(modelFile, "XML")) {
            return false
        }
        boolean hasMLRoot = hasRoot(modelFile, "morpheusmodel")
        hasMLRoot
    }
}
