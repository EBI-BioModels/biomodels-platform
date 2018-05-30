databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "20170720104405-1") {
        modifyDataType(tableName: "repository_file", columnName: "description",
            newDataType: "varchar(1500)")
    }
}
