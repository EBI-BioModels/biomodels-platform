/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Apache Commons (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Commons used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.core.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

class JummpHttpService implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(JummpHttpService.class)

    def configurationService

    private static Proxy proxy

    static String getStatus(String url) throws IOException {
        String result = ""
        int code = getStatusCode(url)
        try {
            URL siteURL = new URL(url)
            HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection()
            connection.setRequestMethod("GET")
            connection.setConnectTimeout(3000)
            connection.connect()

            code = connection.getResponseCode()
            if (code == 200) {
                result = "-> Green <-\t" + "Code: " + code

            } else {
                result = "-> Yellow <-\t" + "Code: " + code
            }
        } catch (Exception e) {
            result = "-> Red <-\t" + "Wrong domain - Exception: " + e.getMessage()

        }
        System.out.println(url + "\t\tStatus:" + result)
        result
    }

    static int getStatusCode(String url) throws IOException {
        int code = 200
        try {
            URL siteURL = new URL(url)
            HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection()
            connection.setRequestMethod("GET")
            connection.setConnectTimeout(3000)
            connection.connect()
            code = connection.getResponseCode()
        } catch (Exception e) {
            code = 404
        }
        return code
    }

    static String jsonGetRequest(String urlQueryString) {
        String json = null
        try {
            URL url = new URL(urlQueryString)
            HttpURLConnection connection
            if (proxy) {
                connection = (HttpURLConnection) url.openConnection(proxy)
            } else {
                connection = (HttpURLConnection) url.openConnection()
            }
            connection.setDoOutput(true)
            connection.setInstanceFollowRedirects(true)
            connection.setRequestMethod("GET")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("charset", "utf-8")
            connection.connect()
            InputStream inStream = connection.getInputStream()
            json = streamToString(inStream) // input stream to string
        } catch (IOException ex) {
            ex.printStackTrace()
        }
        return json
    }

    static String getDataTypeAndAccession(String uri) {
        if (uri == null || uri.isEmpty()) {
            logger.error("The URI given is null or empty");
            return null;
        }
        if (uri.startsWith("http://")) {
            uri = uri.replace("http", "https");
        }
        String rest = uri.substring(("https://identifiers.org/").length());
        return rest;
    }

    @Override
    void afterPropertiesSet() throws Exception {
        proxy = configurationService.verifyHttpProxy()
    }

    private static String streamToString(InputStream inputStream) {
        String text = new Scanner(inputStream, "UTF-8").useDelimiter("\\Z").next()
        return text
    }
}
