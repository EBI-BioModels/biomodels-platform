import net.biomodels.jummp.model.ModellingApproach

databaseChangeLog = {
    changeSet(author: "tnguyen (customised)", id: "1571138572-1") {
        grailsChange {
            change {
                def accession =  "OTHER"
                def label = "Other"
                def iri = "Unknown"
                ModellingApproach approach = ModellingApproach.findOrSaveWhere([accession: accession,
                                                                                name: label,
                                                                                resource: iri])
                if (approach) {
                    println "Modelling approach 'Other' was saved successfully"
                } else {
                    println "Cannot create the modelling approach 'Other'"
                }
            }
        }
    }
}
