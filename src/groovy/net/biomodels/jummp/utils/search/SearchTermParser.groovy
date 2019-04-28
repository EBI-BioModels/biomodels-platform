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

package net.biomodels.jummp.utils.search

/**
 * This utility class is used for extracting precise searching keywords or
 * facets included in the query. By extracting keywords, we are able to know
 * the specific searching words that users want to search for beside the selected facets.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class SearchTermParser {
    private final static Set<String> LOGICAL_OPS = ["AND", "OR", "NOT"] as Set
    String query

    List<String> findFacets() {
        List<String> words = query.split("\\s+")
        return words.findAll { isFacet(it) }
    }

    String extractSearchTerm() {
        String regex = '( *)((AND|OR|NOT)( ))?([a-z_A-Z0-9]*):("\\w+.*"|\\w+(:?\\w+))'
        String searchAll = "*:*"
        if (query == searchAll) {
            return query
        } else {
            boolean isDefaultSearch = query.contains("*:*")
            // remove *:* for processing the facets
            if (isDefaultSearch) {
                query = query.replaceAll("\\*:\\*(\\s)?(AND|OR|NOT)?", '')
            }
            // remove facets
            String result = query.replaceAll(regex, '')
            result = result.trim()
            // remove spare spaces
            result = result.trim().replaceAll(" +", " ")
            // get *:* back to the query
            if (isDefaultSearch && !result.isEmpty()) {
                result = "*:* AND $result"
            } else if (isDefaultSearch) {
                result = searchAll
            }

            return result
        }
    }

    private boolean isFacet(String string) {
        /**
         * A facet is often considered a couple of facet id and value.
         * For example:
         * curationstatus:"Manually curated"
         * curationstatus:"Non-curated"
         * TAXONOMY:40674
         *
         * As seen here, a facet is a word consisting of two parts. The prefix is called
         * facet identifier while the rest represents the value of that facet.
         */
        if (!string)
            return false
        else {
            string.split(":").size() == 2
        }
    }
}
