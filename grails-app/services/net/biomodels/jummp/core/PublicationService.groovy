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
 **/





package net.biomodels.jummp.core

import net.biomodels.jummp.core.adapters.DomainAdapter
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter
import net.biomodels.jummp.core.model.PublicationDetailExtractionContext
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.model.PublicationPerson
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.PersonTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.validation.ObjectError
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @short Service that provides facilities for doing CRUD operations on publication.
 *
 * This service class is used to manage addition and update publication for model.
 * The methods implemented here are served in verifying publication link type, publication link,
 * managing authors, loading publication already saved in database or fetching it from external
 * resource, etc.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @date created on 08/06/2016.
 */


class PublicationService {
    final Log log = LogFactory.getLog(getClass())
    static transactional = false

    def pubMedService

    PublicationTransportCommand createPTCWithMinimalInformation(String pubLinkProvider,
                                                                String pubLink,
                                                                List<PersonTransportCommand> authors) {
        def provider = PublicationLinkProvider.LinkType.findLinkTypeByLabel(pubLinkProvider)
        PublicationTransportCommand retrieved = new PublicationTransportCommand()
        PublicationLinkProvider publicationLinkProvider = PublicationLinkProvider.createCriteria().get() {
            eq("linkType", provider)
        }
        retrieved.link = pubLink
        retrieved.linkProvider = DomainAdapter.getAdapter(publicationLinkProvider).toCommandObject()
        retrieved.authors = authors
        retrieved
    }

    boolean verifyLink(String linkTypeAsString, String link) {
        def linkProvider = PublicationLinkProvider.LinkType.findLinkTypeByLabel(linkTypeAsString)
        PublicationLinkProvider pubLinkProvider = PublicationLinkProvider.createCriteria().get() {
            eq("linkType", linkProvider)
        }
        if (!pubLinkProvider) {
            return false
        }
        if (PublicationLinkProvider.LinkType.MANUAL_ENTRY == pubLinkProvider.linkType) {
            return true
        }
        Pattern p = Pattern.compile(pubLinkProvider.pattern);
        Matcher m = p.matcher(link);
        return m.matches()
    }

    PublicationDetailExtractionContext getPublicationExtractionContext(PublicationTransportCommand cmd) throws JummpException {
        def linkType = PublicationLinkProvider.LinkType.findLinkTypeByLabel(cmd.linkProvider.linkType)
        Publication publication = Publication.createCriteria().get() {
            eq("link", cmd.link)
            linkProvider {
                eq("linkType", linkType)
            }
        }
        PublicationDetailExtractionContext ctx = new  PublicationDetailExtractionContext()
        if (publication) {
            // if existing in database
            ctx.publication = DomainAdapter.getAdapter(publication).toCommandObject()
            ctx.comesFromDatabase = true
        } else {
            // if not in database
            PublicationLinkProvider.LinkType type =
                PublicationLinkProvider.LinkType.findLinkTypeByLabel(cmd.linkProvider.linkType)
            // fetch from pubmed
            if (type == PublicationLinkProvider.LinkType.PUBMED) {
                ctx.publication = pubMedService.fetchPublicationData(cmd.link)
            } else {
                ctx.publication = null
            }
            ctx.comesFromDatabase = false
        }
        ctx
    }

    public List getPersons(Publication publication) {
        PublicationPerson.findAllByPublication(publication,
            [sort: "position", order: "asc"])
    }

    public void addPublicationAuthor(Publication publication,
                                     Person person,
                                     String realName,
                                     Integer position) {
        def tmp = new PublicationPerson(publication: publication,
            person: person,
            pubAlias: realName,
            position: position)
        if (!tmp.save(flush: true)) {
            log.error("""\
Failed to add author $person to $publication: ${tmp.errors.allErrors.inspect()}""")
        }
    }

    public void removePublicationAuthor(Publication publication, Person person) {
        def tobeDeleted = PublicationPerson.findByPublicationAndPerson(publication, person)
        if (tobeDeleted) {
            tobeDeleted.delete()
        }
    }

    private void reconcile(Publication publication, def tobeAdded) {
        def existing = getPersons(publication)
        tobeAdded.eachWithIndex { newAuthor, index ->
            def existingAuthor = existing.find { oldAuthor ->
                if (newAuthor.id) {
                    return newAuthor.id == oldAuthor.person.id
                }
                else if (newAuthor.orcid) {
                    return newAuthor.orcid == oldAuthor.person.orcid
                }
                return false
            }
            if (!existingAuthor) {
                Person newlyCreatedPubAuthor
                if (newAuthor.orcid) {
                    def personWithSameOrcid = Person.findByOrcid(newAuthor.orcid)
                    if (personWithSameOrcid) {
                        newlyCreatedPubAuthor = personWithSameOrcid
                    }
                }
                if (!newlyCreatedPubAuthor) {
                    newlyCreatedPubAuthor = new Person(userRealName: newAuthor.userRealName,
                        orcid: newAuthor.orcid)
                    newlyCreatedPubAuthor.save(failOnError: true, flush: true);
                }
                try {
                    addPublicationAuthor(publication, newlyCreatedPubAuthor, newAuthor.userRealName, index)
                }
                catch(Exception e) {
                    e.printStackTrace()
                }
            }
            else {
                if (existingAuthor.position != index) {
                    existingAuthor.position = index
                    existingAuthor.save()
                }
            }
        }

        existing.eachWithIndex{ PublicationPerson author, int index ->
            // find the authors will be remove out of the publication authors
            def willBeRemovedAuthor = tobeAdded.find { willBeAddedAuthor ->
                if (willBeAddedAuthor.id) {
                    return willBeAddedAuthor.id == author.person.id
                }
                else if (willBeAddedAuthor.orcid) {
                    return willBeAddedAuthor.orcid == author.person.orcid
                }
                return false
            } // return false means the author is not existing in the tobeAdded list
            if (!willBeRemovedAuthor) {
                removePublicationAuthor(publication, author.person)
            }
        }
    }

    Publication fromCommandObject(PublicationTransportCommand cmd) {
        Publication publication = Publication.createCriteria().get() {
            eq("link",cmd.link)
            linkProvider {
                eq("linkType", PublicationLinkProvider.LinkType.findLinkTypeByLabel(cmd.linkProvider.linkType))
            }
        }
        if (publication) {
            publication.title = cmd.title
            publication.affiliation = cmd.affiliation
            publication.synopsis = cmd.synopsis
            publication.journal = cmd.journal
            publication.year = cmd.year
            publication.month = cmd.month
            publication.day = cmd.day
            publication.volume = cmd.volume
            publication.issue = cmd.issue
            publication.pages = cmd.pages
            publication.save(flush: true)
            reconcile(publication, cmd.authors)
            return publication
        }
        Publication publ = new Publication(journal: cmd.journal,
            title: cmd.title,
            affiliation: cmd.affiliation,
            synopsis: cmd.synopsis,
            year: cmd.year,
            month: cmd.month,
            day: cmd.day,
            volume: cmd.volume,
            issue: cmd.issue,
            pages: cmd.pages,
            linkProvider: PublicationLinkProviderAdapter.fromCommandObject(cmd.linkProvider),
            link: cmd.link)
        if (publ.save(flush: true)) {
            reconcile(publ, cmd.authors)
        } else {
            StringBuilder err = new StringBuilder()
            publ.errors?.allErrors?.each { ObjectError e ->
                err.append(e.defaultMessage).append('. ')
            }
            log.error("Error encountered while saving publication ${publ.dump()}: $err".toString())
        }
        return publ
    }
}
