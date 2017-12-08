databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "populate default value for the empty description") {
        grailsChange {
            change {
                def repoFiles = sql.dataSet("repository_file")
                repoFiles.rows().each { r ->
                    boolean isEmpty = !r.description
                    if (isEmpty) {
                        sql.executeUpdate "update repository_file set description = ${r.path} where id = ${r.id}"
                        println "update repository_file set description = ${r.path} where id = ${r.id}"
                    } else {
                        println "No need to set $r.path for $r.description"
                    }
                }
            }
        }
    }
	changeSet(author: "Tung Nguyen", id: "1511544481575-3") {
		addNotNullConstraint(columnDataType: "varchar(1500)", columnName: "description", tableName: "repository_file")
	}
}
