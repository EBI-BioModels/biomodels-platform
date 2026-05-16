import net.biomodels.jummp.plugins.security.AuthToken

databaseChangeLog = {
    changeSet(author: "Tung Nguyen (generated)", id: "1718631186225-1") {
        addColumn(tableName: "auth_token") {
            column(name: "expired_date", type: "datetime") {
                constraints(nullable: "true")
            }
        }
    }
}
