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

/**
 * This class provides means of sorting searching results.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date   12/06/2017
 */
class SortOrder {
    String field

    enum SortDirection {ASC, DESC}

    SortDirection direction

    SortOrder() {
        this.field = "" // "relevance" by default
        this.direction = SortDirection.DESC
    }

    SortOrder(String field, SortDirection direction) {
        this.field = field.equalsIgnoreCase("relevance") ? "" : field
        this.direction = direction
    }

    SortOrder(String field, String direction) {
        if (field) {
            this.field = field.equalsIgnoreCase("relevance") ? "" : field
        } else {
            this.field = ""
        }
        if (direction) {
            SortDirection sortDirection = direction.equalsIgnoreCase("asc") ? SortDirection.ASC : SortDirection.DESC
            this.direction = sortDirection
        } else
            this.direction = SortDirection.DESC
    }
}
