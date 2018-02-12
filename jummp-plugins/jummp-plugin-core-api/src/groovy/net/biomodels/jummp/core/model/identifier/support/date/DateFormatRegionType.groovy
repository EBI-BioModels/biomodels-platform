/**
 * Copyright (C) 2010-2017 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core.model.identifier.support.date

/**
 * Enum for the types of regions there can be in the date format used by a model identifier scheme.
 *
 * @see DateFormatRegion
 * @see net.biomodels.jummp.core.model.identifier.support.DateModelIdentifierPartition
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
enum DateFormatRegionType {
    y,       // year
    M,       // month -- can be a number or a word
    W, w,    // week
    D, d, F, // day
    H, k, h, // hour
    m,       // minute
    S, s,    // second
    MISC     // any other type of character

    static DateFormatRegionType from(char c) {
        switch (c) {
            case 'y': return y
            case 'M': return M
            case 'W': return W
            case 'w': return w
            case 'D': return D
            case 'd': return d
            case 'F': return F
            case 'H': return H
            case 'k': return k
            case 'h': return h
            case 'm': return m
            case 'S': return S
            case 's': return s
            default: return MISC
        }
    }
}
