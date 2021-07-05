/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* groovy (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of groovy used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core.model

import groovy.util.slurpersupport.GPathResult
import net.biomodels.jummp.core.user.PersonTransportCommand
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Wrapper for a Publication to be transported through JMS.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
@grails.validation.Validateable
class PublicationTransportCommand implements Serializable {
    private static final long serialVersionUID = 1L
    /**
     * The class logger
     */
    private final Log log = LogFactory.getLog(PublicationTransportCommand.class)

    Long id
    /**
     * Name of the journal where the publication has been published
     */
    String journal
    /**
     * The title of the publication.
     */
    String title
    /**
     * The authors' affiliation.
     */
    String affiliation
    /**
     * The abstract of the publication.
     */
    String synopsis
    // TODO: merge date fields into one property and store a format property (only year, year/month, complete date)
    /**
     * The year the Journal issue has been published.
     */
    Integer year
    /**
     * The month the Journal issue has been published.
     */
    String month
    /**
     * The day the Journal issue has been published.
     */
    Integer day
    /**
     * The volume of the Journal issue.
     */
    String volume
    /**
     * The issue of the Journal the publication has been published in.
     */
    String issue
    /**
     * The pages of the publication in the Journal Issue.
     */
    String pages
    /**
     * The provider of the publication id (e.g. PubMed)
     */
    PLPTC linkProvider
    /**
     * The key to the publication at the linkProvider or a URL
     */
    String link
    List<PersonTransportCommand> authors

    static constraints = {
        id(nullable: true)
        // importFrom Publication ...would have been nice :(
        journal(nullable: false, blank: false)
        title(nullable: false, blank: false)
        affiliation(nullable: false, blank: false)
        synopsis(nullable: false, blank: true, maxSize: 5000)
        year(nullable: true)
        month(nullable: true)
        day(nullable: true)
        volume(nullable: true)
        issue(nullable: true)
        pages(nullable: true)
        authors nullable: false, validator: { authorValue, pubObj ->
            return !authorValue.isEmpty() && !authorValue.find { !it.validate() }
        }
        link(nullable: true, unique: 'linkProvider')
    }

    static PublicationTransportCommand fromDOI(PublicationTransportCommand pubTC) {
        return pubTC
    }

    static PublicationTransportCommand fromPubMed(PLPTC linkProvider, String pmid, GPathResult xml) {
        PublicationTransportCommand publication =
            new PublicationTransportCommand(linkProvider: linkProvider, link: pmid)
        publication.extractManuscriptInfoFromPubMed(xml)
        publication.extractAuthorsFromPubMed(xml)
        publication
    }

    String prettierPrint() {
        StringBuilder returnedText = new StringBuilder("")
        String linkTypeLabel = linkProvider ? linkProvider.linkType : ""
        if (linkTypeLabel) {
            returnedText.append("${linkTypeLabel}:<br/>&emsp;")
            returnedText.append(link)
        }
        if (title) {
            returnedText.append("<br/>Title:<br/>&emsp;")
            returnedText.append(title)
        }
        if (synopsis) {
            returnedText.append("<br/>Abstract:<br/>&emsp;")
            returnedText.append(synopsis)
        }
        returnedText.toString()
    }


    /**
     * Parses the author information provided from a GPathResult object and adds them to this publication
     *
     * @param slurper GPathResult object holding the author list which is fetched from PubMed Central
     */
    void extractAuthorsFromPubMed(def slurper) {
        authors = new ArrayList<>()
        def authorsXml = slurper.resultList.result.authorList.author
        for (def authorXml in authorsXml) {
            PersonTransportCommand author = new PersonTransportCommand()
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
            log.debug("Author: ${author?.userRealName}")
            String affiliation = authorXml.affiliation[0].text()
            author.institution = affiliation
            this.authors.add(author)
        }
    }

    void extractManuscriptInfoFromPubMed(def slurper) {
        def result = slurper.resultList.result
        setFieldIfItExists("pages", result.pageInfo, false)
        setFieldIfItExists("title", result.title, false)
        setFieldIfItExists("affiliation", result.affiliation, false)
        setFieldIfItExists("synopsis", result.abstractText, false)

        if (result.journalInfo) {
            setFieldIfItExists("month", result.journalInfo.monthOfPublication, true)
            setFieldIfItExists("year", result.journalInfo.yearOfPublication, true)
            // cannot retrieve publication day directly like all other details
            def isoDateField = slurper.resultList.resultList.journalInfo.printPublicationDate
            if (isoDateField) {
                String isoDate = isoDateField.text()
                String[] dateParts = isoDate?.split('-')
                if (dateParts.length == 3) {
                    String dayAsString = dateParts[-1]
                    try {
                        day = dayAsString as int
                    } catch (NumberFormatException ignored) {
                        log.warn("Invalid publication day $dayAsString for ${link}")
                    }
                }
            }
            setFieldIfItExists("volume", result.journalInfo.volume, false)
            setFieldIfItExists("issue", result.journalInfo.issue, false)
            setFieldIfItExists("journal", result.journalInfo.journal.title, false)
        }
    }

    boolean isEmpty() {
        // Both two primary required fields are empty
        title == null && journal == null
    }

    private void setFieldIfItExists(String fieldName, def xmlField, boolean castToInt) {
        try {
            if (xmlField && xmlField.size() == 1) {
                String text = xmlField.text()
                if (castToInt) {
                    try {
                        this."${fieldName}" = text as int
                    } catch (NumberFormatException ignored) {
                        final String pId = link
                        log.warn("Field '$fieldName' of publication $pId is not numerical: $text")
                    }
                }
                else {
                    this."${fieldName}" = text
                }
            }
        } catch(Exception e) {
            log.error(e.message, e)
        }
    }
}
