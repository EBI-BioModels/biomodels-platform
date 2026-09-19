/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.transaction.Transactional
import groovy.json.JsonSlurper
import net.biomodels.jummp.core.adapters.PublicationAdapter
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter as PLPA
import net.biomodels.jummp.core.model.PublicationDetailExtractionContext as PDEC
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.core.user.PersonTransportCommand as PersonTC
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationLinkProvider as PLP
import net.biomodels.jummp.model.PublicationPerson
import net.biomodels.jummp.plugins.security.Person
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.acls.domain.BasePermission
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

class PublicationService implements IPublicationService, InitializingBean {
    final Log log = LogFactory.getLog(getClass())
    static transactional = false

    def doiService
    def pubMedService
    def messageSource
    def aclUtilService
    def springSecurityService

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Finished the bean initialisation")
    }

    /**
     * Whether the current user is a curator or admin, i.e. is allowed to manage any publication
     * through the standalone publication editor regardless of which model(s) it belongs to.
     */
    boolean isCurationStaff() {
        SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN,ROLE_CURATOR")
    }

    /**
     * Whether the current user may edit the given publication: curators/admins always can,
     * otherwise the user must have write access to at least one Model the publication belongs to.
     */
    boolean canManagePublication(Publication publication) {
        if (!publication) {
            return false
        }
        if (isCurationStaff()) {
            return true
        }
        Model.findAllByPublication(publication).any { Model model ->
            aclUtilService.hasPermission(springSecurityService.authentication, model, BasePermission.WRITE)
        }
    }

    List<PubTC> getAll() {
        List pubs = Publication.all
        List pubCmds= new ArrayList()
        pubs.each {
            pubCmds.add new PublicationAdapter(publication: it).toCommandObject()
        }
        pubCmds
    }

    PubTC createPTCWithMinimalInformation(String pubLinkProvider, String pubLink, List<PersonTC> authors) {
        def provider = PLP.LinkType.findLinkTypeByLabel(pubLinkProvider)
        PubTC retrieved = new PubTC()
        PLP publicationLinkProvider = PLP.withCriteria(uniqueResult: true) {
            eq("linkType", provider)
        } as PLP
        retrieved.link = pubLink
        retrieved.linkProvider = new PLPA(linkProvider:
                publicationLinkProvider).toCommandObject()
        retrieved.authors = authors
        retrieved
    }

    PubTC fetchPublicationData(final String linkTypeAsString, final String link) {
        PubTC pubTC = null
        PLP.LinkType type = PLP.LinkType.findLinkTypeByLabel(linkTypeAsString)

        if (type == PLP.LinkType.PUBMED || type == PLP.LinkType.DOI) {
            pubTC = pubMedService.fetchPublicationData(link, type)
            log.debug("The publication details of ${link} fetched from EuropePMC look ${pubTC?.dump()}")
            // a null result means EuropePMC could not be reached or answered with an error
            if (type == PLP.LinkType.DOI && (pubTC == null || pubTC.isEmpty())) {
                // fallback to DoiService if the entry hasn't indexed in PubMed centre yet
                try {
                    pubTC = doiService.fetchPublicationData(link)
                    log.debug("The publication details of ${link} fetched from https://doi.org look ${pubTC?.dump()}")
                } catch (JummpException je) {
                    log.error("""An errors occurred when fetching the publication metadata of \
${linkTypeAsString}:${link} due to ${je.message}""")
                    // do not hand back the empty EuropePMC record as if it were a real publication
                    pubTC = null
                }
            }
        }
        pubTC
    }

    PubTC findPublicationOfModel(final String modelId) {
        String queryStr = "from Model as m where m.publicationId = :modelId or m.submissionId = :modelId"
        List result = Model.executeQuery(queryStr, [modelId: modelId])
        Publication publication = result ? result.first()?.publication : null
        if (!publication) { return null }
        new PublicationAdapter(publication: publication).toCommandObject()
    }

    boolean verifyLink(String linkTypeAsString, String link) {
        def linkProvider = PLP.LinkType.findLinkTypeByLabel(linkTypeAsString)
        PLP pubLinkProvider = PLP.withCriteria(uniqueResult: true) {
            eq("linkType", linkProvider)
        } as PLP
        if (!pubLinkProvider) {
            return false
        }
        if (PLP.LinkType.MANUAL_ENTRY == pubLinkProvider.linkType) {
            return true
        }
        Pattern p = Pattern.compile(pubLinkProvider.pattern)
        Matcher m = p.matcher(link)
        return m.matches()
    }

    Map doVerifyPubLinkAndFetchData(String pubLinkProvider, String pubLink) {
        PubTC cmd = new PubTC()
        String message
        String status
        boolean comesFromDB = false
        if (pubLinkProvider == "NoPub" && pubLink) {
            message = "Please select a publication link type."
            status = "Failed"
        } else {
            if (!verifyLink(pubLinkProvider, pubLink)) {
                message = "The link is not a valid ${pubLinkProvider}"
                status = "Failed"
            } else {
                message = "The publication details have been fetched successfully."
                status = "OK"
                cmd = createPTCWithMinimalInformation(pubLinkProvider, pubLink, [])
                Map m = loadOrFetchOrCreatePublication(cmd, pubLinkProvider)
                PDEC ctx = m["pubCtx"] as PDEC
                if (m["message"]) { message += "<br/>" + m["message"] }
                // reassign cmd to a newly refreshed one
                cmd = ctx?.publication
                if (!cmd) {
                    status = "Unavailable"
                    message = "No record found. Please do check and try again."
                } else if (!cmd?.synopsis || !cmd?.affiliation || !cmd?.title) {
                    // for DOI fetched from DOI service or for PubMed entry not having any values for these fields
                    status = "Warning"
                    String t = cmd.linkProvider.linkType
                    String pubHref = "https://doi.org/${pubLink}"
                    if (t == PLP.LinkType.PUBMED.getLabel()) {
                        pubHref = "https://identifiers.org/pubmed:${pubLink}"
                    }
                    message = """The publication details are the best which our system can automatically
fetch from <a href="${pubHref}" target="_blank">${pubHref}</a>. Currently they are
missing a title, an affiliation and/or an abstract. Please verify the form and fill in the empty fields manually."""
                }

                comesFromDB = ctx?.comesFromDatabase
            }
        }
        ["message": message, "status": status, "publication": cmd, "comesFromDB": comesFromDB]
    }

    PLPTC inferPublicationLinkProvider(final String linkTypeAsString) {
        PLP.LinkType linkProvider = PLP.LinkType.findLinkTypeByLabel(linkTypeAsString)
        PLP pubLinkProvider = PLP.withCriteria(uniqueResult: true) {
            eq("linkType", linkProvider)
        } as PLP
        PLPTC transportCommand = new PLPA(linkProvider: pubLinkProvider).toCommandObject()
        return transportCommand
    }

    PDEC getPublicationExtractionContext(PubTC cmd) throws JummpException {
        Publication publication = findByPublicationTransportCommand(cmd)
        PDEC ctx = new  PDEC()
        PubTC pubTC
        if (publication) {
            // if existing in database
            pubTC = new PublicationAdapter(publication: publication).toCommandObject()
            ctx.publication = pubTC
            ctx.comesFromDatabase = true
        } else {
            // if not in database
            String type = cmd.linkProvider.linkType
            pubTC = fetchPublicationData(type, cmd?.link)
            ctx.publication = pubTC
            ctx.comesFromDatabase = false
        }
        ctx
    }

    List getPersons(Publication publication) {
        PublicationPerson.findAllByPublication(publication, [sort: "position", order: "asc"])
    }

    void addPublicationAuthor(Publication publication,
                                     Person person,
                                     String pubAlias,
                                     Integer position) {
        PublicationPerson tmp = new PublicationPerson(publication: publication,
            person: person,
            pubAlias: pubAlias,
            position: position)
        if (!tmp.save(flush: true)) {
            log.error("""\
Failed to add author $person to $publication: ${tmp.errors.allErrors.inspect()}""")
        }
    }

    void removePublicationAuthor(Publication publication, Person person) {
        def tobeDeleted = PublicationPerson.findByPublicationAndPerson(publication, person)
        if (tobeDeleted) {
            tobeDeleted.delete()
        }
    }

    @Transactional
    Publication fromCommandObject(PubTC cmd) {
        Publication publication = findByPublicationTransportCommand(cmd)
        if (publication) {
            String linkTypeLabel = cmd.linkProvider.linkType
            PLP.LinkType linkType = PLP.LinkType.findLinkTypeByLabel(linkTypeLabel)
            publication.linkProvider = PLP.findByLinkType(linkType)
            publication.link = cmd.link
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
        } else {
            publication = new Publication(journal: cmd.journal,
                title: cmd.title,
                affiliation: cmd.affiliation,
                synopsis: cmd.synopsis,
                year: cmd.year,
                month: cmd.month,
                day: cmd.day,
                volume: cmd.volume,
                issue: cmd.issue,
                pages: cmd.pages,
                linkProvider: PLPA.fromCommandObject(cmd.linkProvider),
                link: cmd.link)
        }
        if (publication.save(flush: true)) {
            reconcile(publication, cmd.authors)
        } else {
            StringBuilder err = new StringBuilder()
            publication.errors?.allErrors?.each { ObjectError e ->
                err.append(e.defaultMessage).append('. ')
            }
            log.error("Error encountered while saving publication ${publication.dump()}: $err".toString())
            publication = null
        }
        return publication
    }

    /**
     * Parses the publication authors come from a JSON string and fits them into a given publication transport command.
     *
     * @param cmd  The publication transport command
     * @param authorsAsJson A string representing the information of publication authors provided by submitters.
     * @return  An updated publication transport command
     */
    PubTC assembleAuthors(PubTC cmd, def authorsAsJson) throws InvalidPublicationAuthorsException {
        List<PersonTC> validatedAuthors
        validatedAuthors = parseAuthorsJSON(authorsAsJson)
        cmd.authors = validatedAuthors
        if (!cmd.validate()) {
            String authors = validatedAuthors == [] ? "[]" : validatedAuthors.collect {
                it.userRealName
            }.join("; ")
            String p  = cmd.toString()
            String error = cmd.errors.allErrors.collect { it }.join("; ")
            if (cmd.errors.hasFieldErrors("authors")) {
                error = cmd.errors.getFieldError("authors").rejectedValue
            }
            log.error("""\
There has been errors when assembling authors $authors into the publication '${p}' because the authors are $error.""")
        }
        cmd
    }

    Map buildPublicationFromJSONData(final String JSONData) {
        PubTC tempPTC = new PubTC()
        def pubDetails = JSON.parse(JSONData)
        bindJSONData(tempPTC, pubDetails)
        String linkTypeProvider = ""
        if (pubDetails?.linkProvider instanceof JSONObject) {
            linkTypeProvider = pubDetails.linkProvider.linkType
        } else if (pubDetails?.linkProvider instanceof String) {
            linkTypeProvider = pubDetails.linkProvider
        }
        tempPTC.linkProvider = inferPublicationLinkProvider(linkTypeProvider)
        String message
        String status
        List errors = new ArrayList()
        try  {
            assembleAuthors(tempPTC, pubDetails.authors)
            message = "Authors have been successfully assembled"
            status = "Success"
        } catch (InvalidPublicationAuthorsException e) {
            String errMsg = e.getI18nErrorMessage4InvalidAuthor()
            message = "There have been errors while parsing authors of the publication:<br/>${errMsg}"
            status = "Error"
        }
        if (tempPTC.hasErrors()) {
            def locale = Locale.getDefault()
            for (fieldErrors in tempPTC.errors) {
                for (error in fieldErrors.allErrors) {
                    message = messageSource.getMessage(error, locale)
                    errors.add(message)
                    log.error(message)
                }
            }
            status = "Error"
        }
        ["message": message, "status": status, "errors": errors, "publication": tempPTC] as Map
    }

    PubTC getById(Long id) {
        Publication publication = Publication.get(id)
        new PublicationAdapter(publication: publication).toCommandObject()
    }

    private Map loadOrFetchOrCreatePublication(PubTC pubTC, String pubLinkProvider) {
        String message = null
        PDEC publicationContext = null
        try {
            publicationContext = getPublicationExtractionContext(pubTC)
            if (publicationContext.publication) {
                if (publicationContext.comesFromDatabase) {
                    String code = "publication.editor.duplicateEntry.message"
                    String pubLink = pubTC.link
                    message = messageSource.getMessage(code, [pubLink] as Object[], Locale.default)
                    log.debug(message)
                }
            } else {
                PubTC retrieved
                retrieved = createPTCWithMinimalInformation(pubLinkProvider, pubTC.link, [])
                publicationContext.publication = retrieved
                publicationContext.comesFromDatabase = false
            }
        } catch (Exception e) {
            message = e.message
            log.error(message, e)
        } finally {
            log.info("Finished loading and creating a publication holder for...")
        }
        return [message: message, pubCtx: publicationContext]
    }

    private static PubTC bindJSONData(PubTC pubTC, def jsonData) {
        pubTC.link = jsonData.link
        pubTC.title = jsonData.title
        pubTC.journal = jsonData.journal
        pubTC.affiliation = jsonData.affiliation
        pubTC.synopsis = jsonData.synopsis
        pubTC.year = jsonData.year as Integer
        pubTC.month = jsonData.month
        pubTC.volume = jsonData.volume
        pubTC.issue = jsonData.issue
        pubTC.pages = jsonData.pages
        pubTC
    }

    private void reconcile(Publication publication, List<PersonTC> tobeAdded) {
        // get rid of duplications if they exist. Filtering duplications is often processed at the client-side.
        // However, duplications could be bypassed for whatever reason. We handle such duplications here just in case.
        List noDupList = tobeAdded
        tobeAdded = []
        noDupList.eachWithIndex { PersonTC entry, int i ->
            def personAdded = tobeAdded.find { PersonTC person ->
                person.userRealName.trim() == entry.userRealName.trim() && person.orcid?.trim() == entry.orcid?.trim() && person.institution?.trim() == entry.institution?.trim()
            }
            if (!personAdded) {
                tobeAdded.add(entry)
            }
        }

        List<PublicationPerson> existing = getPersons(publication)
        existing.eachWithIndex { PublicationPerson author, int index ->
            // find the authors will be remove out of the publication authors
            def willBeRemovedAuthor = tobeAdded.find { willBeAddedAuthor ->
                if (willBeAddedAuthor.id) {
                    return willBeAddedAuthor.id == author.person.id
                } else if (willBeAddedAuthor.orcid) {
                    return willBeAddedAuthor.orcid == author.person.orcid
                } else if (willBeAddedAuthor.userRealName || willBeAddedAuthor.institution) {
                    return willBeAddedAuthor.userRealName == author.person.userRealName &&
                        willBeAddedAuthor.institution == author.person.institution
                }
                return false
            } // return false means the author is not existing in the tobeAdded list
            if (!willBeRemovedAuthor) {
                removePublicationAuthor(publication, author.person)
            }
        }
        tobeAdded.eachWithIndex { PersonTC newAuthor, Integer index ->
            def existingAuthor = existing.find { PublicationPerson oldAuthor ->
                if (newAuthor.id) {
                    return newAuthor.id == oldAuthor.person.id
                } else if (newAuthor.orcid) {
                    return newAuthor.orcid == oldAuthor.person.orcid
                } else {
                    return newAuthor.userRealName == oldAuthor.person.userRealName &&
                        newAuthor.institution == oldAuthor.person.institution
                }
            }
            if (!existingAuthor) {
                Person newlyCreatedPubAuthor
                if (newAuthor.orcid) {
                    newlyCreatedPubAuthor = Person.findOrCreateByOrcid(newAuthor.orcid)
                } else {
                    newlyCreatedPubAuthor = Person.findOrCreateWhere(userRealName: newAuthor.userRealName,
                        orcid: newAuthor.orcid, institution: newAuthor.institution)
                }
                if (!newlyCreatedPubAuthor.id && newlyCreatedPubAuthor.orcid) {
                    newlyCreatedPubAuthor.userRealName = newAuthor.userRealName
                    newlyCreatedPubAuthor.institution = newAuthor.institution
                }

                if (newlyCreatedPubAuthor.save(flush: true)) {
                    addPublicationAuthor(publication, newlyCreatedPubAuthor, newlyCreatedPubAuthor.userRealName, index)
                } else {
                    /**
                     * The `transactionStatus` is an implicit variable defined and made available thanks to the AST
                     * transformation in Grails Groovy. It could be applied to transactional methods during the
                     * compilation process. The minimal example of how the transactional roll back works can be found
                     * at the link https://bitbucket.org/MihaiGlont/grails2-rollback-demo/
                     */
                    transactionStatus.setRollbackOnly()
                    def a = newlyCreatedPubAuthor.userRealName
                    def err = newlyCreatedPubAuthor.errors.allErrors.collect { e ->
                        messageSource.getMessage(e.code, [newlyCreatedPubAuthor] as Object[], null)
                    }.join(';')
                    def p = publication.linkProvider.linkType == PLP.LinkType.MANUAL_ENTRY ?
                        "${publication.title}" : "${publication.link}"
                    log.error("Author $a could not be saved due to $err. Publication '$p' will be rolled back")
                }
            } else {
                // If the position of authors have been updated
                if (existingAuthor.position != index ||
                    existingAuthor.person.institution != newAuthor.institution ||
                    existingAuthor.pubAlias != newAuthor.userRealName) {
                    String query = """update PublicationPerson pp
set pp.position = :newPosition, pp.pubAlias = :newPubAlias
where pp.publication = :publication and pp.person = :person and pp.position = :oldPosition"""
                    Map parameters = [newPosition: index,
                                      newPubAlias: newAuthor.userRealName,
                                      publication: existingAuthor.publication,
                                      person     : existingAuthor.person,
                                      oldPosition: existingAuthor.position]
                    PublicationPerson.executeUpdate(query, parameters)
                }
                // If authors' else properties have been changed
                if (existingAuthor.person.userRealName != newAuthor.userRealName ||
                    existingAuthor.person.orcid != newAuthor.orcid ||
                    existingAuthor.person.institution != newAuthor.institution) {
                    Person person = existingAuthor.person
                    person.userRealName = newAuthor.userRealName
                    person.orcid = newAuthor.orcid
                    person.institution = newAuthor.institution
                    if (!person.save(flush: true)) {
                        log.error("Cannot saved the updates of the person ${person.errors.allErrors.inspect()}")
                    }
                }
            }
        }
    }

    private static List<PersonTC> parseAuthorsJSON(def jsonData) {
        List<PersonTC> validatedAuthors = new LinkedList<>()
        def authorList
        if (jsonData instanceof String) {
            def slurper = new JsonSlurper()
            def parsedJson = slurper.parseText(jsonData)
            if (!parsedJson['authors']) {
                return []
            }
            authorList = parsedJson['authors']
        } else if (jsonData instanceof JSONArray) {
            authorList = jsonData
        }
        InvalidPublicationAuthorsException invalidAuthorsException = new InvalidPublicationAuthorsException()
        for (Object authorJson : authorList) {
            if (!authorJson) {
                continue // skip this record
            }
            Long id = authorJson["id"] ?: null
            String name = authorJson["userRealName"]
            String institution = authorJson["institution"] ?: null
            String orcid = authorJson["orcid"] ?: null
            PersonTC author = new PersonTC(id: id, userRealName: name, orcid: orcid, institution: institution)
            if (author.validate()) {
                validatedAuthors.add(author)
            } else {
                // this person record is invalid
                invalidAuthorsException.addInvalidPublicationAuthor(author)
            }
        }
        int nbBadAuthors = invalidAuthorsException.getInvalidAuthors()?.size()
        if (nbBadAuthors > 0) {
            // throw a checked exception that is caught downstream -- e.g. in ModelController
            throw invalidAuthorsException
        }
        return validatedAuthors
    }

    private Publication findByPublicationTransportCommand(PubTC cmd) {
        Publication publication = null
        if (cmd?.id) {
            publication = Publication.get(cmd.id)
        } else if (cmd?.link) {
            PLP.LinkType linkType = PLP.LinkType.findLinkTypeByLabel(cmd.linkProvider.linkType)
            publication = Publication.withCriteria(uniqueResult: true) {
                eq("link", cmd.link)
                linkProvider {
                    eq("linkType", linkType)
                }
            } as Publication
        }
        publication
    }
}
