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

import net.biomodels.jummp.annotationstore.Qualifier

/**
 * Convenience class for adding methods to the Qualifier class.
 *
 * Relies on Groovy categories, a form of runtime metaprogramming.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@Category(Qualifier)
class QualifierCategory {
    /**
     * Provides a lightweight command object that can be used outside Jummp's core.
     *
     * @return the QualifierTransportCommand representation of a Qualifier object.
     */
    public QualifierTransportCommand toCommandObject() {
        String ns = this.namespace
        String accession = this.accession
        String uri = this.uri
        String type = this.qualifierType
        return new QualifierTransportCommand(namespace: ns, accession: accession, uri: uri,
                type: type)
    }
}
