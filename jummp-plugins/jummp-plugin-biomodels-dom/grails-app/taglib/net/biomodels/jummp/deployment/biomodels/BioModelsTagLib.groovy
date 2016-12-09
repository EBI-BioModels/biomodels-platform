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

/**
 * @short General purpose helper for rendering BioModels pages.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class BioModelsTagLib {
    static defaultEncodeAs = [taglib:'html']
    static namespace = 'biomd'

    /**
     * Dependency injection
     */
    def modelOfTheMonthService

    /**
     * Displays the Model of the Month (MoM) entry for the given model.
     *
     * @attr modelId REQUIRED the id of the model for which to render
     * the MoM entry.
     */
    def renderModelOfMonth = { attrs ->
        Long id = attrs.modelId
        if (!id) {
            return
        }
        def entries = modelOfTheMonthService.fetchEntriesForModel id
        out << render(collection: entries, template: '/templates/modelOfTheMonth',
                plugin: 'jummp-plugin-biomodels-dom')
        // calls momService.fetchEntriesForModel for given modelId
        // delegates rendering to dedicated template
    }
}
