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





package net.biomodels.jummp.core

import grails.plugin.cache.Cacheable
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter as PLPA
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.core.user.PersonTransportCommand
import net.biomodels.jummp.model.PublicationLinkProvider
import org.apache.commons.lang3.StringUtils
import org.apache.tools.ant.util.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @short Singleton-scoped facade for fetching publication metadata via DOI.
 *
 * This service provides means of looking up the metadata of publications via DOI.
 *
 * @author <a href="mailto:tungnguyenvn@pm.me">tungnguyenvn@pm.me</a>
 * @date   2021-01-17
 */
class DoiService implements PubDataFetchStrategy {
    static transactional = false
    private static final Logger logger = LoggerFactory.getLogger(DoiService.class)

    @Override
    PubTC fetchPublicationData(final String doi) throws JummpException {
        Map fetchedData = lookupPublicationDataFromDOI(doi)
        PubTC pubTC = buildPubTCFromRawData(fetchedData)
        PubTC.fromDOI(pubTC)
    }

    @Cacheable("doiLinkProviderInstance")
    @Override
    PLPTC createLinkProviderInstance() {
        PublicationLinkProvider link = PublicationLinkProvider.withCriteria(uniqueResult: true) {
            eq("linkType", PublicationLinkProvider.LinkType.DOI)
        }
        PLPTC linkCommand = new PLPA(linkProvider: link).toCommandObject()
        linkCommand
    }

    PubTC buildPubTCFromRawData(final Map rawData) {
        String doi = rawData["doi"]
        String rawPubDetails = rawData["pubDetails"]
        if (!rawPubDetails) {
            logger.debug("The raw details of the publication record ${rawData.get('doi')} cannot be empty.")
            return null
        }
        if (rawPubDetails.charAt(0) != '@') {
            logger.debug("DOI ${doi} Not Found")
            return null
        }
        String left = rawPubDetails.substring(rawPubDetails.indexOf(",") + 1)
        left = left?.substring(0, left?.length() - 1)
        String need = left?.substring(0, left.lastIndexOf("}"))
        String[] parts = need?.split(",\n\t")
        parts[0].replace("\n\t", "")
        PLPTC linkProvider = createLinkProviderInstance()
        PubTC pubTC = new PubTC(linkProvider: linkProvider, link: rawData["doi"])
        Map pubMap = [:]
        for (String p : parts) {
            String[] items = p.split(" = ")
            String attr = items[0].trim()
            String val = items[1].trim()
            val = val.replaceAll("\\{", "").replaceAll("\\}", "")
            pubMap.put(attr, val)
        }
        if (!pubMap?.isEmpty()) {
            pubTC.link = pubMap.get("doi")
            pubTC.title = pubMap.get("title")
            pubTC.journal = pubMap.get("journal")
            pubTC.authors = parseAuthorsFromRawText(pubMap.get("author"))
            pubTC.volume = pubMap.get("volume")
            pubTC.issue = pubMap.get("number")
            pubTC.pages = pubMap.get("pages")
            pubTC.year = Integer.parseInt(pubMap.get("year"))
            pubTC.month = inferFromMonthName(pubMap.get("month"))

            // Currently, the two attributes below are missing due to the limitations of this approach
            /*pubTC.affiliation
            pubTC.synopsis*/
        }
        return pubTC
    }

    private Map lookupPublicationDataFromDOI(final String doi) {
        // this method works without specifying proxy in the curl command
        // because we had given the proxy arguments to JVM
        Map result = ["doi": doi]
        String url = "https://dx.doi.org/$doi"
        String[] cmd = ["curl", "-LH", "Accept: application/x-bibtex", url]
        String txt = cmd.execute().text
        result.put("pubDetails", txt)
        result
    }

    List<PersonTransportCommand> parseAuthorsFromRawText(final String rawText) {
        List<PersonTransportCommand> authors = new ArrayList<>()
        if (rawText) {
            String[] authorSet = rawText.trim().split(" and ")
            if (authorSet?.size()) {
                for (String authorName : authorSet) {
                    PersonTransportCommand author = new PersonTransportCommand()
                    author.userRealName = authorName
                    authors.add(author)
                }
            }
        }
        return authors
    }

    private String inferFromMonthName(final String name) {
        int retVal
        switch (name) {
            case "jan":
                retVal = 1
                break
            case "feb":
                retVal = 2
                break
            case "mar":
                retVal = 3
                break
            case "apr":
                retVal = 4
                break
            case "may":
                retVal = 5
                break
            case "jun":
                retVal = 6
                break
            case "jul":
                retVal = 7
                break
            case "aug":
                retVal = 8
                break
            case "sep":
                retVal = 9
                break
            case "oct":
                retVal = 10
                break
            case "nov":
                retVal = 11
                break
            case "dec":
                retVal = 12
                break
        }
        retVal as String
    }
}
