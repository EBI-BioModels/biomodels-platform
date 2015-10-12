databaseChangeLog = {

    changeSet(author: "raza", id: "1633776637124-1") {
        dropNotNullConstraint(columnDataType: "varchar(80)", columnName: "name", tableName: "resource_reference")
    }
    
    changeSet(author: "raza", id: "1633776637124-2") {
        dropNotNullConstraint(columnDataType: "varchar(80)", columnName: "accession", tableName: "resource_reference")
    }
}