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
 **/

package net.biomodels.jummp.core.adapters
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.PersonTransportCommand
import grails.util.Holders
/**
 * @short Adapter class for the Publication domain class
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
public class PublicationAdapter extends DomainAdapter {
    Publication publication

    def publicationService = Holders.getGrailsApplication().mainContext.publicationService

    PublicationTransportCommand toCommandObject() {
        PublicationTransportCommand pubTC = new PublicationTransportCommand(journal: publication.journal,
                title: publication.title,
                affiliation: publication.affiliation,
                synopsis: publication.synopsis,
                year: publication.year,
                month: publication.month,
                day: publication.day,
                volume: publication.volume,
                issue: publication.issue,
                pages: publication.pages,
                linkProvider: getAdapter(publication.linkProvider).toCommandObject(),
                link: publication.link,
                authors: new LinkedList<PersonTransportCommand>())
        def authors = publicationService.getPersons(publication)
        authors.each {
            PersonTransportCommand personAlias = getAdapter(it.person).toCommandObject()
            personAlias.userRealName = it.pubAlias
            pubTC.authors.add(personAlias)
        }
        return pubTC
    }
}
