databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1703240129553-createAuthTokenTable") {
		createTable(tableName: "auth_token") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "auth_tokenPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "token", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "username", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	/*changeSet(author: "tnguyen (generated)", id: "1703240129553-2") {
		addNotNullConstraint(columnDataType: "varchar(1024)", columnName: "body", tableName: "notification")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-3") {
		dropForeignKeyConstraint(baseTableName: "tag_links", baseTableSchemaName: "jummp-biomodels", constraintName: "FK_lmil1jg72pjc8ei5p6kk5g9un")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-10") {
		dropIndex(indexName: "idx_autogen_cate_model_id", tableName: "auto_generated_category")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-11") {
		dropIndex(indexName: "idx_autogen_cate_name", tableName: "auto_generated_category")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-12") {
		dropIndex(indexName: "idx_ma_name", tableName: "modelling_approach")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-13") {
		dropIndex(indexName: "IDX_member_p2m", tableName: "p2mmapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-14") {
		dropIndex(indexName: "IDX_representative_p2m", tableName: "p2mmapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-15") {
		dropIndex(indexName: "idx_p2m_mem", tableName: "p2mmapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-16") {
		dropIndex(indexName: "idx_p2m_rep", tableName: "p2mmapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-17") {
		dropIndex(indexName: "unique_link", tableName: "publication")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-18") {
		dropIndex(indexName: "Accession_Index", tableName: "resource_reference")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-19") {
		dropIndex(indexName: "revision_number", tableName: "revision")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-20") {
		dropIndex(indexName: "upload_date", tableName: "revision")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-21") {
		dropIndex(indexName: "UK_t48xdq560gs3gap9g7jg36kgc", tableName: "tags")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-22") {
		dropIndex(indexName: "idx_uhlen_mem", tableName: "uhlen_model_mapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-23") {
		dropIndex(indexName: "idx_uhlen_rep", tableName: "uhlen_model_mapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-24") {
		dropIndex(indexName: "FK_1i8ibd0dm67mut0hh09oqt452", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-25") {
		dropIndex(indexName: "FK_8duair0heuk4lw83fsp2arsce", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-26") {
		dropIndex(indexName: "FK_c9u6eo21g7eku09byvrsjjm0s", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-27") {
		dropIndex(indexName: "FK_qs79ql67napbqpkvni2tw6ha0", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-28") {
		dropIndex(indexName: "FK_sgkk9pe0yu0f2brjsl8j20svy", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-29") {
		dropIndex(indexName: "content_aliasURI_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-30") {
		dropIndex(indexName: "content_changedOn_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-31") {
		dropIndex(indexName: "content_contentName_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-32") {
		dropIndex(indexName: "content_createdOn_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-33") {
		dropIndex(indexName: "content_space_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-34") {
		dropIndex(indexName: "unique_aliasuri", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-35") {
		dropIndex(indexName: "FK_1i8ibd0dm67mut0hh09oqt452", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-36") {
		dropIndex(indexName: "FK_8duair0heuk4lw83fsp2arsce", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-37") {
		dropIndex(indexName: "FK_c9u6eo21g7eku09byvrsjjm0s", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-38") {
		dropIndex(indexName: "FK_qs79ql67napbqpkvni2tw6ha0", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-39") {
		dropIndex(indexName: "FK_sgkk9pe0yu0f2brjsl8j20svy", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-40") {
		dropIndex(indexName: "content_aliasURI_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-41") {
		dropIndex(indexName: "content_changedOn_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-42") {
		dropIndex(indexName: "content_contentName_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-43") {
		dropIndex(indexName: "content_createdOn_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-44") {
		dropIndex(indexName: "content_space_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-45") {
		dropIndex(indexName: "unique_aliasuri", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-46") {
		dropIndex(indexName: "FK_gunqv3de4a2aowjtox5l10ug4", tableName: "wcm_related_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-47") {
		dropIndex(indexName: "FK_oxph3lt0u4eteq81gnh2dljrd", tableName: "wcm_related_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-48") {
		dropIndex(indexName: "UK_gkc6ixn9qkooura2r6lga6wll", tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-49") {
		dropIndex(indexName: "UK_hxt5tmvc0vgitd6qmw3yfd6or", tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-50") {
		dropIndex(indexName: "space_aliasURI_Idx", tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-51") {
		dropIndex(indexName: "space_name_Idx", tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-52") {
		createIndex(indexName: "FK_218s9w731wllynchepjvhb0eh", tableName: "contribution_details") {
			column(name: "role_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-53") {
		createIndex(indexName: "FK_7aw1u6d16fk6dpu21x2sv9bmu", tableName: "contribution_details") {
			column(name: "revision_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-54") {
		createIndex(indexName: "FK_dm2tsvy61mgariputdcy0gsmg", tableName: "contribution_details") {
			column(name: "contributor_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-55") {
		createIndex(indexName: "FK_192yj1slixr6439vtvoaejldw", tableName: "contribution_invite") {
			column(name: "role_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-56") {
		createIndex(indexName: "FK_3ol60twku48u5jfo20mss3u8x", tableName: "contribution_invite") {
			column(name: "revision_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-57") {
		createIndex(indexName: "FK_gc9wng3us8nf331g2xiy837t1", tableName: "contribution_invite") {
			column(name: "inviter_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-58") {
		createIndex(indexName: "reference_uniq_1703240129030", tableName: "contribution_invite", unique: "true") {
			column(name: "reference")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-59") {
		dropTable(tableName: "tag_links")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-60") {
		dropTable(tableName: "tags")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-61") {
		dropTable(tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-62") {
		dropTable(tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-63") {
		dropTable(tableName: "wcm_content_version")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-64") {
		dropTable(tableName: "wcm_related_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-65") {
		dropTable(tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-66") {
		dropTable(tableName: "wcm_status")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-4") {
		addForeignKeyConstraint(baseColumnNames: "contributor_id", baseTableName: "contribution_details", constraintName: "FK_dm2tsvy61mgariputdcy0gsmg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-5") {
		addForeignKeyConstraint(baseColumnNames: "revision_id", baseTableName: "contribution_details", constraintName: "FK_7aw1u6d16fk6dpu21x2sv9bmu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "revision", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-6") {
		addForeignKeyConstraint(baseColumnNames: "role_id", baseTableName: "contribution_details", constraintName: "FK_218s9w731wllynchepjvhb0eh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "contribution_role", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-7") {
		addForeignKeyConstraint(baseColumnNames: "inviter_id", baseTableName: "contribution_invite", constraintName: "FK_gc9wng3us8nf331g2xiy837t1", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-8") {
		addForeignKeyConstraint(baseColumnNames: "revision_id", baseTableName: "contribution_invite", constraintName: "FK_3ol60twku48u5jfo20mss3u8x", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "revision", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703240129553-9") {
		addForeignKeyConstraint(baseColumnNames: "role_id", baseTableName: "contribution_invite", constraintName: "FK_192yj1slixr6439vtvoaejldw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "contribution_role", referencesUniqueColumn: "false")
	}*/
}
