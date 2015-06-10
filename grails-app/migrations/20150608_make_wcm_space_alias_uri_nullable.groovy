databaseChangeLog = {

    changeSet(author: "mglont (generated)", id: "1433776637034-1") {
        dropNotNullConstraint(columnDataType: "varchar(80)", columnName: "aliasuri", tableName: "wcm_space")
    }
}
