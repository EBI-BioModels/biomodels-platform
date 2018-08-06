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

import net.biomodels.jummp.annotationstore.ResourceReference

/**
 * Convenience class for adding methods to the ResourceReference class.
 *
 * Relies on Groovy categories, a form of runtime metaprogramming.
 *
 * @see net.biomodels.jummp.annotationstore.ResourceReference
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@Category(ResourceReference)
class ResourceReferenceCategory {
    /**
     * Returns a lightweight representation of an instance of ResourceReference.
     *
     * @return a ResourceReferenceTransportCommand that can be used outside of Jummp's core.
     */
    ResourceReferenceTransportCommand toCommandObject() {
        return new ResourceReferenceTransportCommand(datatype: this.datatype, name: this.name,
                uri: this.uri, accession: this.accession, shortName: this.shortName,
                description: this.description, collectionName: this.collectionName)
    }
}
