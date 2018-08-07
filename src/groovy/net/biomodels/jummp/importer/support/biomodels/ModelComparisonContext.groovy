/*
 * Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.importer.support.biomodels

import groovy.transform.CompileStatic
import net.biomodels.jummp.model.Revision

/**
 * This class is used to create a wrapper which is capturing the key attributes for the model change detector.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 *
 * Created by Mihai Glonț on 08/02/18.
 * Updated by Tung nguyen on 04/06/18.
 */
@CompileStatic
class ModelComparisonContext {
    String name
    String description
    String originalModel
    String biomodelsId
    Date lastModified
    String pathToCurationImage
    String curationComment
    Revision imported
}
