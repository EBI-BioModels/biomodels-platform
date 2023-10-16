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

import groovy.json.JsonSlurper
import net.biomodels.jummp.core.constants.BioModels

/**
 * This class collects all utility methods to work with BioModels data published on EBI Search Server
 * @author Tung Nguyen <nvntung@gmail.com>
 * @date   13/06/2023
 */
class EbiSearchHelper extends WebServiceFetcher {

    EbiSearchHelper(String _requestUrl) {
        super(_requestUrl)
    }

    /**
     * Makes the simple hit to EBI Search Server to get the number of hits
     * @return an Integer object indicating the number of hits
     */
    static int fetchBioModelsHitCount() {
        String queryStr = "${BioModels.EBI_PROD_WS_REST_BM_URL}?query=*:*&size=10&start=0&fields=submissionid,publicationid&format=json"
        EbiSearchHelper wsFetcher = new EbiSearchHelper(queryStr)
        String result = wsFetcher.getText()
        JsonSlurper slurper = new JsonSlurper()
        Map parsedJson = slurper.parseText(result) as Map
        int hitCount = parsedJson.get("hitCount") as Integer
        hitCount
    }

    /**
     * Computes how many loops if we have to iterate on the list of all entries based on the size of each request
     *
     * @param size It is the number of entries we want the remote server to return
     * @return an Integer object telling the number of loops
     */
    static int computeLoopCountBasedSize(final int size) {
        int hitCount = fetchBioModelsHitCount()
        Math.ceil(hitCount/size).toInteger()
    }

    /**
     * Checks whether OmicsDI has duplicated indexed data or not.
     * @return a Map object indicating duplicated model identifiers
     */
    static Map checkIndexedData() {
        final int SIZE = 1_000
        final String WS_URL = "${BioModels.EBI_PROD_WS_REST_BM_URL}?query=*:*&fields=submissionid,publicationid&format=json"
        int loopCount = computeLoopCountBasedSize(SIZE)
        int start = 0
        Map mapResult = [:]
        for (int i = 1; i <= loopCount; i++) {
            String queryStr = "${WS_URL}&size=${SIZE}&start=${start}"
            WebServiceFetcher wsFetcher = new WebServiceFetcher(queryStr)
            String result = wsFetcher.getText()
            Map parsedJson = new JsonSlurper().parseText(result) as Map
            processData(parsedJson, mapResult)
            start = i*SIZE
        }
        /*File out = new File("all-models.csv")
        mapResult.each {
            out << "${it.key}: ${it.value}\n"
        }*/

        Map mapRedundant = findOutRedundantDataInOmicsDi(mapResult)

        return mapRedundant
    }

    private static Map findOutRedundantDataInOmicsDi(final Map mapResult) {
        Map mapRedundant = new HashMap()
        List listDuplicatedIds = new ArrayList()
        mapResult.each {
            if (it.value) {
                String subId = it.value
                String key = it.key
                if (mapResult.containsKey(subId) && key.indexOf("BIOMD") >= 0) {
                    listDuplicatedIds.add subId
                }
            }
        }
        if (listDuplicatedIds?.size()) {
            mapRedundant.put "message", "Redundant"
            mapRedundant.put "listDuplicatedIds", listDuplicatedIds
        } else {
            mapRedundant.put "message", "OK"
        }
        mapRedundant
    }

    private static void processData(def jsonData, Map mapResult) {
        List entries = jsonData.get("entries")
        int nbEntries = entries?.size()
        for (int i = 0; i < nbEntries; i++) {
            String id = entries[i].get("id")
            String submissionId = entries[i].fields.submissionid[0]
            mapResult.put(id, submissionId)
        }
    }
}
