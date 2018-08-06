databaseChangeLog = {
    changeSet(author: "mglont (generated)", id: "1449476671923-1") {
        dropNotNullConstraint(columnDataType: "varchar(255)", columnName: "uri", tableName: "resource_reference")
    }
}
