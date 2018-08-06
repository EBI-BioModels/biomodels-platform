databaseChangeLog = {
    changeSet(author: "Mihai Glont", id: "20150820-1") {
        modifyDataType(tableName: "curation_notes", columnName: "comment", newDataType: "mediumtext")
    }
}
