databaseChangeLog = {

    changeSet(author: "tnguyen (generated)", id: "1715614742991-1") {
        addColumn(tableName: "model_of_the_month") {
            column(name: "published_from", type: "datetime")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1715614742991-2") {
        addColumn(tableName: "model_of_the_month") {
            column(name: "published_until", type: "datetime")
        }
    }
}
