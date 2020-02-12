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
 * <p>Define properties of common formats</p>
 *
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *     <li><a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a></li>
 *     <li><a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glonț</a></li>
 * </ul>
 */
enum CommonFormat {
    C_CPP("C/C++", "C_CPP", C_CPP_MIME, "ccppFormatService"),
    JAVA("Java", "Java", JAVA_MIME),
    MATHEMATICA("Mathematica", "Mathematica", MATHEMATICA_MIME),
    MATLAB("MATLAB (Octave)", "matlab", DEFAULT_VERSIONS, MATLAB_MIME, "matlabFormatService", "matlab"),
    PYTHON("Python", "Python", PYTHON_VERSIONS, PYTHON_MIME),
    R("R", "R", DEFAULT_VERSIONS, R_MIME, "rlangFormatService", DEFAULT_CONTROLLER)

    public static final Set<String> C_CPP_MIME = ["text/x-csrc", "text/x-c++src"] as Set<String>
    public static final Set<String> JAVA_MIME = ["text/x-java-source"] as Set<String>
    public static final Set<String> MATHEMATICA_MIME = ["application/mathematica"] as Set<String>
    public static final Set<String> MATLAB_MIME = ["application/x-matlab", "application/matlab",
            "text/x-matlab", "text/matlab"] as Set<String>
    public static final Set<String> PYTHON_MIME = ["text/x-python"] as Set<String>
    public static final String[] PYTHON_VERSIONS = ['2.7', '3.6'] as String[]
    public static final Set<String> R_MIME = ["text/x-rsrc"] as Set<String>
    public static final String[] DEFAULT_VERSIONS = ['*'] as String[]
    public static final String DEFAULT_CONTROLLER = "commonFormat"

    private String name
    private String identifier
    private Set<String> acceptedMimeTypes
    private String[] versions
    private String service
    private String controller

    String getName() {
        this.name
    }

    String getIdentifier() {
        this.identifier
    }

    Set<String> getAcceptedMimeTypes() {
        this.acceptedMimeTypes
    }

    String[] getVersions() {
        return versions
    }

    String getService() {
        return service
    }

    String getController() {
        return controller
    }

    private CommonFormat(String name, String identifier, String[] versions,
            Set<String> acceptedMimeTypes, String service, String controller) {
        this.name = name
        this.identifier = identifier
        this.versions = versions
        this.acceptedMimeTypes = acceptedMimeTypes
        this.service = service
        this.controller = controller
    }

    private CommonFormat(String name, String identifier, Set<String> acceptedMimeTypes) {
        this(name, identifier, DEFAULT_VERSIONS, acceptedMimeTypes,
                defaultServiceNameForFormat(identifier), DEFAULT_CONTROLLER)
    }

    private CommonFormat(String name, String identifier, String[] versions,
            Set<String> acceptedMimeTypes) {
        this(name, identifier, versions, acceptedMimeTypes, defaultServiceNameForFormat(identifier),
                DEFAULT_CONTROLLER)
    }

    private CommonFormat(String name, String identifier, Set<String> acceptedMimeTypes,
            String service) {
        this(name, identifier, DEFAULT_VERSIONS, acceptedMimeTypes, service, DEFAULT_CONTROLLER)
    }

    private static String defaultServiceNameForFormat(String identifier) {
        String lower = identifier.toLowerCase()
        "${lower}FormatService"
    }
}
