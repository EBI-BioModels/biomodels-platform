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





package net.biomodels.jummp.core

import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.search.SearchResponse
import net.biomodels.jummp.search.SortOrder

/**
 * Allow dynamically changing suitable search strategy based on configuration setting.
 *
 * Mihai Glonț <mihai.glont@ebi.ac.uk> on 30/08/2016.
 * Tung Nguyen <tung.nguyen@ebi.ac.uk> on 30/08/2016.
 */

@CompileStatic
interface ModelSearchStrategy {
    void clearIndex()
    void clearIndex(RevisionTransportCommand revision)
    SearchResponse searchModels(String query, String domain, SortOrder sortOrder,
                                Map<String, Integer> paginationCriteria)
    /**
     * Re-indexes all annotations or model-level annotations of a given revision.
     * Some model revisions are coded a massive amount of annotations but there are some invalid URIs.
     * Those URIs break the indexing pipeline, so we only have to index the model-level annotations.
     *
     * @param revision A Revision instance
     * @param level either 'full' if indexing the full set of annotations or 'bare' for only model-level ones
     */
    void updateIndex(RevisionTransportCommand revision, String level)
    String[] getSortFields()
    Map checkIndexedData()
    void indexDB()
}
