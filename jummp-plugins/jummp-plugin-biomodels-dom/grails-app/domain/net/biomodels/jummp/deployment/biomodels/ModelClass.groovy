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
 **/





package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.ModelClassStatus
import net.biomodels.jummp.plugins.security.User

/**
 * @short Domain class for storing the right class of a model.
 *
 *
 *
 * @author Vu Tu <tvu@ebi.ac.uk>
 */
class ModelClass {
    /**
     * The model which is being classified
     */
    Model model

    /**
     * The class name based on GO term, which is classified by hand
     */
    String className

    /**
     * Indicate whether the model was already trained or not
     */
    ModelClassStatus status

    /**
     * The user who mapped/assigned the class and model manually
     */
    User createdBy

    /**
     * The user who update this mapping
     */
    User updatedBy

    /**
     * The date time when the mapping is updated.
     */
    Date updatedDate

    /**
     * The date time when the mapping is created.
     */
    Date createdDate

    static constraints = {
        model(nullable: false)
        className(nullable: false)
        status(nullable: false)
        createdBy(nullable: false)
        updatedDate(nullable: false)
        createdDate(nullable: false)
        updatedBy(nullable: false)
    }

    static mapping = {
        className column: "class", sqlType: "varchar", length: 15
        createdBy column: "created_by"
        updatedBy column: "updated_by"
        status enumType:"ordinal"
    }
}
