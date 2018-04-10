/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.core.adapters.ModelAdapter

/**
 * Convenience class for adding customised methods to the CurationNotes domain class.
 *
 * Relies on Groovy categories, a form of runtime meta-programming.
 *
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Category(CurationNotes)
class CurationNotesCategory {

    /**
     * Provides a lightweight command object that can be used outside Jummp's core.
     *
     * @return the CurationNotesTransportCommand representation of a CurationNotes object.
     */
    CurationNotesTransportCommand toCommandObject() {
        new CurationNotesTransportCommand(
            model: new ModelAdapter(model: model).toCommandObject(),
            submitter: this.submitter, lastModifier: this.lastModifier,
            dateAdded: this.dateAdded, lastModified: this.lastModified,
            comment: this.comment, internalComment: this.internalComment,
            curationImage: this.curationImage)
    }
}
