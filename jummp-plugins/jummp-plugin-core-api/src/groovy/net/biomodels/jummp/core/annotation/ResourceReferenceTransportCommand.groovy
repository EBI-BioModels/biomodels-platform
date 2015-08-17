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
 */

package net.biomodels.jummp.core.annotation

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Lightweight representation of the ResourceReference domain class.
 *
 * Instances of this class are immutable.
 *
 * @see net.biomodels.jummp.annotationstore.ResourceReference
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@Immutable
class ResourceReferenceTransportCommand implements Serializable {
    static final long serialVersionUID = 1L
    String datatype
    String collectionName
    String name
    String uri
    String accession
    String shortName
    String description
}

