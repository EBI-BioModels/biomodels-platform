/**
* Copyright (C) 2010-2021 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
**/

import net.biomodels.jummp.core.model.identifier.ModelIdentifierUtils
import net.biomodels.jummp.plugins.configuration.ConfigurationService

Properties dbProps = new Properties()
try {
    println "${new Date().format("yyyy-MM-dd HH:mm:ss")} ${this.getClass().name} LOADING THE EXTERNAL CONFIG FILE..."
    def service = new ConfigurationService()
    String pathToConfig = service.getConfigFilePath()
    if (!pathToConfig) {
        throw new Exception("No config file available, using defaults")
    }
    dbProps.load(new FileInputStream(pathToConfig))
    String server = dbProps.getProperty("jummp.database.server")
    String port = dbProps.getProperty("jummp.database.port")
    String database = dbProps.getProperty("jummp.database.database")
    String protocol
    String dbType = dbProps.getProperty("jummp.database.type")
    switch (dbType) {
        case "POSTGRESQL":
            protocol = "postgresql"
            dbProps.setProperty("jummp.database.driver", "org.postgresql.Driver")
            dbProps.setProperty("jummp.database.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            break
        case "MYSQL":
            protocol = "mysql"
            dbProps.setProperty("jummp.database.driver", "com.mysql.cj.jdbc.Driver")
            dbProps.setProperty("jummp.database.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect")
            break
        case "MARIADB":
            protocol = "mariadb"
            dbProps.setProperty("jummp.database.driver", "org.mariadb.jdbc.Driver")
            dbProps.setProperty("jummp.database.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect")
            break
        default:
            protocol = ModelIdentifierUtils.DEFAULT_PROTOCOL
            dbProps.setProperty("jummp.database.driver", ModelIdentifierUtils.DEFAULT_DRIVER)
            dbProps.setProperty("jummp.database.dialect", ModelIdentifierUtils.DEFAULT_DIALECT)
            dbProps.setProperty("jummp.database.username", ModelIdentifierUtils.DEFAULT_USERNAME)
            dbProps.setProperty("jummp.database.password", ModelIdentifierUtils.DEFAULT_PASSWORD)
            dbProps.setProperty("jummp.database.url", ModelIdentifierUtils.DEFAULT_URL)
            dbProps.setProperty("jummp.database.pooled", 'false')
    }
    if (protocol != ModelIdentifierUtils.DEFAULT_PROTOCOL) {
        dbProps.setProperty("jummp.database.url", "jdbc:${protocol}://${server}:${port}/${database}")
        dbProps.setProperty("jummp.database.pooled", "true")
    }
    if (protocol == 'mysql') {
        String unicodeOpts = "useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=CONVERT_TO_NULL"
        unicodeOpts += "&serverTimezone=GMT&useSSL=false&allowPublicKeyRetrieval=true"
        // Connector/J 8 no longer treats a null catalog as the current database (5.x did). Liquibase 2.0.5
        // (database-migration 1.4.1) calls DatabaseMetaData.getTables(null, ...), which 8.0.11 turns into
        // invalid SQL ("WHERE HAVING TABLE_TYPE IN ...") and later 8.0.x turn into a scan of every database
        // on the server, so dbm-gorm-diff & co. either crash or diff against the wrong schemas.
        unicodeOpts += "&nullCatalogMeansCurrent=true"
        dbProps.setProperty("jummp.database.url",
            "jdbc:${protocol}://${server}:${port}/${database}?${unicodeOpts}")
    }
    if (protocol == 'mariadb') {
        String unicodeOpts = "useUnicode=true&characterEncoding=UTF-8"
        dbProps.setProperty("jummp.database.url",
            "jdbc:${protocol}://${server}:${port}/${database}?${unicodeOpts}")
    }

    ConfigObject dbConfig = new ConfigSlurper().parse(dbProps)

    dataSource {
//        logSql = true
        jmxEnabled = true
        pooled = Boolean.parseBoolean(dbConfig.jummp.database.pooled as String)
        driverClassName = dbConfig.jummp.database.driver
        dialect  = dbConfig.jummp.database.dialect
        username = dbConfig.jummp.database.username
        password = dbConfig.jummp.database.password
        url = dbConfig.jummp.database.url
        if (protocol != ModelIdentifierUtils.DEFAULT_PROTOCOL) {
            properties {
                // Documentation for Tomcat JDBC Pool
                // http://tomcat.apache.org/tomcat-7.0-doc/jdbc-pool.html#Common_Attributes
                // https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/tomcat/jdbc/pool/PoolConfiguration.html
                maxActive = 100
                maxIdle = 25
                minIdle = 1
                initialSize = 1
                minEvictableIdleTimeMillis = 60000
                timeBetweenEvictionRunsMillis = 60000
                numTestsPerEvictionRun = 3
                maxWait = 30000
                maxAge = 10 * 60000

                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = false

                validationQuery = "SELECT 1"
                validationQueryTimeout = 3
                validationInterval = 15000
            }
        } else {
            //dbCreate = 'update'
        }
    }
    hibernate {
        cache.use_second_level_cache = true
        cache.use_query_cache = true
        cache.region.factory_class = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory4' // needed to runApp
        format_sql = true
        use_sql_comments = true
        singleSession = true // configure OSIV singleSession mode
        flush.mode = 'manual' // OSIV session flush mode outside of transactional context
    }
    // environment specific settings
    environments {
        development {
            dataSource {
//                logSql = true
//                dbCreate = "create"
            }
        }
        test {
            hibernate {
                cache.use_second_level_cache = false
                cache.use_query_cache = false
            }
            dataSource {
                url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
                username = ModelIdentifierUtils.DEFAULT_USERNAME
                password = ModelIdentifierUtils.DEFAULT_PASSWORD
                dialect = ModelIdentifierUtils.DEFAULT_DIALECT
                driverClassName = ModelIdentifierUtils.DEFAULT_DRIVER
                // can't use databaseMigrations
                dbCreate = "update"
//                logSql = true
            }
        }
        production {
            dataSource {
//                logSql = true
                properties {
                    ignoreExceptionOnPreLoad = true
                    jdbcInterceptors = "ConnectionState;StatementCache(max=200)"
                    abandonWhenPercentageFull = 100
                    removeAbandoned = true
                    removeAbandonedTimeout = 120
                    logAbandoned = false
                    if (it.driverClassName in ["com.mysql.cj.jdbc.Driver", "org.mariadb.jdbc.Driver"]) {
                        // JDBC driver properties
                        // Mysql as example
                        dbProperties {
                            autoReconnect = false
                            jdbcCompliantTruncation = false
                            zeroDateTimeBehavior = 'convertToNull'
                            cachePrepStmts = false
                            cacheCallableStmts = false
                            dontTrackOpenResources = false
                            holdResultsOpenOverStatementClose = true
                            useServerPrepStmts = false
                            cacheServerConfiguration = true
                            cacheResultSetMetadata = true
                            metadataCacheSize = 100
                            connectionTimeout = 15000
                            socketTimeout = 120000
                            maintainTimeStats = false
                            enableQueryTimeouts = false
                            noDatetimeStringSync = true
                        }
                    }
                }
            }
        }
    }

} catch (Exception ignored) {
    // no database configured yet, use h2
    hibernate {
        cache.use_second_level_cache = false
        cache.use_query_cache = false
    }
    dataSource {
        url = ModelIdentifierUtils.DEFAULT_URL
        username = ModelIdentifierUtils.DEFAULT_USERNAME
        password = ModelIdentifierUtils.DEFAULT_PASSWORD
        dialect = ModelIdentifierUtils.DEFAULT_DIALECT
        driverClassName = ModelIdentifierUtils.DEFAULT_DRIVER
    }
}
