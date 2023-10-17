/**
 * Copyright (C) 2010-2023 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

/**
 * A class for connecting to remote documents and fetching their context as text
 * @author Tung Nguyen <nvntung@gmail.com>
 * @date   13/06/2023
 */
class WebServiceFetcher implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceFetcher.class)

    private static HttpURLConnection conn
    private static Proxy proxy
    private static String requestUrl

    def configurationService

    WebServiceFetcher(final String _requestUrl) {
        requestUrl = _requestUrl
    }

    static getText = { ->
        URL url = new URL(requestUrl)
        String result = null
        try {
            if (proxy) {
                conn = (HttpURLConnection) url.openConnection(proxy)
            } else {
                conn = (HttpURLConnection) url.openConnection()
            }

            conn.setConnectTimeout(15000)
            conn.setReadTimeout(30000)
            conn.connect()
            if (conn.responseCode < 400) {
                result = conn.getInputStream().text
            } else {
                LOGGER.error("""Couldn't fetch data from the resource ${url.toString()} because of the error \
caused by ${conn.getErrorStream().inspect()}""")
            }
        } catch (SocketTimeoutException ste) {
            String msg = """Error while trying to retrieve data from ${url.toString()} due to ${ste.getMessage()}""".toString()
            LOGGER.error(msg, ste)
        } catch (IllegalArgumentException iae) {
            LOGGER.error("The proxy setting cannot be null or ${iae.getMessage()}")
        } finally {
            conn.getInputStream().close()
            return result
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        proxy = configurationService.verifyHttpProxy()
    }
}
