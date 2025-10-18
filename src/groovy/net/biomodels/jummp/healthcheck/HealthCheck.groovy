/*
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */

package net.biomodels.jummp.healthcheck

import grails.util.Holders
import groovy.transform.CompileStatic
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.CacheMode
import org.hibernate.Session

import java.sql.SQLException

import static net.biomodels.jummp.healthcheck.HealthCheck.STATUS.*
import static net.biomodels.jummp.healthcheck.HealthCheck.TYPE.DATABASE

@CompileStatic
class HealthCheck {
    private static final Log log = LogFactory.getLog(HealthCheck.class)
    private static String validationQuery

    enum TYPE {
        DATABASE
    }

    enum STATUS {
        OK, ERROR, UNKNOWN
    }

    TYPE type
    STATUS status

    static {
        validationQuery = getValidationQuery()
    }

    static HealthCheck forDatabase() {
        def check = new HealthCheck(type: DATABASE, status: UNKNOWN)
        try {
            User.withSession { Session session ->
                session.createSQLQuery(validationQuery)
                    .setCacheMode(CacheMode.IGNORE)
                    .setTimeout(30)
                    .setReadOnly(true)
                    .uniqueResult()
            }
            check.status = OK
        } catch (SQLException e) {
            log.fatal("Cannot connect to the database", e)
            check.status = ERROR
        }
        check
    }

    Map<String, String> toMap() {
        def map = [:]
        String key = asName(type)
        String value = asName(status)
        map[key] = value
        map as Map<String, String>
    }

    String toString() {
        "HealthCheck ${toMap()}"
    }

    private String asName(Enum value) {
        value.name().toLowerCase(Locale.ENGLISH)
    }

    private static String getValidationQuery() {
        Holders.config.flatten().get("dataSource.properties.validationQuery")
    }
}
