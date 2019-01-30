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
import groovy.util.slurpersupport.GPathResult
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter as PLPA
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.model.PublicationLinkProvider
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
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
class PubMedService {
    final Log log = LogFactory.getLog(getClass())
    static transactional = false

    /**
     * Downloads the XML describing the PubMed resource and parses the Publication information.
     * @param id The PubMed Identifier
     * @return A fully populated Publication
     */
    @SuppressWarnings("EmptyCatchBlock")
    PubTC fetchPublicationData(String id) throws JummpException {
        def slurper = lookupPublicationDataInPubMed(id)

        PublicationLinkProviderTransportCommand linkCommand = createPubMedLinkProviderInstance()
        PubTC.fromPubMed(linkCommand, id, slurper)
    }

    @Cacheable("pubMedLinkProviderInstance")
    PublicationLinkProviderTransportCommand createPubMedLinkProviderInstance() {
        PublicationLinkProvider link = PublicationLinkProvider.withCriteria(uniqueResult: true) {
            eq("linkType", PublicationLinkProvider.LinkType.PUBMED)
        }
        PLPTC linkCommand = new PLPA(linkProvider: link).toCommandObject()
        linkCommand
    }

    GPathResult lookupPublicationDataInPubMed(String id) throws JummpException {
        URL url
        try {
            url = new URL("https://www.ebi.ac.uk/europepmc/webservices/rest/search/query=ext_id:${id}%20src:med&resulttype=core")
        } catch (MalformedURLException e) {
            // TODO: throw a specific exception
            throw new JummpException("PubMed URL is malformed", e)
        }

        def slurper = null
        try {
            slurper = new XmlSlurper().parse(url.openStream())
        } catch (SAXParseException e) {
            throw new JummpException("Could not parse PubMed information", e)
        } catch (Exception e) {
            throw new JummpException("Error retrieving publication info", e)
        }
        slurper
    }
}
