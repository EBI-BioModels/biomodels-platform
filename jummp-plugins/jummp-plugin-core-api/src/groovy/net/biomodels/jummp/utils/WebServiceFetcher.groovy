/**
 * Copyright (C) 2010-2025 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import org.apache.http.HttpHost
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
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

    static getHttpStatus = {
        hit(0)
    }

    static getText = { ->
        hit(1)
    }

    private static def hit(int op = 1)  {
        URL url = new URL(requestUrl)
        def result = null
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
                result = op == 0 ? conn.responseCode : conn.getInputStream().text
            } else {
                result = conn.responseCode
                LOGGER.error("""Couldn't fetch data from the resource ${url.toString()} because of the error \
caused by ${conn.responseCode}: ${conn.getErrorStream().inspect()}""")
            }
        } catch (SocketTimeoutException ste) {
            result = conn.responseCode
            String msg = """Error while trying to retrieve data from ${url.toString()} due to ${ste.getMessage()}""".toString()
            LOGGER.error(msg, ste)
        } catch (IllegalArgumentException iae) {
            result = conn.responseCode
            LOGGER.error("The proxy setting cannot be null or ${iae.getMessage()}")
        } catch (FileNotFoundException exception) {
            result = conn.responseCode
            LOGGER.error("File Not Found ${exception.getMessage()}")
        } finally {
            if (conn.responseCode < 400)  {
                conn.getInputStream().close()
            }
        }
        return result
    }

    static String executePostRequest(final String serviceURI, final String requestBody) {
        CloseableHttpClient httpClient
        String hostName = System.getenv("HTTP_PROXY_HOST")
        int hostPort = System.getenv("HTTP_PROXY_PORT") as int
        HttpHost host = new HttpHost(hostName, hostPort)
        if (host) {
            httpClient = HttpClientBuilder.create().setProxy(host).build()
        } else {
            httpClient = HttpClientBuilder.create().build()
        }
        String result = null
        try {
            HttpPost request = new HttpPost(serviceURI)
            StringEntity params = new StringEntity(requestBody, "UTF-8")
            request.addHeader("content-type", "application/json")
            request.setEntity(params)

            CloseableHttpResponse response = httpClient.execute(request)
            try {
                HttpEntity entity = response.getEntity()
                if (entity != null) {
                    result = EntityUtils.toString(entity)
                }
            } finally {
                response.close()
            }
        } catch (Exception ignored) {
            // handle exception here
            ignored.printStackTrace()
        } finally {
            httpClient.close()
        }

        return result
    }

    static boolean isUserAgentSupported(final String userAgent) {
        def having = ["AppleWebKit", "Chrome", "Edg", "Edge", "Firefox",
                "Mozilla", "Opera", "Presto", "Safari"].find {
            userAgent.contains(it)
        }
        having != null
    }

    // https://stackoverflow.com/a/70198713/865603
    static URI addPath(URI uri, String path) {
        String newPath
        if (path.startsWith("/")) {
            newPath = path.replaceAll("//+", "/")
        } else if (uri.getPath().endsWith("/")) {
            newPath = uri.getPath() + path.replaceAll("//+", "/")
        } else {
            newPath = uri.getPath() + "/" + path.replaceAll("//+", "/")
        }

        // replace spaces with %20, see https://stackoverflow.com/a/2593319/865603
        return uri.resolve(newPath.replaceAll(" ", "%20")).normalize()

    }

    @Override
    void afterPropertiesSet() throws Exception {
        proxy = configurationService.verifyHttpProxy()
    }
}
