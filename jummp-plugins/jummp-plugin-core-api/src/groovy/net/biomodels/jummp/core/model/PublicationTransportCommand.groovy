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

import net.biomodels.jummp.plugins.security.PersonTransportCommand
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
    final Log log = LogFactory.getLog(PublicationTransportCommand.class)
    /**
     * Dependency injection of messageSource
     */
    def messageSource

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
    PublicationLinkProviderTransportCommand linkProvider
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
            return !authorValue.isEmpty()
        }
        link(nullable: true, unique: 'linkProvider')
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
     * @param slurper GPathResult object holding the author list
     */
    void parseAuthors(def slurper) {
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
            if (author.validate())
                this.authors.add(author)
            else {
                String err = author.errors.allErrors.collect { e ->
                    messageSource.getMessage(e.code, [author] as Object[], null)
                }.join(';')
                String p = this.prettierPrint()
                log.error("Validation error with author ${author.inspect()} of publication $p: $err")
            }
        }
    }
}
