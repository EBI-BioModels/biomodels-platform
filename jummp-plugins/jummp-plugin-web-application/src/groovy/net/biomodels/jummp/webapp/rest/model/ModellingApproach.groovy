package net.biomodels.jummp.webapp.rest.model

class ModellingApproach {
    String accession
    String name
    String resource

    ModellingApproach(net.biomodels.jummp.model.ModellingApproach approach) {
        accession = approach.accession
        name = approach.name
        resource = approach.resource
    }
}
