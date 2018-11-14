databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "change utf8_unicode_ci to utf8mb4_unicode_ci") {
        modifyDataType(tableName: "repository_file", columnName: "description",
            newDataType: "varchar(1500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ")
    }

    changeSet(author: "Tung Nguyen", id: "update the description of the model files") {
        grailsChange {
            change {
                def repoFiles = sql.dataSet("repository_file")
                repoFiles.rows().each { rf ->
                    if (rf.main_file) {
                        def revId = rf.revision_id
                        def revision = sql.firstRow("select * from revision where id = ${revId}")
                        if (revision) {
                            def format = sql.firstRow("select * from model_format where id = ${revision.format_id}")
                            def formatName = format?.name
                            def formatVersion = format?.format_version
                            def escapedRevisionName = revision.name
                            String newDesc = "${formatName} ${formatVersion} representation of ${escapedRevisionName}"
                            String command = "update repository_file set description = ? where id = ?"
                            sql.executeUpdate(command, [newDesc.replace('\'','\\') as String, rf.id])
                            println "update repository_file set description = '${newDesc}' where id = ${rf.id}"
                        }
                    } else {
                        println "No need to update description for the file ${rf.path} of the revision ${rf.revision_id}"
                    }
                }
            }
        }
    }
}
