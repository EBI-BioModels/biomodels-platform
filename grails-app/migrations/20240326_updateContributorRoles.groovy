import net.biomodels.jummp.model.ContributionRole as CR

databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "1711473748-1") {
        grailsChange {
            change {
                List newRoleList = [
                    ["name": "Curator", "description": "Any person who has contributed to the implementation of the model code and annotations"],
                    ["name": "Modeller", "description": "Any person who has made a significant contribution to model submission"],
                    ["name": "Submitter", "description": "Any person who has made the first version of your submission"],
                    ["name": "Code Curator", "description": "Any person who has made the model code"],
                    ["name": "Annotation Curator", "description": "Any person who has created annotations and metadata"],
                    ["name": "Other", "description": "Any person who has made a slightly significant contribution to your work"]
                ]

                newRoleList.each {
                    CR cr = CR.findOrCreateWhere([name: it.get("name")])
                    cr.description = it.get("description")
                    if (cr.save(flush: true)) {
                        println "The contributor role  [${it.get('name')}: ${it.get('description')}] was created/updated successfully."
                    } else {
                        println "Cannot create the contributor role [${it.get('name')}: ${it.get('description')}]."
                    }
                }
            }
        }
    }
}
