databaseChangeLog = {
    changeSet(author: "mglont (generated)", id: "1433757856028-1") {
        createTable(tableName: "element_annotation") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "element_annotPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "creator_id", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "revision_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "statement_id", type: "bigint") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-2") {
        createTable(tableName: "qualifier") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "qualifierPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "accession", type: "varchar(255)")

            column(name: "namespace", type: "varchar(255)")

            column(name: "qualifier_type", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "uri", type: "varchar(255)")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-6") {
        createTable(tableName: "statement") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "statementPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "object_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "qualifier_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "subject_id", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-29") {
        createIndex(indexName: "FKCFAB2C52265115F5", tableName: "element_annotation") {
            column(name: "revision_id")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-30") {
        createIndex(indexName: "FKCFAB2C5253C160C8", tableName: "element_annotation") {
            column(name: "statement_id")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-34") {
        createIndex(indexName: "FK83B7296F9C92D2E8", tableName: "statement") {
            column(name: "qualifier_id")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-19") {
        addForeignKeyConstraint(baseColumnNames: "revision_id", baseTableName: "element_annotation", constraintName: "FKCFAB2C52265115F5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "revision", referencesUniqueColumn: "false")
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-20") {
        addForeignKeyConstraint(baseColumnNames: "statement_id", baseTableName: "element_annotation", constraintName: "FKCFAB2C5253C160C8", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "statement", referencesUniqueColumn: "false")
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-24") {
        addForeignKeyConstraint(baseColumnNames: "object_id", baseTableName: "statement", constraintName: "FK83B7296FED61A866", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "resource_reference", referencesUniqueColumn: "false")
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-25") {
        addForeignKeyConstraint(baseColumnNames: "qualifier_id", baseTableName: "statement", constraintName: "FK83B7296F9C92D2E8", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "qualifier", referencesUniqueColumn: "false")
    }
}

