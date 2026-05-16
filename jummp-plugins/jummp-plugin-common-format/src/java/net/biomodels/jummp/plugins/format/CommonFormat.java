package net.biomodels.jummp.plugins.format;

import java.util.*;
import java.util.stream.Stream;

import static net.biomodels.jummp.plugins.format.CommonFormat.Constants.*;

/**
 * <p>Define properties of common formats</p>
 *
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 * <li><a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a></li>
 * </ul>
 */
public enum CommonFormat {
    C_CPP("C/C++", "C_CPP", C_CPP_MIME, "ccppFormatService"),
    CC3DML("CompuCell3D", "CC3DML", CC3DML_MIME, "cc3dmlFormatService"),
    JAVA("Java", "Java", JAVA_MIME),
    MATHEMATICA("Mathematica", "Mathematica", MATHEMATICA_MIME),
    MATLAB("MATLAB (Octave)", "matlab", DEFAULT_VERSIONS, MATLAB_MIME, "matlabFormatService", "matlab"),
    MORPHEUSML("MorpheusML", "MorpheusML", DEFAULT_VERSIONS, MORPHEUSML_MIME, "morpheusMLFormatService", DEFAULT_CONTROLLER),
    ONNX("Open Neural Network Exchange", "ONNX", DEFAULT_VERSIONS, ONNX_MIME, "onnxFormatService", DEFAULT_CONTROLLER),
    BMS("BioModels Metadata Submission", "BMS", DEFAULT_VERSIONS, BMS_MIME, "bmsFormatService", DEFAULT_CONTROLLER),
    PYTHON("Python", "Python", Stream.concat(Arrays.stream(DEFAULT_VERSIONS), Arrays.stream(PYTHON_VERSIONS)).toArray(String[]::new), PYTHON_MIME),
    R("R", "R", DEFAULT_VERSIONS, R_MIME, "rlangFormatService", DEFAULT_CONTROLLER);

    public String getName() {
        return this.name;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public Set<String> getAcceptedMimeTypes() {
        return this.acceptedMimeTypes;
    }

    public String[] getVersions() {
        return versions;
    }

    public String getService() {
        return service;
    }

    public String getController() {
        return controller;
    }

    public static CommonFormat fromIdentifier(final String id) {
        return Arrays.stream(values())
            .filter(f -> f.identifier.equals(id))
            .findAny()
            .orElse(null);
    }

    public static boolean hasDefaultVersion(String id) {
        CommonFormat self = fromIdentifier(id);
        if (null == self) return false;
        return Arrays.equals(self.versions, DEFAULT_VERSIONS);
    }

    CommonFormat(String name, String identifier, String[] versions, Set<String> acceptedMimeTypes, String service, String controller) {
        this.name = name;
        this.identifier = identifier;
        this.versions = versions;
        this.acceptedMimeTypes = acceptedMimeTypes;
        this.service = service;
        this.controller = controller;
    }

    CommonFormat(String name, String identifier, Set<String> acceptedMimeTypes) {
        this(name, identifier, DEFAULT_VERSIONS, acceptedMimeTypes, defaultServiceNameForFormat(identifier), DEFAULT_CONTROLLER);
    }

    CommonFormat(String name, String identifier, String[] versions, Set<String> acceptedMimeTypes) {
        this(name, identifier, versions, acceptedMimeTypes, defaultServiceNameForFormat(identifier), DEFAULT_CONTROLLER);
    }

    CommonFormat(String name, String identifier, Set<String> acceptedMimeTypes, String service) {
        this(name, identifier, DEFAULT_VERSIONS, acceptedMimeTypes, service, DEFAULT_CONTROLLER);
    }

    private static String defaultServiceNameForFormat(String identifier) {
        final String lower = identifier.toLowerCase();
        return lower + "FormatService";
    }

    private String name;
    private String identifier;
    private Set<String> acceptedMimeTypes;
    private String[] versions;
    private String service;
    private String controller;

    public static final class Constants {
        public static final Set<String> C_CPP_MIME = new LinkedHashSet<>(
                Arrays.asList("text/x-csrc", "text/x-c++src"));
        public static final Set<String> JAVA_MIME = new LinkedHashSet<>(
                Collections.singletonList("text/x-java-source"));
        public static final Set<String> MATHEMATICA_MIME = new LinkedHashSet<>(
                Collections.singletonList("application/mathematica"));
        public static final Set<String> MATLAB_MIME = new LinkedHashSet<>(Arrays.asList(
                "application/x-matlab", "application/matlab", "text/x-matlab", "text/matlab"));
        public static final Set<String> PYTHON_MIME = new LinkedHashSet<>(
                Collections.singletonList("text/x-python"));
        public static final Set<String> MORPHEUSML_MIME = new LinkedHashSet<>(
                Collections.singletonList("application/xml"));
        public static final Set<String> ONNX_MIME = new LinkedHashSet<>(
                Collections.singletonList("text/onnx"));

        public static final Set<String> BMS_MIME = new LinkedHashSet<>(
                Collections.singletonList("text/bms"));
        public static final Set<String> CC3DML_MIME = new LinkedHashSet<>(
                Collections.singletonList("application/xml"));
        public static final Set<String> R_MIME = new LinkedHashSet<>(
                Collections.singletonList("text/x-rsrc"));
        public static final String[] DEFAULT_VERSIONS = new String[]{"*"};
        public static final String[] PYTHON_VERSIONS = new String[]{"2.7", "3.6"};
        public static final String DEFAULT_CONTROLLER = "commonFormat";

    }
}
