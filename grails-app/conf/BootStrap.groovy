/**
* Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* groovy, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of groovy, Spring Security used as well as
* that of the covered work.}
**/


import grails.plugin.springsecurity.acl.AclSid
import grails.plugins.rest.client.RestBuilder
import grails.util.Environment
import net.biomodels.jummp.core.adapters.ModelFormatAdapter
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PubLinkProvTC
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.healthcheck.HealthCheckUtil
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.model.ContributionRole
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BootStrap {
    private final Logger LOGGER = LoggerFactory.getLogger(BootStrap.class)
    def contributorService
    def springSecurityService
    def grailsApplication
    def modelFileFormatService
    def idGeneratorRegistryFactoryBean
    def subscribeClientService

    void doInitialiseSomeUsersAndRoles() {
        if (Environment.getCurrent() != Environment.TEST) {
            ["ROLE_USER", "ROLE_CURATOR", "ROLE_ADMIN", "ROLE_QC_PROVIDER", "ROLE_REVIEWER"].each {
                if (!Role.findByAuthority(it)) {
                    new Role(authority: it).save(flush: true)
                }
            }
            if (!User.findByUsername("administrator")) {
                def person = new Person(userRealName: "administrator")
                person.save(flush: true)
                def user = new User(username: "administrator",
                    password: springSecurityService.encodePassword("administrator"),
                    email: "user@test.com",
                    person: person,
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
                user.save(flush: true)
                new AclSid(sid: user.username, principal: true).save(flush: true)
                Role userRole = Role.findByAuthority("ROLE_USER")
                UserRole.create(user, userRole, true)
                userRole = Role.findByAuthority("ROLE_ADMIN")
                UserRole.create(user, userRole, true)
            }

            if (!User.findByUsername("anonymous")) {
                def person = new Person(userRealName: "anonymous")
                person.save(flush: true)
                def user = new User(username: "anonymous",
                    password: springSecurityService.encodePassword("anonymous"),
                    email: "user@anoymous.com",
                    person: person,
                    enabled: false,
                    accountExpired: true,
                    accountLocked: true,
                    passwordExpired: true)
                user.save(flush: true)
            }
        }
    }

    void doInitialiseSomeContributionRoles() {
        Map<String, String> roles = [
            "Curator": "Any person who has contributed to update, correct and submit your model files",
            "Modeller": "Any person who has made a significant contribution to model submission",
            "Other": "Any person who has made any amount of contribution to your submission",
        ]
        if (Environment.getCurrent() != Environment.TEST) {
            roles.each {
                if (!ContributionRole.findByName(it.key)) {
                  new ContributionRole(name: it.key, description: it.value).save(flush: true)
                }
            }
        }
    }

    void addPublicationLinkProvider(PubLinkProvTC cmd) {
        def publinkType=PublicationLinkProvider.LinkType.valueOf(cmd.linkType)
        if (!PublicationLinkProvider.findByLinkType(publinkType)) {
            def publinkprov = new PublicationLinkProvider(linkType: publinkType,
                        pattern:cmd.pattern, identifiersPrefix: cmd.identifiersPrefix)
            publinkprov.save(flush: true)
        }
    }

    void doInitialisePublicationLinkProvider() {
        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.PUBMED,
            pattern:"^\\d+",
            identifiersPrefix:"http://identifiers.org/pubmed/"))

        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.DOI,
            pattern:"^(doi\\:)?\\d{2}\\.\\d{4}.*",
            identifiersPrefix:"http://identifiers.org/doi/"))

        /* ignore until we fix the integration with the annotation UI.
        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.ARXIV,
                         pattern:"^(\\w+(\\-\\w+)?(\\.\\w+)?/)?\\d{4,7}(\\.\\d{4}(v\\d+)?)?",
                         identifiersPrefix:"http://identifiers.org/arxiv/"))

        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.ISBN,
                         pattern:"^(ISBN)?(-13|-10)?[:]?[ ]?(\\d{2,3}[ -]?)?\\d{1,5}[ -]?\\d{1,7}[ -]?\\d{1,6}[ -]?(\\d|X)",
                         identifiersPrefix:"http://identifiers.org/isbn/"))

         addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.ISSN,
                         pattern:"^\\d{4}\\-\\d{4}",
                         identifiersPrefix:"http://identifiers.org/issn/"))

         addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.JSTOR,
                         pattern:"^\\d+",
                         identifiersPrefix:"http://identifiers.org/jstor/"))

         addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.NARCIS,
                         pattern:"^oai\\:cwi\\.nl\\:\\d+",
                         identifiersPrefix:"http://identifiers.org/narcis/"))

        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.NBN,
                        pattern:"^urn\\:nbn\\:[A-Za-z_0-9]+\\:([A-Za-z_0-9]+\\:)?[A-Za-z_0-9]+",
                        identifiersPrefix:"http://identifiers.org/nbn/"))

        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.PMC,
                         pattern:"PMC\\d+",
                         identifiersPrefix:"http://identifiers.org/pmc/"))
        */
        addPublicationLinkProvider(new PubLinkProvTC(linkType: PublicationLinkProvider.LinkType.CUSTOM,
            pattern: "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))
        addPublicationLinkProvider(new PubLinkProvTC(linkType: PublicationLinkProvider.LinkType.MANUAL_ENTRY,
            pattern: "\\A\\z" /* i.e. start of input then end of input -- ignored */))
    }

    void registerDefaultModelElementTypes() {
        def modelFormats = ModelFormat.list().each { ModelFormat fmt ->
            def fmtCmd = new ModelFormatAdapter(format: fmt).toCommandObject()
            try {
                modelFileFormatService.registerModelElementType(fmtCmd, "model")
            } catch (IllegalStateException e) {
                String id = fmt.identifier
                String v = fmt.formatVersion
                println "Cannot register default model element type for $id $v"
            }
        }
    }

    void doInitialiseModelFormatAndRelated() {
        ModelFormat format = ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*")
        if (!format) {
            format = new ModelFormat(identifier: "UNKNOWN", name: "Other", formatVersion: "*")
            format.save(flush: true)
        }
        def modelFormat = modelFileFormatService.registerModelFormat("UNKNOWN", "UNKNOWN")

        modelFileFormatService.handleModelFormat(modelFormat, "unknownFormatService", "unknown")
    }

    void doAddValidationMethods2DomainClass() {
        grailsApplication.domainClasses.each { GrailsClass gc ->
            DomainClassGrailsPlugin.addValidationMethods(grailsApplication, gc,
                grailsApplication.mainContext)
        }
    }

    void doCustomiseRestBuilderConstructor() {
        // Below is the provisional solution as suggested at
        // https://github.com/grails-plugins/grails-rest-client-builder/issues/40
        RestBuilder.metaClass.constructor = { ->
            def constructor = RestBuilder.class.getConstructor()
            def instance = constructor.newInstance()
            instance.restTemplate.messageConverters.removeAll {
                it.class.name == 'org.springframework.http.converter.json.GsonHttpMessageConverter' }
            instance
        }
    }

    void doSubscribeRedisChannelsRelated2ModelIdentifierGeneration() {
        subscribeClientService.init()
        LOGGER.debug("Background process successfully started")
    }

    void doDestroyRedisPubSubClients() {
        subscribeClientService.destroy()
    }

    def init = { servletContext ->
        HealthCheckUtil.registerObjectMarshaller()

        def generatorRegistry = idGeneratorRegistryFactoryBean.object
        println "Using model id generators ${generatorRegistry?.generatorMap}"

        def ctx = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)
        RevisionTransportCommand.context = ctx
        doInitialiseModelFormatAndRelated()
        registerDefaultModelElementTypes()
        doAddValidationMethods2DomainClass()
        doInitialisePublicationLinkProvider()
        doInitialiseSomeUsersAndRoles()
        doInitialiseSomeContributionRoles()
        doCustomiseRestBuilderConstructor()
        doSubscribeRedisChannelsRelated2ModelIdentifierGeneration()

        contributorService.init()
    }

    def destroy = { servletContext ->
        doDestroyRedisPubSubClients()
        LOGGER.debug("Background process successfully destroyed")
    }
}
