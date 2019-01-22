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

import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter as PLPA
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.plugins.security.PersonTransportCommand as PersonTC
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
    def messageSource

    private setFieldIfItExists(String fieldName, PubTC publication,
                               def xmlField, boolean castToInt) {
        try {
            if (xmlField && xmlField.size() == 1) {
                String text = xmlField.text()
                if (castToInt) {
                    try {
                        publication."${fieldName}" = text as int
                    } catch (NumberFormatException ignored) {
                        final String pId = publication.link
                        log.warn "Field '$fieldName' of publication $pId is not numerical: $text"
                    }
                }
                else {
                    publication."${fieldName}" = text
                }
            }
        } catch(Exception e) {
            log.error e.message, e
        }
    }

    /**
     * Downloads the XML describing the PubMed resource and parses the Publication information.
     * @param id The PubMed Identifier
     * @return A fully populated Publication
     */
    @SuppressWarnings("EmptyCatchBlock")
    PubTC fetchPublicationData(String id) throws JummpException {
        URL url
        try {
            url = new URL("https://www.ebi.ac.uk/europepmc/webservices/rest/search/query=ext_id:${id}%20src:med&resulttype=core")
        } catch (MalformedURLException e) {
            // TODO: throw a specific exception
            throw new JummpException("PubMed URL is malformed", e)
        }

        def slurper
        try {
            slurper = new XmlSlurper().parse(url.openStream())
        } catch (SAXParseException e) {
            throw new JummpException("Could not parse PubMed information", e)
        }
        catch (Exception e) {
            throw new JummpException("Error retrieving publication info", e)
        }
        PublicationLinkProvider link = PublicationLinkProvider.withCriteria(uniqueResult: true) {
            eq("linkType", PublicationLinkProvider.LinkType.PUBMED)
        }
        PLPTC linkCommand = new PLPA(
                linkProvider: link).toCommandObject()
        PubTC publication = new PubTC(linkProvider:
                linkCommand, link: id)
        setFieldIfItExists("pages", publication, slurper.resultList.result.pageInfo, false)
        setFieldIfItExists("title", publication, slurper.resultList.result.title, false)
        setFieldIfItExists("affiliation", publication, slurper.resultList.result.affiliation, false)
        setFieldIfItExists("synopsis", publication, slurper.resultList.result.abstractText, false)

        if (slurper.resultList.result.journalInfo) {
            setFieldIfItExists("month", publication, slurper.resultList.result.journalInfo.monthOfPublication, true)
            setFieldIfItExists("year", publication, slurper.resultList.result.journalInfo.yearOfPublication, true)
            // cannot retrieve publication day directly like all other details
            def isoDateField = slurper.resultList.resultList.journalInfo.printPublicationDate
            if (isoDateField) {
                String isoDate = isoDateField.text()
                String[] dateParts = isoDate?.split('-')
                if (dateParts.length == 3) {
                    String dayAsString = dateParts[-1]
                    try {
                        publication.day = dayAsString as int
                    } catch (NumberFormatException ignored) {
                        log.warn "Invalid publication day $dayAsString for ${publication.link}"
                    }
                }
            }
            setFieldIfItExists("volume", publication, slurper.resultList.result.journalInfo.volume, false)
            setFieldIfItExists("issue", publication, slurper.resultList.result.journalInfo.issue, false)
            setFieldIfItExists("journal", publication, slurper.resultList.result.journalInfo.journal.title, false)
        }
        parseAuthors(slurper, publication)

        return publication
    }

    /**
     * Parses the author information provided from a JSON string and adds them to the given publication
     *
     * @param slurper The parsed XML document
     * @param publication The publication to add the authors to
     */
    private void parseAuthors(def slurper, PubTC publication) {
        publication.authors = []
        for (def authorXml in slurper.resultList.result.authorList.author) {
            PersonTC author = new PersonTC()
            if (authorXml.authorId[0]?.@type == "ORCID") {
                String orcid = authorXml.authorId[0].text()
                author.orcid = orcid
            }
            /**
             * Apparently, the full name should be combined from firstName and lastName
             * rather than populated from the fullName field.
             * The fullName field actually roles as the pubAlias property of PublicationPerson class
             *
             * TODO: capture the fullName, then assign it to the pubAlias property when we create an instance of
             * PublicationPerson from PersonTransportCommand in PublicationService
             */
            String userRealName = authorXml.fullName[0].text()
            author.userRealName = userRealName
            if (author.validate())
                publication.authors.add(author)
            else {
                String err = author.errors.allErrors.collect { e ->
                    messageSource.getMessage(e.code, author, null)
                }.join(';')
                String p = publication.prettierPrint()
                log.error("Validation error with author ${author.inspect()} of publication $p: $err")
            }
        }
    }
}
