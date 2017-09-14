/**
 * Created by tnguyen on 14/09/17.
 */
databaseChangeLog = {
    changeSet(author: "tnguyen (generated)", id: "1505386625932-6") {
        dropPrimaryKey(tableName: "publication_person")
    }

    changeSet(author: "tnguyen (generated)", id: "1505386625932-5") {
        addPrimaryKey(columnNames: "publication_id, person_id, position", constraintName: "publication_pPK", tableName: "publication_person")
    }

}
