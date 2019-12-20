package net.biomodels.jummp.plugins.sbml.parameters

/**
 * @author carankalle on 13/08/2019.
 */
class BPComponentReactions {

    String reactionShow
    String rateShow
    String parameters

    @Override
    public String toString() {
        return "BPComponentReactions{" +
            "reactionShow='" + reactionShow + '\'' +
            ", rateShow='" + rateShow + '\'' +
            ", parameters='" + parameters + '\'' +
            '}';
    }
}
