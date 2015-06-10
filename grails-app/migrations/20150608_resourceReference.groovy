databaseChangeLog = {
    changeSet(author: "mglont (generated)", id: "1433757856028-3") {
        createTable(tableName: "resource_reference") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "resource_refePK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "accession", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "datatype", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "description", type: "longtext")

            column(name: "name", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "short_name", type: "varchar(255)")

            column(name: "uri", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-4") {
        createTable(tableName: "resource_reference_resource_reference") {
            column(name: "resource_reference_parents_id", type: "bigint")
            column(name: "resource_reference_id", type: "bigint")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-5") {
        createTable(tableName: "resource_reference_synonyms") {
            column(name: "resource_reference_id", type: "bigint")
            column(name: "synonyms_string", type: "varchar(255)")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-31") {
        createIndex(indexName: "FK6C1245F29AEA761", tableName: "resource_reference_resource_reference") {
            column(name: "resource_reference_parents_id")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-32") {
        createIndex(indexName: "FK6C1245F758A2D6B", tableName: "resource_reference_resource_reference") {
            column(name: "resource_reference_id")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-33") {
        createIndex(indexName: "FK5FECF61D758A2D6B", tableName: "resource_reference_synonyms") {
            column(name: "resource_reference_id")
        }
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-21") {
        addForeignKeyConstraint(baseColumnNames: "resource_reference_id", baseTableName: "resource_reference_resource_reference", constraintName: "FK6C1245F758A2D6B", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "resource_reference", referencesUniqueColumn: "false")
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-22") {
        addForeignKeyConstraint(baseColumnNames: "resource_reference_parents_id", baseTableName: "resource_reference_resource_reference", constraintName: "FK6C1245F29AEA761", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "resource_reference", referencesUniqueColumn: "false")
    }

    changeSet(author: "mglont (generated)", id: "1433757856028-23") {
        addForeignKeyConstraint(baseColumnNames: "resource_reference_id", baseTableName: "resource_reference_synonyms", constraintName: "FK5FECF61D758A2D6B", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "resource_reference", referencesUniqueColumn: "false")
    }
}

