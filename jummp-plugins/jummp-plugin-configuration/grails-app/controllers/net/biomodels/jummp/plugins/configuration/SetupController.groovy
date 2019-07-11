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





package net.biomodels.jummp.plugins.configuration

import net.biomodels.jummp.core.user.UserCommand

/**
 * @short Controller to bootstrap the configuration of an application instance.
 *
 * The controller consists of two webflows:
 * @li @c setupFlow to configure database and authentication
 * @li @c firstRunFlow to create the admin user after the database is configured
 *
 * The configuration is stored into the file @c ~/.jummp.properties and used by
 * the Grails config.
 * The SetupFilters takes care to redirect all requests to the currently required
 * flow and blocks all access to this controller as soon as the application instance
 * is configured.
 * @see net.biomodels.jummp.filters.SetupFilters
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @date 20130705
 */
class SetupController {
    def configurationService
    def springSecurityService
    def userService

    def index = {
        redirect(action: "setup")
    }

    def setupFlow = {
        start {
            on("next") { DatabaseCommand cmd ->
                flow.database = cmd
                if (flow.database.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("authenticationBackend")
        }

        authenticationBackend {
            on("next").to("validateAuthenticationBackend")
            on("back").to("start")
        }
        
        validateAuthenticationBackend {
            action {
                if (params.authenticationBackend == "database") {
                	flow.authenticationBackend = "database"
                    return database()
                } else if (params.authenticationBackend == "ldap") {
                    flow.validationErrorOn="LDAP is currently not supported"
                	error()
                } else {
                    error()
                }
            }
            on("database").to("vcs")
            on("ldap").to("ldap")
            on("error").to("authenticationBackend")
        }
        
        ldap {
            on("next") { LdapCommand cmd ->
                flow.ldap = cmd
                if (flow.ldap.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("vcs")
            on("back").to("authenticationBackend")
        }
        
        vcs {
            on("next") { VcsCommand cmd ->
                flow.vcs = cmd
                if (flow.vcs.hasErrors()) {
                    error()
                } 
            }.to("branchOnVcsType")
            on("back").to("decideBackFromVcs")
        }
        
        branchOnVcsType {
            action { 
            	if (flow.vcs.isGit()) {
                    git()
                } 
                else if (flow.vcs.isSvn()) {
                    svn()
                }
            }
            on("svn").to("svn")
            on("git").to("userRegistration")
            on(Exception).to("exception")
        }
        
        svn {
            on("next") { SvnCommand cmd ->
                flow.svn = cmd
                if (flow.svn.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("firstRun")
            on("back").to("vcs")
        }
        /* Disabled as it adds nothing*/
        git {
            on("next").to("firstRun")
            on("back").to("vcs")
        }
        /* Disabled as admins created by config.groovy*/
        firstRun {
            on("next") { FirstRunCommand cmd ->
                flow.firstRun = cmd
                if (flow.firstRun.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("userRegistration")
            on("back").to("vcs")
        }

        userRegistration {
            on("next") { UserRegistrationCommand cmd ->
                flow.userRegistration = cmd
                if (flow.userRegistration.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("remoteExport")
            on("back").to("vcs")
        }
        /* Disabled */
        changePassword {
            on("next") { ChangePasswordCommand cmd ->
                flow.changePassword = cmd
                if (flow.changePassword.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("remoteExport")
            on("back").to("userRegistration")
        }

        remoteExport {
            on("next") { RemoteCommand cmd ->
                flow.remote = cmd
                if (flow.remote.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("validateRemote")
            on("back").to("userRegistration")
        }

        remoteRemote {
            on("next") { RemoteCommand cmd ->
                cmd.jummpExportJms = flow.remote.jummpExportJms
                flow.remote = cmd
                flow.remote.validate()
                if (flow.remote.hasErrors()) {
                    return error()
                } else  {
                    return success()
                }
            }.to("server")
            on("back").to("remoteExport")
        }

        server {
            on("next") { ServerCommand cmd ->
                flow.server = cmd
                if (flow.server.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("trigger")
            on("back").to("remoteExport")
        }

        trigger {
            on("next") { TriggerCommand cmd ->
                flow.trigger = cmd
                if (flow.trigger.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("mail")
            on("back").to("server")
        }
        
        mail {
            on("next") { MailCommand cmd ->
                flow.mail = cmd
                if (flow.mail.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("search")
            on("back").to("trigger")
        }
        
        search {
            on("next") { SearchCommand cmd ->
                flow.search = cmd
                if (flow.search.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("sbml")
            on("back").to("mail")
        }

        sbml {
            on("next") { SBMLCommand cmd ->
                flow.sbml = cmd
                if (flow.sbml.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("cms")
            on("back").to("search")
        }
        /* Disabled */
        bives {
            on("next") { BivesCommand cmd ->
                flow.bives = cmd
                if (flow.bives.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("cms")
            on("back").to("sbml")
        }

        cms {
            on("next") { CmsCommand cmd ->
                flow.cms = cmd
                if (flow.cms.hasErrors()) {
                    return error()
                } else {
                    configurationService.storeConfiguration(flow.database, 
                    										(flow.authenticationBackend == "ldap") ? flow.ldap : null, 
                    										flow.vcs, flow.svn, flow.firstRun, 
                    										flow.server, flow.userRegistration, flow.changePassword?:null, 
                    										flow.remote, flow.trigger, flow.sbml, flow.bives, 
                    										flow.cms, flow.branding?:null, 
                    										flow.search, flow.mail)
                    return success()
                }
            }.to("finish")
            on("back").to("sbml")
        }
        /* Disabled */
        branding {
            on("next") { BrandingCommand cmd ->
                flow.branding = cmd
                if (flow.branding.hasErrors()) {
                    return error()
                } else {
                    configurationService.storeConfiguration(flow.database, (flow.authenticationBackend == "ldap") ? flow.ldap : null, flow.vcs, flow.svn, flow.firstRun, flow.server, flow.userRegistration, flow.changePassword, flow.remote, flow.trigger, flow.sbml, flow.bives, flow.cms, flow.branding)
                    return success()
                }
            }.to("finish")
            on("back").to("cms")
        }
        validateRemote {
            action {
                if (flow.remote.jummpExportJms) {
                    remote()
                }
                else {
                    server()
                }
            }
            on("remote").to("remoteRemote")
            on("server").to("server")
        }

        decideBackFromVcs {
            action {
                if (flow.authenticationBackend == "ldap") {
                    ldap()
                } else {
                    authenticationBackend()
                }
            }
            on("ldap").to("ldap")
            on("authenticationBackend").to("authenticationBackend")
        }

        decideBackFromFirstRun {
            action {
                if (flow.vcs.isGit()) {
                    git()
                } else if (flow.vcs.isSvn()) {
                    svn()
                } else {
                    // just for safety
                    error()
                }
            }
            on("git").to("git")
            on("svn").to("svn")
            on("error").to("firstRun")
        }

        finish {
        }
    }

    def firstRunFlow = {
            start {
                on("next") { UserCommand cmd ->
                flow.user = cmd
                if (flow.user.hasErrors()) {
                    return error()
                } else {
                    return success()
                }
            }.to("validateAdmin")
        }

        validateAdmin {
            action {
            	try
            	{
            		userService.createAdmin(flow.user)
                    Properties props = new Properties()
                    File file = new File(configurationService.getConfigFilePath())
                    props.load(new FileInputStream(file))
                    props.setProperty("jummp.firstRun", "false")
                    FileOutputStream out = new FileOutputStream(file)
                    props.store(out, "Jummp Configuration")
                    next()
            	}
            	catch(Exception e)
            	{
            		e.printStackTrace()
                    error()
            	}
            }
            on("error").to("start")
            on("next").to("finish")
        }

        finish {
        }
    }
}
