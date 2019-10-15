databaseChangeLog = {

    changeSet(author: "tnguyen (generated)", id: "1571139562570-1") {
        addColumn(tableName: "model") {
            column(name: "other_info", type: "varchar(255)")
        }
    }
}
