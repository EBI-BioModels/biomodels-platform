/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccessTokenExpirationCheckerJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    def restAccessTokenService

    static triggers = {
        // cron name: "checkAccessTokenExpiration", cronExpression: "0 0/5 * 1/1 * ? *"
        cron name: "checkAccessTokenExpiration", cronExpression: "0 0/1 * 1/1 * ? *"
    }

    def execute() {
        String msgLog = """\
${new Date().format('yyyy-MM-dd HH:mm:ss')} QuartzJob: Started checking the access token expiration."""
        LOGGER.info(msgLog)
        println(msgLog)
        try {
            println "Let run the job: check expiredDate whether sending an email to remind or delete the expired one and sending an email"
            LOGGER.info("Let run the job")
            restAccessTokenService.doCheckAndExpireAccessTokens()
        } catch (Exception e) {
            println "Caught the bug and do nothing ${e.getMessage()}"

            LOGGER.info("Caught the bug and do nothing")
        }
    }
}
