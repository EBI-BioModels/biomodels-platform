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
* Spring Framework (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0 the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Framework used as well as
* that of the covered work.}
**/

package net.biomodels.jummp.core

import net.biomodels.jummp.core.adapters.DomainAdapter
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationPerson
import net.biomodels.jummp.model.PublicationLinkProvider
import org.springframework.validation.ObjectError
import org.xml.sax.SAXParseException
import org.springframework.transaction.annotation.Transactional
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.plugins.security.Person
import java.util.regex.Pattern
import java.util.regex.Matcher
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
/**
 * @short Service for fetching Publication Information for PubMed resources.
 *
 * This service class handles the interaction with the Web Service to retrieve
 * publication information for PubMed resources. It connects to citexplore and
 * parses the returned HTML page for the publication information.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class PubMedService {
    final Log log = LogFactory.getLog(getClass())
    static transactional = true

    PublicationTransportCommand getPublication(String id) throws JummpException {
        Publication publication = Publication.createCriteria().get() {
            eq("link",id)
            linkProvider {
                eq("linkType",PublicationLinkProvider.LinkType.PUBMED)
            }
        }
        if (publication) {
            return DomainAdapter.getAdapter(publication).toCommandObject();
        } else {
            return fetchPublicationData(id)
        }
    }

    PublicationTransportCommand getPublication(PublicationTransportCommand cmd) throws JummpException {
        PublicationLinkProvider.LinkType type =
                PublicationLinkProvider.LinkType.findLinkTypeByLabel(cmd.linkProvider.linkType)
        if (type == PublicationLinkProvider.LinkType.PUBMED) {
            return getPublication(cmd.link)
        }
        return null
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
      tmp.save(failOnError:true, flush: true);
     }


    private setFieldIfItExists(String fieldName, PublicationTransportCommand publication, def xmlField, boolean castToInt) {
        try
        {
            if (xmlField && xmlField.size()==1) {
                String text=xmlField.text()
                def fields=Publication.getFields()
                if (castToInt) {
                    publication."${fieldName}"=Integer.parseInt(text)
                }
                else {
                    publication."${fieldName}"=text
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * Downloads the XML describing the PubMed resource and parses the Publication information.
     * @param id The PubMed Identifier
     * @return A fully populated Publication
     */
    @SuppressWarnings("EmptyCatchBlock")
    @Transactional
    private PublicationTransportCommand fetchPublicationData(String id) throws JummpException {
        URL url
        try {
            url = new URL("http://www.ebi.ac.uk/europepmc/webservices/rest/search/query=ext_id:${id}%20src:med&resulttype=core")
        } catch (MalformedURLException e) {
            // TODO: throw a specific exception
            throw new JummpException("PubMed URL is malformed", e)
        }

        def slurper
        try {
            slurper = new XmlSlurper().parse(url.openStream())
        }
        catch (SAXParseException e) {
            throw new JummpException("Could not parse PubMed information", e)
        }
        catch (Exception e) {
            throw new JummpException("Error retrieving publication info", e)
        }
        PublicationLinkProvider link=PublicationLinkProvider.createCriteria().get() {
            eq("linkType",PublicationLinkProvider.LinkType.PUBMED)
        }
        PublicationTransportCommand publication = new PublicationTransportCommand(linkProvider: DomainAdapter.getAdapter(link).toCommandObject(), link: id)
        setFieldIfItExists("pages", publication, slurper.resultList.result.pageInfo, false)
        setFieldIfItExists("title", publication, slurper.resultList.result.title, false)
        setFieldIfItExists("affiliation", publication, slurper.resultList.result.affiliation, false)
        setFieldIfItExists("synopsis", publication, slurper.resultList.result.abstractText, false)

        if (slurper.resultList.result.journalInfo) {
            setFieldIfItExists("month", publication, slurper.resultList.result.journalInfo.monthOfPublication, true)
            setFieldIfItExists("year", publication, slurper.resultList.result.journalInfo.yearOfPublication, true)
            //setFieldIfItExists("day", publication, slurper.resultList.result.journalInfo.dateOfPublication, true) //we have integer, this returns a string
            setFieldIfItExists("volume", publication, slurper.resultList.result.journalInfo.volume, true)
            setFieldIfItExists("issue", publication, slurper.resultList.result.journalInfo.issue, true)
            setFieldIfItExists("journal", publication, slurper.resultList.result.journalInfo.journal.title, false)
        }
        parseAuthors(slurper, publication)

        return publication
    }

    /**
     * Parses the author information and adds it to @p publication
     * @param slurper The parsed XML document
     * @param publication The publication to add the authors to
     */
    private void parseAuthors(def slurper, PublicationTransportCommand publication) {
        publication.authors=[];
        for (def authorXml in slurper.resultList.result.authorList.author) {
            Person author = new Person()
            author.userRealName = authorXml.fullName[0].text()
            if (authorXml.authorId[0]?.@type=="ORCID") {
                author.orcid = authorXml.authorId[0].text()
            }
            publication.authors.add(author);
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
