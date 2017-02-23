databaseChangeLog = {
    changeSet(author: "mihai glont", id: "20170223-1") {
        modifyDataType(tableName: 'curation_notes', columnName: "comment", newDataType: 'mediumtext')
    }
}
