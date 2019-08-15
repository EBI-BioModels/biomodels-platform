package net.biomodels.jummp.plugins.sbml.parameters

/**
 * @author carankalle on 13/08/2019.
 */
class BPComponentSpecies {

    String initialData
    String speciesAnnotationShow

    @Override
    public String toString() {
        return "BPComponentSpecies{" +
            "initialData='" + initialData + '\'' +
            ", speciesAnnotationShow='" + speciesAnnotationShow + '\'' +
            '}';
    }
}
