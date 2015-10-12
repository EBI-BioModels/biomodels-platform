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
* groovy, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of groovy, Spring Security used as well as
* that of the covered work.}
**/

import grails.util.Environment
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PubLinkProvTC
import net.biomodels.jummp.core.model.identifier.decorator.AbstractAppendingDecorator
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin

class BootStrap {
    def springSecurityService
    def wcmSecurityService
    def searchableService
    def grailsApplication

    void addPublicationLinkProvider(PubLinkProvTC cmd) {
        def publinkType=PublicationLinkProvider.LinkType.valueOf(cmd.linkType)
        if (!PublicationLinkProvider.findByLinkType(publinkType)) {
            def publinkprov = new PublicationLinkProvider(linkType: publinkType,
                        pattern:cmd.pattern, identifiersPrefix: cmd.identifiersPrefix)
            publinkprov.save(flush: true)
        }
    }

    def init = { servletContext ->
        ModelFormat format = ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*")
        if (!format) {
            format = new ModelFormat(identifier: "UNKNOWN", name: "Unknown format", formatVersion: "*")
            format.save(flush: true)
        }
        def ctx = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)
        def service = ctx.getBean("modelFileFormatService")
        def modelFormat = service.registerModelFormat("UNKNOWN", "UNKNOWN")
        service.handleModelFormat(modelFormat, "unknownFormatService", "unknown")

         grailsApplication.domainClasses.each { GrailsClass gc ->
             DomainClassGrailsPlugin.addValidationMethods(grailsApplication, gc,
                    grailsApplication.mainContext)
        }

        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.PUBMED,
                         pattern:"^\\d+",
                         identifiersPrefix:"http://identifiers.org/pubmed/"))

        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.DOI,
                         pattern:"^(doi\\:)?\\d{2}\\.\\d{4}.*",
                         identifiersPrefix:"http://identifiers.org/doi/"))

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

        addPublicationLinkProvider(new PubLinkProvTC(linkType:PublicationLinkProvider.LinkType.CUSTOM,
                         pattern:"^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",))
        if (Environment.getCurrent() != Environment.TEST) {
             if (!Role.findByAuthority("ROLE_USER")) {
                new Role(authority: "ROLE_USER").save(flush: true)
            }
            if (!Role.findByAuthority("ROLE_CURATOR")) {
                new Role(authority: "ROLE_CURATOR").save(flush: true)
            }
            if (!Role.findByAuthority("ROLE_ADMIN")) {
                new Role(authority: "ROLE_ADMIN").save(flush: true)
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
                if (!Role.findByAuthority("ROLE_ADMIN")) {
                    new Role(authority: "ROLE_ADMIN").save(flush: true)
                }
                userRole = Role.findByAuthority("ROLE_ADMIN")
                UserRole.create(user, userRole, true)
            }
            // Manually start Searchable's mirroring process to ensure that it comes after the automated migrations.
            //searchableService.reindex()
            searchableService.startMirroring()
        }

        // custom mapping for weceem as it fails to work with an LDAPUserDetailsImpl
        wcmSecurityService.securityDelegate = [
            getUserName : { ->
                if (springSecurityService.isLoggedIn()) {
                    return springSecurityService.principal.username
                } else {
                    return null
                }
            },
            getUserEmail : { ->
                return null
            },
            getUserRoles : { ->
                if (springSecurityService.isLoggedIn()) {
                    return springSecurityService.principal.authorities
                } else {
                    return ['ROLE_GUEST']
                }
            },
            getUserPrincipal : { ->
                def principal = springSecurityService.getPrincipal()
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    return new org.springframework.security.core.userdetails.UserDetails() {
                        Collection<org.springframework.security.core.GrantedAuthority> getAuthorities() {
                            return principal.authorities
                        }
                        String getPassword() {
                            return principal.password
                        }
                        String getUsername() {
                            return principal.username
                        }
                        boolean isAccountNonExpired() {
                            return principal.isAccountNonExpired()
                        }
                        boolean isAccountNonLocked() {
                            return principal.isAccountNonLocked()
                        }
                        boolean isCredentialsNonExpired() {
                            return principal.isCredentialsNonExpired()
                        }
                        boolean isEnabled() {
                            return principal.isEnabled()
                        }
                        String getEmail() {
                            return null
                        }
                        String getFirstName() {
                            return null
                        }
                        String getLastName() {
                            return null
                        }
                    }
                } else {
                    return principal
                }
            }
        ]
        AbstractAppendingDecorator.context = ctx
        RevisionTransportCommand.context = ctx
    }

    def destroy = {
    }
}
