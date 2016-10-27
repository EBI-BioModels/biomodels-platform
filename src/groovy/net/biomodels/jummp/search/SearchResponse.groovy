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
 **/





package net.biomodels.jummp.search

import net.biomodels.jummp.core.model.ModelTransportCommand
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
/**
 * This class provides means of dealing with searching related results.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date   27/10/2016
 */
class SearchResponse {
    // the set of models/entries matching what are looking for
    HashSet<ModelTransportCommand> results = new HashSet<ModelTransportCommand>()
    // the set of facets what the models/entries should belong in
    HashSet<Facet> facets = new HashSet<Facet>()
    // the number of total results
    long totalCount = 0
}
