/**
 * Copyright (C) 2010-2026 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

/**
 * @short Thrown when the search server cannot answer, e.g. because it timed out or is down.
 *
 * It is an exception, and not an empty SearchResponse, so that the cache on
 * OmicsdiBasedSearch.searchModels does not keep the failure and serve it for as long as its entries
 * live.
 */
class SearchUnavailableException extends RuntimeException {
    SearchUnavailableException(String message, Throwable cause) {
        super(message, cause)
    }
}
