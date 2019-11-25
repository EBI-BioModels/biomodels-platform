import groovy.json.JsonSlurper
import net.biomodels.jummp.model.ModellingApproach

databaseChangeLog = {
    changeSet(author: "tnguyen (customised)", id: "1570725855-1") {
        grailsChange {
            change {

                // we set the size as 100 because the actual total elements are 73.
                String requestString = "https://www.ebi.ac.uk/ols/api/ontologies/mamo/terms/http%253A%252F%252Fidentifiers" +
                    ".org%252Fmamo%252FMAMO_0000037/descendants?size=100"
                URL apiUrl = new URL(requestString)
                def terms = new JsonSlurper().parseText(apiUrl.text)
                def nbRecords = terms["_embedded"]["terms"].results.size()
                (0..(nbRecords - 1)).each { int index ->
                    def label = terms["_embedded"]["terms"][index]["label"]
                    def iri = terms["_embedded"]["terms"][index]["iri"]
                    def short_form = terms["_embedded"]["terms"][index]["short_form"]
                    ModellingApproach approach = ModellingApproach.findOrSaveWhere([accession: short_form, name: label, resource: iri])
                    if (approach) {
                        println "successfully saved ${approach.dump()}"
                    }
                    // clear session and save records after every 50 entries created
                    if (index % 50 == 0) {
                        ModellingApproach.withSession { session ->
                            session.flush()
                            session.clear()
                        }
                    }
                }
                // clear session and save last records
                ModellingApproach.withSession { session ->
                    session.flush()
                    session.clear()
                }
            }
        }
    }
}
