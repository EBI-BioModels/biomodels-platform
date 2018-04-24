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
     * Which model was accessed
     */
    Model model

    /**
     * What is the class of this model
     */
    String className

    /**
     * Indicate whether the model trained or not
     */
    ModelClassStatus status

    /**
     * Who create this mapping
     */
    User createBy

    /**
     * When it changed
     */
    Date updateTime

    /**
     * When it created
     */
    Date createTime

    static constraints = {
        model(nullable: false)
        className(nullable: false)
        status(nullable: false)
        createBy(nullable: false)
        updateTime(nullable: false)
        createTime(nullable: false)
    }

    static mapping = {
        className column: "class", sqlType: "varchar", length: 15
        createBy column: "create_by"
        status enumType:"ordinal"
    }
}
