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

import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.plugins.security.PersonTransportCommand

/**
 * @short Wrapper for a Publciation to be transported through JMS.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
@grails.validation.Validateable
class PublicationTransportCommand implements Serializable {
    private static final long serialVersionUID = 1L
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
    	// importFrom Publication ...would have been nice :(
    	// TODO: do we need more than 250 characters?
        journal(nullable: false, blank: false)
        // TODO: do we need more than 250 characters?
        title(nullable: false, blank: false)
        // TODO: do we need more than 250 characters?
        affiliation(nullable: false, blank: false)
        // TODO: How long can an abstract be? Are 5000 characters sufficient?
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
        Publication publication = Publication.findByLink(link)
        StringBuilder returnedText = new StringBuilder("")
        String linkTypeLabel = linkProvider.linkType
        returnedText.append("${linkTypeLabel}:<br/>&emsp;")
        returnedText.append(publication.link)
        returnedText.append("<br/>Title:<br/>&emsp;")
        returnedText.append(publication.title)
        returnedText.append("<br/>Abstract:<br/>&emsp;")
        returnedText.append(publication.synopsis)
        returnedText.toString()
    }
}
