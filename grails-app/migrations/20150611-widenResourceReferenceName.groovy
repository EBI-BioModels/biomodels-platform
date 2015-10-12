databaseChangeLog = {
    changeSet(author: "Mihai Glont", id: "20150611-1") {
        modifyDataType(tableName: "resource_reference", columnName: "name", newDataType: "varchar(255)")
    }
}

