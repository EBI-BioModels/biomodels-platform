import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
* Copyright (C) 2010-20125 EMBL-European Bioinformatics Institute (EMBL-EBI),
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







class RequestLoggingFilters {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass())
    def filters = {
        // Define a filter for all API requests
        requestLogging(controller: '*', action: '*') {
            before = {
                if (request.queryString?.contains("format=") || request.queryString?.contains("/api/")) {
                    // Log the request and start a timer
                    request.setAttribute('startTime', System.currentTimeMillis())
                    LOGGER.info("""INCOMING REQUEST: ${request.getMethod()} \
${request.forwardURI}${request.queryString ? "?" + request.queryString : ""} \
from ${request.getRemoteAddr()}""")
                    // Return true to continue the request chain
                    // return true
                }
            }

            after = {
                // Not used for REST APIs
            }

            afterView = {
                if (request.queryString?.contains("format=") || request.queryString?.contains("/api/")) {
                    long startTime = request.getAttribute('startTime') as long
                    long duration = System.currentTimeMillis() - startTime
                    LOGGER.info("""OUTGOING RESPONSE: ${request.getMethod()} \
${request.forwardURI}${request.queryString ? "?" + request.queryString : ""} \
responded with ${response.getStatus()} in ${duration}ms""")
                }
            }
        }
    }
}
