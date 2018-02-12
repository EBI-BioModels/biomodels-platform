databaseChangeLog = {

    changeSet(author: "mihai glont", id: "1234") {
        modifyDataType(tableName: 'resource_reference_synonyms', columnName: "synonyms_string", newDataType: 'mediumtext')
    }
}
