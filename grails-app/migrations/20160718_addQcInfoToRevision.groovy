/**
 * Created by tnguyen on 18/07/16.
 */
databaseChangeLog = {
    changeSet(author: "tung (generated)", id: "addQcInfoToRevision_20160718_172011") {
        addColumn(tableName: "revision") {
            column(name: "qc_info_id", type: "bigint") {
                constraints(nullable: "true")
            }
        }
    }
}
