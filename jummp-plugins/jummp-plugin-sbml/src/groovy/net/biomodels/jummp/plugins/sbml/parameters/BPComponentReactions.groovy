package net.biomodels.jummp.plugins.sbml.parameters

/**
 * @author carankalle on 13/08/2019.
 */
class BPComponentReactions implements Serializable {

    String reaction
    String rate
    String parameters

    @Override
    public String toString() {
        return "BPComponentReactions{" +
            "reaction='" + reaction + '\'' +
            ", rate='" + rate + '\'' +
            ", parameters='" + parameters + '\'' +
            '}';
    }
}
