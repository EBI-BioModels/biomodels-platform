databaseChangeLog = {
    changeSet(author: "mglont", id: "improve-rdf-file-description-20160510") {
        grailsChange {
            def descriptionProvider = ctx.getBean("rdfMetadataWriter")
            final String CURRENT_DESCRIPTION = "annotation file"
            final String NEW_DESCRIPTION = descriptionProvider.defaultRdfFileDescription
            // select all rdf files, whether user-submitted or auto-generated
            final String selectQuery =
                    "select * from repository_file where main_file = false and path like '%.rdf'"
            final String updateQuery =
                    "update repository_file set description = ? where id = ?"
            change {
                def table = sql.dataSet("repository_file")
                table.rows(selectQuery).each { rf ->
                    // only update description if we have the current description,
                    // or if a user has uploaded the file and left out the description
                    boolean shouldUpdate = CURRENT_DESCRIPTION == rf.description ||
                            (!(rf.description) && rf.user_submitted)
                    if (shouldUpdate) {
                        println "Updating description ${rf.id} for repository file ${rf.id}"
                        sql.executeUpdate(updateQuery,  [NEW_DESCRIPTION, rf.id])
                    }
                }
                println "Finished updating the auto-generated description for RDF files."
            }
        }
    }
}
