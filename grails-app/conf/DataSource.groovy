/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

Properties databaseProperties = new Properties()
try {
    def service = new net.biomodels.jummp.plugins.configuration.ConfigurationService()
    String pathToConfig=service.getConfigFilePath()
    if (!pathToConfig) {
        throw new Exception("No config file available, using defaults")
    }
    databaseProperties.load(new FileInputStream(pathToConfig))
    String server = databaseProperties.getProperty("jummp.database.server")
    String port = databaseProperties.getProperty("jummp.database.port")
    String database = databaseProperties.getProperty("jummp.database.database")
    String protocol
    switch (databaseProperties.getProperty("jummp.database.type")) {
        case "POSTGRESQL":
            protocol = "postgresql"
            databaseProperties.setProperty("jummp.database.driver", "org.postgresql.Driver")
            databaseProperties.setProperty("jummp.database.dialect",
                        "org.hibernate.dialect.PostgreSQLDialect")
            break
        case "MYSQL":
            protocol = "mysql"
            databaseProperties.setProperty("jummp.database.driver", "com.mysql.jdbc.Driver")
            databaseProperties.setProperty("jummp.database.dialect",
                        "org.hibernate.dialect.MySQL5InnoDBDialect")
            break
        default:
            protocol = ModelIdentifierUtils.DEFAULT_PROTOCOL
            databaseProperties.setProperty("jummp.database.driver", DEFAULT_DRIVER)
            databaseProperties.setProperty("jummp.database.dialect", DEFAULT_DIALECT)
            databaseProperties.setProperty("jummp.database.username", DEFAULT_USERNAME)
            databaseProperties.setProperty("jummp.database.password", DEFAULT_PASSWORD)
            databaseProperties.setProperty("jummp.database.url", DEFAULT_URL)
            databaseProperties.setProperty("jummp.database.pooled", 'false')
    }
    if (protocol != ModelIdentifierUtils.DEFAULT_PROTOCOL) {
        databaseProperties.setProperty("jummp.database.url",
                    "jdbc:${protocol}://${server}:${port}/${database}")
        databaseProperties.setProperty("jummp.database.pooled", "true")
    }
    def databaseConfig = new ConfigSlurper().parse(databaseProperties)

    dataSource {
        jmxEnabled = true
        pooled = Boolean.parseBoolean(databaseConfig.jummp.database.pooled)
        driverClassName = databaseConfig.jummp.database.driver
        username = databaseConfig.jummp.database.username
        password = databaseConfig.jummp.database.password
        dialect  = databaseConfig.jummp.database.dialect
        if (protocol != ModelIdentifierUtils.DEFAULT_PROTOCOL) {
            properties {
                maxActive = 50
                maxIdle = 25
                minIdle =1
                initialSize = 1
                minEvictableIdleTimeMillis = 60000
                timeBetweenEvictionRunsMillis = 60000
                numTestsPerEvictionRun = 3
                maxWait = 10000
                maxAge = 10 * 60000

                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = false

                validationQuery = "SELECT 1"
                validationQueryTimeout = 3
                validationInterval = 15000
            }
        } else {
            dbCreate = 'update'
        }
    }
    hibernate {
        cache.use_second_level_cache = true
        cache.use_query_cache = true
        cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
        format_sql = true
        use_sql_comments = true
    }
    // environment specific settings
    environments {
        development {
            dataSource {
                driverClassName = databaseConfig.jummp.database.driver
                username = databaseConfig.jummp.database.username
                password = databaseConfig.jummp.database.password
                dialect  = databaseConfig.jummp.database.dialect
                url = databaseConfig.jummp.database.url
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
            }
        }
        production {
            dataSource {
                driverClassName = databaseConfig.jummp.database.driver
                properties {
                    ignoreExceptionOnPreLoad = true
                    jdbcInterceptors = "ConnectionState;StatementCache(max=200)"
                    abandonWhenPercentageFull = 100
                    removeAbandoned = true
                    removeAbandonedTimeout = 120
                    logAbandoned = false
                    if (driverClassName == "com.mysql.jdbc.Driver") {
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
                username = databaseConfig.jummp.database.username
                password = databaseConfig.jummp.database.password
                dialect  = databaseConfig.jummp.database.dialect
                url = databaseConfig.jummp.database.url
            }
        }
    }

} catch (Exception e) {
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
