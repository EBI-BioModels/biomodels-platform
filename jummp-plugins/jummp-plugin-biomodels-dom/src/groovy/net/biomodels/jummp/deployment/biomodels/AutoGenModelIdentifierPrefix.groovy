package net.biomodels.jummp.deployment.biomodels

enum AutoGenModelIdentifierPrefix {
    P2M("BMID"),
    PDGSMM("MODEL170711")

    private String prefix

    String getPrefix() {
        return prefix
    }
    AutoGenModelIdentifierPrefix(String prefix) {
        this.prefix = prefix
    }
}
