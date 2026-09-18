/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* Spring Framework (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0 the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Framework used as well as
* that of the covered work.}
**/

package net.biomodels.jummp.core

import grails.plugin.cache.Cacheable
import groovy.transform.PackageScope
import groovy.util.slurpersupport.GPathResult
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter as PLPA
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.model.PublicationLinkProvider as PubLP
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.InitializingBean
import org.xml.sax.SAXParseException

/**
 * @short Service for fetching Publication Information for PubMed resources.
 *
 * This service class handles the interaction with the Web Service to retrieve
 * publication information for PubMed resources. It connects to citexplore and
 * parses the returned HTML page for the publication information.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class PubMedService extends AbstractPubDataFetchStrategy implements InitializingBean {
    final Log log = LogFactory.getLog(getClass())
    static transactional = false

    def configurationService

    final String PUBMED_API_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query="

    /**
     * Downloads the XML describing the PubMed resource and parses the Publication information.
     * @param id The PubMed Identifier
     * @return A fully populated Publication
     */
    @SuppressWarnings("EmptyCatchBlock")
    @Override
    PubTC fetchPublicationData(String id) throws JummpException {
        // default PubMed
        final queryString = "${PUBMED_API_URL}ext_id:${id}%20src:med&resulttype=core"
        def slurper = lookupPublicationDataInPubMed(queryString)
        if (!slurper) {
            return null
        }

        PLPTC linkCommand = createLinkProviderInstance()
        PubTC.fromPubMed(linkCommand, id, slurper)
    }

    @SuppressWarnings("EmptyCatchBlock")
    @Override
    PubTC fetchPublicationData(final String id, final String linkType) throws JummpException {
        PubLP.LinkType type = PubLP.LinkType.findLinkTypeByLabel(linkType)
        fetchPublicationData(id, type)

    }

    @SuppressWarnings("EmptyCatchBlock")
    @Override
    PubTC fetchPublicationData(final String id, final PubLP.LinkType linkType) throws JummpException {
        String queryString = ""
        if (linkType == PubLP.LinkType.PUBMED) {
            queryString = "${PUBMED_API_URL}ext_id:${id}%20src:med&resulttype=core"
        } else if (linkType == PubLP.LinkType.DOI) {
                queryString = "${PUBMED_API_URL}doi:${id}%20&resulttype=core"
        }
        def slurper = lookupPublicationDataInPubMed(queryString)
        if (!slurper) {
            return null
        }

        PLPTC linkCommand = createLinkProviderInstance(linkType)
        PubTC.fromPubMed(linkCommand, id, slurper)
    }

    @Cacheable("pubMedLinkProviderInstance")
    @Override
    PLPTC createLinkProviderInstance() {
        PubLP link = PubLP.withCriteria(uniqueResult: true) {
            eq("linkType", PubLP.LinkType.PUBMED)
        } as PubLP
        PLPTC linkCommand = new PLPA(linkProvider: link).toCommandObject()
        linkCommand
    }

    @Cacheable("pubMedLinkProviderInstance")
    @Override
    PLPTC createLinkProviderInstance(final String linkTypeAsString) {
        PubLP.LinkType type = PubLP.LinkType.findLinkTypeByLabel(linkTypeAsString)
        createLinkProviderInstance(type)
    }

    @Cacheable("pubMedLinkProviderInstance")
    @Override
    PLPTC createLinkProviderInstance(final PubLP.LinkType linkType) {
        PubLP link = PubLP.withCriteria(uniqueResult: true) {
            eq("linkType", linkType)
        } as PubLP
        PLPTC linkCommand = new PLPA(linkProvider: link).toCommandObject()
        linkCommand
    }

    // @PackageScope (not private) so tests can stub it via metaClass - a bare method with no
    // modifier is public in Groovy (unlike fields), and a genuinely private method's internal
    // self-calls compile to a direct JVM invokespecial that bypasses the MetaClass entirely.
    @PackageScope
    GPathResult lookupPublicationDataInPubMed(String strURL) throws JummpException {
        URL url
        try {
            url = new URL(strURL)
        } catch (MalformedURLException e) {
            // TODO: throw a specific exception
            throw new JummpException("PubMed URL is malformed", e)
        }

        // TODO: take the following snippet and the same in ParameterSearchService into a common service
        HttpURLConnection conn
        Proxy proxy = configurationService.verifyHttpProxy()
        try {
            if (proxy) {
                conn = (HttpURLConnection) url.openConnection(proxy)
                log.debug("via HTTP PROXY: ${proxy.dump()}")
            } else {
                conn = (HttpURLConnection) url.openConnection()
                log.debug("No HTTP PROXY")
            }
            conn.setConnectTimeout(1000)
            conn.setReadTimeout(1000)
            conn.connect()
            if (conn.responseCode < 400) {
                try {
                    String records = conn.getInputStream().text
                    def slurper
                    try {
                        slurper = new XmlSlurper().parseText(records)
                    } catch (SAXParseException e) {
                        throw new JummpException("Could not parse PubMed information", e)
                    } catch (Exception e) {
                        throw new JummpException("Error retrieving publication info", e)
                    }
                    return slurper
                } catch (IOException e) {
                    log.error("""Error while getting data from HttpUrlConnection ${conn.dump()} \
because of the error ${e.message}""")
                    return null
                }
            } else {
                log.error("""Couldn't fetch data from the resource ${url.dump()} because of the error caused by ${conn
                    .getErrorStream()
                    .inspect()}""")
                return null
            }
        } catch (SocketException se) {
            log.error("Error while retrieving records from PubMed Centre", se)
        } catch (IllegalArgumentException ile) {
            log.error("The proxy setting cannot be null", ile)
        }
        return null
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Finished the bean initialisation")
    }
}
