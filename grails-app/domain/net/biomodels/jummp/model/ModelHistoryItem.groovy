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





package net.biomodels.jummp.model

import net.biomodels.jummp.plugins.security.User

/**
 * @short Domain class for storing when a user accessed a Model.
 *
 * A history item consists of a Model, the user who accessed it and
 * when it was last accessed.
 *
 * Do not use this domain class directly, but the ModelHistoryService.
 * This service ensures that the History is correct.
 *
 * @author Martin Graesslin <m.graesslin@dkfz.de>
 * @see net.biomodels.jummp.core.ModelHistoryService
 */
class ModelHistoryItem implements Serializable {
    /**
     * The Model the user accessed
     */
    Model model
    /**
     * The User who accessed the Model
     */
    User user
    /**
     * The Timestamp of last access to this history element
     */
    long lastAccessedDate

    static mapping = {
        id(composite:['model', 'user'])
        // disable the version field as not needed in this domain class
        version false
    }

    /**
     * Updates the last accessed date of this history item.
     */
    public void touch() {
        this.lastAccessedDate = System.currentTimeMillis()
        save()
    }

    /**
     * Factory method to ensure that the history item is setup correctly.
     * Always use this method instead of the constructor.
     * @param model The Model which has to be in the history
     * @param user The User who accessed the Model
     * @return A history item for the Model and User
     */
    static ModelHistoryItem create(Model model, User user) {
        ModelHistoryItem item = new ModelHistoryItem()
        item.model = model
        item.user = user
        item.lastAccessedDate = System.currentTimeMillis()
        return item
    }
}
