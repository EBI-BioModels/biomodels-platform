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
import groovy.json.JsonSlurper
import groovy.transform.PackageScope
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter as PLPA
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.core.user.PersonTransportCommand as PersonTC
import net.biomodels.jummp.model.PublicationLinkProvider as PubLP
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

/**
 * @short Singleton-scoped facade for fetching publication metadata via DOI.
 *
 * This service provides means of looking up the metadata of publications via DOI. It asks doi.org for the
 * CSL-JSON representation of the record. Unlike BibTeX, whose layout differs between registration agencies
 * (Crossref emits one line, DataCite one field per line) and has changed over time, CSL-JSON has the same
 * structure whichever agency registered the DOI.
 *
 * @author <a href="mailto:tungnguyenvn@pm.me">tungnguyenvn@pm.me</a>
 * @date   2021-01-17
 * @update 2026-09-19
 */
class DoiService extends AbstractPubDataFetchStrategy implements InitializingBean {
    static transactional = false
    private static final Logger logger = LoggerFactory.getLogger(DoiService.class)
    private static final String CSL_JSON_MEDIA_TYPE = "application/vnd.citationstyles.csl+json"
    private static final String NOT_AVAILABLE = "N/A"
    private static final String BARE_ORCID_PATTERN = /^\d{4}-\d{4}-\d{4}-\d{3}(\d|X)$/

    @Override
    PubTC fetchPublicationData(final String doi) throws JummpException {
        PubTC pubTC = buildPubTCFromCslJson(doi, lookupPublicationDataFromDOI(doi))
        if (!pubTC) {
            return null
        }
        if (pubTC.validate()) {
            return PubTC.fromDOI(pubTC)
        }
        logger.error("The DOI ${doi}: cannot pull all required information! Errors: ${pubTC.errors.toString()}")
        return null
    }

    @Cacheable("doiLinkProviderInstance")
    @Override
    PLPTC createLinkProviderInstance() {
        PubLP link = PubLP.withCriteria(uniqueResult: true) {
            eq("linkType", PubLP.LinkType.DOI)
        } as PubLP
        PLPTC linkCommand = new PLPA(linkProvider: link).toCommandObject()
        linkCommand
    }

    /**
     * Turns the CSL-JSON record of a DOI into a publication.
     *
     * @param doi the DOI that was looked up
     * @param rawJson the response body from doi.org
     * @return the publication, or null if the response is not a CSL-JSON record with a title, e.g. because
     * the DOI or its registration agency is unknown and doi.org answered with an HTML error page.
     */
    // @PackageScope (not private) so tests can call it and stub its collaborators via metaClass
    @PackageScope
    PubTC buildPubTCFromCslJson(final String doi, final String rawJson) {
        if (!rawJson?.trim()) {
            logger.debug("The raw details of the publication record ${doi} cannot be empty.")
            return null
        }
        def csl
        try {
            csl = new JsonSlurper().parseText(rawJson)
        } catch (Exception ignored) {
            logger.debug("DOI ${doi} Not Found")
            return null
        }
        String title = csl instanceof Map ? firstText(csl["title"]) : null
        if (!title) {
            logger.debug("DOI ${doi} has no title in its CSL-JSON record")
            return null
        }

        PubTC pubTC = new PubTC(linkProvider: createLinkProviderInstance(), link: doi)
        pubTC.title = title
        // preprints have no container-title, in which case the publisher (e.g. openRxiv, arXiv) is the best fit
        pubTC.journal = firstText(csl["container-title"]) ?: firstText(csl["publisher"]) ?: NOT_AVAILABLE
        pubTC.volume = firstText(csl["volume"]) ?: NOT_AVAILABLE
        pubTC.issue = firstText(csl["issue"]) ?: NOT_AVAILABLE
        pubTC.pages = firstText(csl["page"]) ?: NOT_AVAILABLE
        // the two attributes below are not extracted from the record yet
        pubTC.affiliation = NOT_AVAILABLE
        pubTC.synopsis = NOT_AVAILABLE
        applyPublicationDate(pubTC, csl["issued"])
        pubTC.authors = parseAuthors(csl["author"])
        return pubTC
    }

    // @PackageScope (not private) so tests can stub it via metaClass - see PubMedService.lookupPublicationDataInPubMed
    @PackageScope
    String lookupPublicationDataFromDOI(final String doi) {
        // this method works without specifying proxy in the curl command
        // because we had given the proxy arguments to JVM
        String acceptHeader = "Accept: " + CSL_JSON_MEDIA_TYPE
        String url = "https://dx.doi.org/" + doi
        String[] cmd = ["curl", "-sL", "-m", "20", "-H", acceptHeader, url]
        return cmd.execute().text
    }

    /**
     * CSL-JSON stores some values as a string on one record and as a list on another, e.g. the
     * container-title of a preprint is an empty list.
     */
    private static String firstText(final def value) {
        def first = value instanceof Collection ? (value ? value.first() : null) : value
        first?.toString()?.trim() ?: null
    }

    private static void applyPublicationDate(final PubTC pubTC, final def issued) {
        def dateParts = issued instanceof Map ? issued["date-parts"] : null
        List parts = dateParts instanceof List && dateParts && dateParts[0] instanceof List ? dateParts[0] : []
        if (parts.size() > 0 && parts[0] != null) {
            pubTC.year = parts[0] as Integer
        }
        if (parts.size() > 1 && parts[1] != null) {
            pubTC.month = parts[1] as String
        }
        if (parts.size() > 2 && parts[2] != null) {
            pubTC.day = parts[2] as Integer
        }
    }

    private static List<PersonTC> parseAuthors(final def rawAuthors) {
        List<PersonTC> authors = new ArrayList<>()
        for (def rawAuthor : (rawAuthors instanceof Collection ? rawAuthors : [])) {
            if (!(rawAuthor instanceof Map)) {
                continue
            }
            // an organisation is recorded under "literal" (or "name") instead of given/family
            String name = [firstText(rawAuthor["given"]), firstText(rawAuthor["family"])].findAll { it }.join(" ")
            name = name ?: (firstText(rawAuthor["literal"]) ?: firstText(rawAuthor["name"]))
            if (!name) {
                continue
            }
            PersonTC author = new PersonTC(userRealName: name)
            // Person.orcid only accepts the bare identifier, while CSL-JSON carries the full https://orcid.org/ URL
            String orcid = firstText(rawAuthor["ORCID"])?.replaceFirst(/^https?:\/\/orcid\.org\//, "")
            if (orcid ==~ BARE_ORCID_PATTERN) {
                author.orcid = orcid
            }
            def affiliations = rawAuthor["affiliation"]
            String institution = affiliations instanceof Collection ?
                affiliations.collect { it instanceof Map ? firstText(it["name"]) : firstText(it) }.findAll { it }.join("; ") : null
            if (institution) {
                author.institution = institution
            }
            authors.add(author)
        }
        return authors
    }

    @Override
    void afterPropertiesSet() throws Exception {
        logger.info("Finished the bean initialisation")
    }
}
