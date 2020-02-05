/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.plugins.format

/**
 * <p>Define name of common formats</p>
 *
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *     <li><a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a></li>
 *     <li><a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glonț</a></li>
 * </ul>
 */
enum CommonFormat {
    C_CPP("C_CPP", ["text/x-csrc", "text/x-c++src"] as Set<String>),
    JAVA("Java", ["text/x-java-source"] as Set<String>),
    MATHEMATICA("Mathematica", ["application/mathematica"] as Set<String>),
    MATLAB("Matlab", ["application/x-matlab", "application/matlab", "text/x-matlab", "text/matlab"] as Set<String>),
    PYTHON("Python", ["text/x-python"] as Set<String>),
    R("R", ["text/x-rsrc"] as Set<String>)

    private String name
    private Set<String> acceptedMimeTypes

    String getName() {
        this.name
    }

    Set<String> getAcceptedMimeTypes() {
        this.acceptedMimeTypes
    }

    private CommonFormat(String name, Set<String> acceptedMimeTypes) {
        this.name = name
        this.acceptedMimeTypes = acceptedMimeTypes
    }
}
