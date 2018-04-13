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

import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.plugins.security.User

/**
 * @short   Data transfer object (DTO) for CurationNotes domain class.
 *
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@grails.validation.Validateable
class CurationNotesTransportCommand implements Serializable {
    private static final long serialVersionUID = 1L
    Long id
    ModelTransportCommand model
    User submitter
    User lastModifier
    Date dateAdded
    Date lastModified
    String comment
    String internalComment
    byte[] curationImage
    boolean updated

    static constraints = {
        id nullable: true
        internalComment nullable: true, blank: true
        curationImage(nullable: false, maxSize: 5242880) // max of 5MB file
        //importFrom([exclude: 'id'], CurationNotes)
    }
}
