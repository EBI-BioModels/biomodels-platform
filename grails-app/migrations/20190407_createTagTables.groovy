databaseChangeLog = {

    changeSet(author: "tnguyen (generated)", id: "1554676746196-1") {
        createTable(tableName: "model_tag") {
            column(name: "model_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "tag_id", type: "bigint") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1554676746196-2") {
        createTable(tableName: "tag") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "tagPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "user_created_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "datetime") {
                constraints(nullable: "false")
            }

            column(name: "date_modified", type: "datetime") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1554676746196-4") {
        addPrimaryKey(columnNames: "model_id, tag_id", constraintName: "model_tagPK", tableName: "model_tag")
    }

    changeSet(author: "tnguyen (generated)", id: "1554676746196-14") {
        createIndex(indexName: "FK_jper6vb1lbvb4mm7qa2u1cv5y", tableName: "model_tag") {
            column(name: "model_id")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1554676746196-15") {
        createIndex(indexName: "FK_kq99ay64016cuo3dx45n3fisw", tableName: "model_tag") {
            column(name: "tag_id")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1554676746196-16") {
        createIndex(indexName: "FK_1ct0hcjofs11entijc1u322im", tableName: "tag") {
            column(name: "user_created_id")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1554676746196-17") {
        createIndex(indexName: "name_uniq_1554676744907", tableName: "tag", unique: "true") {
            column(name: "name")
        }
    }


    changeSet(author: "tnguyen (generated)", id: "1554676746196-5") {
        addForeignKeyConstraint(baseColumnNames: "model_id", baseTableName: "model_tag", constraintName: "FK_jper6vb1lbvb4mm7qa2u1cv5y", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "model", referencesUniqueColumn: "false")
    }

    changeSet(author: "tnguyen (generated)", id: "1554676746196-6") {
        addForeignKeyConstraint(baseColumnNames: "tag_id", baseTableName: "model_tag", constraintName: "FK_kq99ay64016cuo3dx45n3fisw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "tag", referencesUniqueColumn: "false")
    }

    changeSet(author: "tnguyen (generated)", id: "1554676746196-7") {
        addForeignKeyConstraint(baseColumnNames: "user_created_id", baseTableName: "tag", constraintName: "FK_1ct0hcjofs11entijc1u322im", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
    }
}
