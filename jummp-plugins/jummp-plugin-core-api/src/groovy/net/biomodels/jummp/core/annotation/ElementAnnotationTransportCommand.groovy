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
import net.biomodels.jummp.core.model.RevisionTransportCommand

/**
 * Lightweight representation of the ElementAnnotation domain class.
 *
 * Instances of this class are immutable. Unlike the domain class, there is no reference
 * to the associated Revision. This is because the class
 * {@link net.biomodels.jummp.core.model.RevisionTransportCommand}, which is the basis for
 * submission, retrieval, search and display, already has a member that is an instance of
 * the ElementAnnotationTransportCommand class.
 *
 * @see net.biomodels.jummp.annotationstore.ElementAnnotation
 * @see net.biomodels.jummp.core.model.RevisionTransportCommand
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@CompileStatic
@Immutable
class ElementAnnotationTransportCommand implements Serializable {
    String creator
    StatementTransportCommand statement
}
