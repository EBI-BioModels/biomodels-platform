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
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Framework used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.configuration

import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

import java.util.concurrent.locks.ReentrantLock

/**
 * @short Service for managing the configuration stored in a properties file.
 *
 * <p>This service can be used by controllers to update the jummp configuration.
 * It is important to remember that a change in the configuration does not have any
 * influence to the runtime behavior of the currently running application.
 * The application needs to be restarted, whenever the configuration changes.
 *
 * @author <a href="m.graesslin@dkfz-heidelberg.de">Martin Gräßlin</a>
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 * @date 20130705
 */
class ConfigurationService implements InitializingBean {
    static scope = "prototype"
    static transactional = false
    /**
     * Methods accessing the configuration files, need to be thread-safe, hence the use of a lock.
     */
    private final ReentrantLock lock = new ReentrantLock()
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class)

    /**
     * The configuration file is set in after properties set to a default value if not already set.
     * Primary purpose of this property is for integration tests to not overwrite the real configuration.
     */
    @SuppressWarnings('GrailsStatelessService')
    File configurationFile = null
    /**
     * Default name of the configuration file
     */
    final String CFG_FILENAME = ".jummp.properties"

    private boolean testPath(String path) {
        File testFile = new File(path)
        return testFile.exists()
    }

    private String getDefaultConfigFile() {
        logger.debug("Jummp is loading the configuration file from the default location")
        String userHome = System.getProperty("user.home")
        String fileSeparator = System.getProperty("file.separator")
        String path =  "${userHome}${fileSeparator}${CFG_FILENAME}".toString()
        if (!testPath(path)) {
            path = null
            logger.error("The default configuration file doesn't exist. The app cannot be started!")
        }
        path
    }

    String getConfigFilePath() {
        String path = System.getenv("JUMMP_CONFIG")
        if (!path || !testPath(path)) {
            path = getDefaultConfigFile()
        }
        return path
    }

    void afterPropertiesSet() {
        String configPath = getConfigFilePath()
        if (!configurationFile && configPath) {
            logger.debug("The app is loading the configuration file ${configPath}")
            configurationFile = new File(configPath)
        } else {
            String errMsg = "Cannot find neither .jummp.properties nor alternative"
            logger.error(errMsg)
            throw new GrailsConfigurationException(errMsg)
        }
    }

    /**
     * Simple class to sort the properties map to make the resulting config file
     * easier to read.
     */
    class SortedProperties extends Properties {
        Enumeration keys() {
            Enumeration keysEnum = super.keys();
            Vector<String> keyList = new Vector<String>();
            while(keysEnum.hasMoreElements()){
                keyList.add((String)keysEnum.nextElement());
            }
            Collections.sort(keyList);
            return keyList.elements();
        }
    }

    /**
     * Rewrites the complete configuration.
     * @param database The database configuration
     * @param ldap The LDAP configuration, if @c null database backend is used
     * @param vcs The Version Control System configuration
     * @param svn The Subversion configuration, may be @c null
     * @param firstRun The First run configuration
     * @param server The Server configuration
     * @param userRegistration the user registration configuration
     * @param changePassword the change/reset password configuration
     * @param remote The Remote configuration
     * @param trigger The Trigger configuration
     * @param tsbml The SBML configuration
     * @param bives the BiVeS configuration
     * @param cms the CMS configuration
     */
    public void storeConfiguration(DatabaseCommand database, LdapCommand ldap, VcsCommand vcs, SvnCommand svn, FirstRunCommand firstRun,
                                   ServerCommand server, UserRegistrationCommand userRegistration, ChangePasswordCommand changePassword,
                                   RemoteCommand remote, TriggerCommand trigger, SBMLCommand sbml, BivesCommand bives,
                                   CmsCommand cms, BrandingCommand branding, SearchCommand search, MailCommand mail) {
        Properties properties = new SortedProperties()
        if (database) {
        	updateDatabaseConfiguration(properties, database)
        }
        if (remote) {
        	updateRemoteConfiguration(properties, remote)
        }
        if (ldap) {
        	updateLdapConfiguration(properties, ldap)
        }
        if (vcs) {
        	updateVcsConfiguration(properties, vcs)
        }
        if (svn) {
        	updateSvnConfiguration(properties, svn)
        }
        if (firstRun) {
        	updateFirstRunConfiguration(properties, firstRun)
        }
        if (server) {
        	updateServerConfiguration(properties, server)
        }
        if (userRegistration) {
        	updateUserRegistrationConfiguration(properties, userRegistration)
        }
        if (changePassword) {
        	updateChangePasswordConfiguration(properties, changePassword)
        }
        if (sbml) {
        	updateSBMLConfiguration(properties, sbml)
        }
        if (trigger) {
        	updateTriggerConfiguration(properties, trigger)
        }
        if (bives) {
        	updateBivesConfiguration(properties, bives)
        }
        if (cms) {
        	updateCmsConfiguration(properties, cms)
        }
        if (branding) {
        	updateBrandingConfiguration(properties, branding)
        }
        if (search) {
        	updateSearchConfiguration(properties, search)
        }
        if (mail) {
        	updateMailConfiguration(properties, mail)
        }
        if (ldap) {
            properties.setProperty("jummp.security.authenticationBackend", "ldap")
        } else {
            properties.setProperty("jummp.security.authenticationBackend", "database")
        }
        saveProperties(properties)
    }

    /**
     * Loads the current BiVeS Configuration.
     * @return A command object encapsulating the current BiVeS configuration
     */
    public BivesCommand loadBivesConfiguration() {
        Properties properties = loadProperties()
        BivesCommand bives = new BivesCommand()
        bives.diffDir   = properties.getProperty("jummp.plugins.bives.diffdir")
        return bives
    }

    /**
     * Loads the current CMS Configuration.
     * @return A command object encapsulating the current CMS configuration
     */
    public CmsCommand loadCmsConfiguration() {
        Properties properties = loadProperties()
        CmsCommand cmsCommand = new CmsCommand()
        cmsCommand.policyFile = properties.getProperty("jummp.security.cms.policy")
        return cmsCommand
    }

    /**
     * Loads the current database Configuration.
     * @return A command object encapsulating the current database configuration
     */
    public DatabaseCommand loadDatabaseConfiguration() {
        Properties properties = loadProperties()
        DatabaseCommand database = new DatabaseCommand()
        switch (properties.getProperty("jummp.database.type")) {
        case "POSTGRESQL":
            database.type = DatabaseType.POSTGRESQL
            break
        case "MYSQL": // fall through
        default:
            database.type = DatabaseType.MYSQL
            break
        }
        database.server   = properties.getProperty("jummp.database.server")
        database.port     = Integer.parseInt(properties.getProperty("jummp.database.port"))
        database.database = properties.getProperty("jummp.database.database")
        database.username = properties.getProperty("jummp.database.username")
        database.password = properties.getProperty("jummp.database.password")
        return database
    }

    /**
     * Loads the current information of HTTP Proxy
     * @return A command object {@link HttpProxyCommand} encapsulating HTTP Proxy information
     */
    HttpProxyCommand loadHttpProxy() {
        Properties properties = loadProperties()
        HttpProxyCommand proxyCommand = new HttpProxyCommand()
        proxyCommand.host = properties.getProperty("jummp.http.proxy.host")
        proxyCommand.port = Integer.parseInt(properties.getProperty("jummp.http.proxy.port"))
        return proxyCommand
    }

    /**
     * Verifies the current configuration having HTTP Proxy information or not
     * @return A command object {@link Proxy} encapsulating HTTP Proxy information
     */
    static Proxy verifyHttpProxy() {
        HttpProxyCommand command = loadHttpProxy()
        String host = command.host
        int port = command.port as int
        boolean noSetProxyHost = host.equalsIgnoreCase("localhost") || host == null
        boolean notSetProxyPort = port == 80 || port == null
        boolean noHttpProxy = noSetProxyHost && notSetProxyPort
        Proxy proxy
        if (noHttpProxy) {
            proxy = null
        } else {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port))
        }
        proxy
    }


    /**
     * Loads the current Remote Configuration.
     * @return A command object encapsulating the current Remote Configuration
     */
    public RemoteCommand loadRemoteConfiguration() {
        Properties properties = loadProperties()
        RemoteCommand remote = new RemoteCommand()
        remote.jummpRemote = properties.getProperty("jummp.remote")
        remote.jummpExportJms = Boolean.parseBoolean(properties.getProperty("jummp.export.jms"))
        return remote
    }

    /**
     * Loads the current LDAP Configuration.
     * @return A command object encapsulating the current LDAP configuration
     */
    public LdapCommand loadLdapConfiguration() {
        Properties properties = loadProperties()
        LdapCommand ldap = new LdapCommand()
        ldap.ldapServer          = properties.getProperty("jummp.security.ldap.server")
        ldap.ldapManagerDn       = properties.getProperty("jummp.security.ldap.managerDn")
        ldap.ldapManagerPassword = properties.getProperty("jummp.security.ldap.managerPw")
        ldap.ldapSearchBase      = properties.getProperty("jummp.security.ldap.search.base")
        ldap.ldapSearchFilter    = properties.getProperty("jummp.security.ldap.search.filter")
        ldap.ldapSearchSubtree   = Boolean.parseBoolean(properties.getProperty("jummp.security.ldap.search.subTree"))
        return ldap
    }

    /**
     * Loads the current Version Control System Configuration.
     * @return A command object encapsulating the current VCS configuration
     */
    public VcsCommand loadVcsConfiguration() {
        Properties properties = loadProperties()
        VcsCommand vcs = new VcsCommand()
        vcs.vcs = properties.getProperty("jummp.vcs.plugin") == "subversion" ? "svn" : "git"
        vcs.exchangeDirectory = properties.getProperty("jummp.vcs.exchangeDirectory")
        vcs.workingDirectory  = properties.getProperty("jummp.vcs.workingDirectory")
        return vcs
    }

    /**
     * Loads the current Subversion Configuration.
     * @return A command object encapsulating the current SVN configuration
     */
    public SvnCommand loadSvnConfiguration() {
        Properties properties = loadProperties()
        SvnCommand svn = new SvnCommand()
        svn.localRepository = properties.getProperty("jummp.plugins.subversion.localRepository")
        return svn
    }

    /**
     * Loads the current Server Configuration.
     * @return A command object encapsulating the current server configuration
     */
    public ServerCommand loadServerConfiguration() {
        Properties properties = loadProperties()
        ServerCommand server = new ServerCommand()
        server.url = properties.getProperty("jummp.server.url")
        server.protectEverything = Boolean.parseBoolean(properties.getProperty("jummp.server.protection"))
        return server
    }

    /**
     * Loads the current user registration Configuration
     * @return A command object encapsulating the current user registration configuration
     */
    public UserRegistrationCommand loadUserRegistrationConfiguration() {
        Properties properties = loadProperties()
        UserRegistrationCommand cmd = new UserRegistrationCommand()
        cmd.registration  = Boolean.parseBoolean(properties.getProperty("jummp.security.anonymousRegistration"))
        cmd.sendEmail     = Boolean.parseBoolean(properties.getProperty("jummp.security.registration.email.send"))
        cmd.sendToAdmin   = Boolean.parseBoolean(properties.getProperty("jummp.security.registration.email.sendToAdmin"))
        cmd.subject       = properties.getProperty("jummp.security.registration.email.subject")
        cmd.body          = properties.getProperty("jummp.security.registration.email.body")
        cmd.url           = properties.getProperty("jummp.security.registration.verificationURL")
        cmd.senderAddress = properties.getProperty("jummp.security.registration.email.sender")
        cmd.adminAddress  = properties.getProperty("jummp.security.registration.email.adminAddress")
        cmd.activationBody    = properties.getProperty("jummp.security.activation.email.body")
        cmd.activationSubject = properties.getProperty("jummp.security.activation.email.subject")
        cmd.activationUrl     = properties.getProperty("jummp.security.activation.activationURL")
        return cmd
    }

    /**
     * Loads the current change/reset password Configuration
     * @return A command object encapsulating the current change/reset password configuration
     */
    public ChangePasswordCommand loadChangePasswordConfiguration() {
        Properties properties = loadProperties()
        ChangePasswordCommand cmd = new ChangePasswordCommand()
        cmd.changePassword = Boolean.parseBoolean(properties.getProperty("jummp.security.ui.changePassword"))
        cmd.resetPassword  = Boolean.parseBoolean(properties.getProperty("jummp.security.resetPassword.email.send"))
        cmd.senderAddress  = properties.getProperty("jummp.security.resetPassword.email.sender")
        cmd.subject        = properties.getProperty("jummp.security.resetPassword.email.subject")
        cmd.body           = properties.getProperty("jummp.security.resetPassword.email.body")
        cmd.url            = properties.getProperty("jummp.security.resetPassword.url")
        return cmd
    }

    /**
     * Loads the current triggerConfiguration.
     * @return A command object encapsulating the current BiVeS configuration
     */
    public TriggerCommand loadTriggerConfiguration() {
        Properties properties = loadProperties()
        TriggerCommand trigger = new TriggerCommand()
        trigger.startRemoveOffset = Long.parseLong(properties.getProperty("jummp.authenticationHash.startRemoveOffset"))
        trigger.removeInterval = Long.parseLong(properties.getProperty("jummp.authenticationHash.removeInterval"))
        trigger.maxInactiveTime = Long.parseLong(properties.getProperty("jummp.authenticationHash.maxInactiveTime"))
        return trigger
    }

    /**
     * Loads the current SBMLConfiguration.
     * @return A command object encapsulating the current SBML configuration
     */
    public SBMLCommand loadSBMLConfiguration() {
        Properties properties = loadProperties()
        SBMLCommand sbml = new SBMLCommand()
        sbml.validate = Boolean.parseBoolean(properties.getProperty("jummp.plugins.sbml.validation"))
        return sbml
    }

    /**
     * Loads the current branding configuration.
     * @return A command object encapsulating the current branding configuration
     */
    public BrandingCommand loadBrandingConfiguration() {
        Properties properties = loadProperties()
        BrandingCommand branding = new BrandingCommand()
        branding.internalColor = properties.getProperty("jummp.branding.internalColor")
        branding.externalColor = properties.getProperty("jummp.branding.externalColor")
        branding.deployment = properties.getProperty("jummp.branding.deployment")
        branding.style = properties.getProperty("jummp.branding.style")
        return branding
    }

    /**
     * Updates the BiVeS configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param bives The new BiVeS configuration
     */
    public void saveBivesConfiguration(BivesCommand bives) {
        Properties properties = loadProperties()
        updateBivesConfiguration(properties, bives)
        saveProperties(properties)
    }

    /**
     * Updates the CMS configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param cmsCommand The new CMS configuration
     */
    public void saveCmsConfiguration(CmsCommand cmsCommand) {
        Properties properties = loadProperties()
        updateCmsConfiguration(properties, cmsCommand)
        saveProperties(properties)
    }

    /**
     * Updates the database configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param database The new database configuration
     */
    public void saveDatabaseConfiguration(DatabaseCommand database) {
        Properties properties = loadProperties()
        updateDatabaseConfiguration(properties, database)
        saveProperties(properties)
    }

    /**
     * Updates the Remote configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param remote The new Remote configuration
     */
    public void saveRemoteConfiguration(RemoteCommand remote) {
        Properties properties = loadProperties()
        updateRemoteConfiguration(properties, remote)
        saveProperties(properties)
    }

    /**
     * Updates the LDAP configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param ldap The new LDAP configuration
     */
    public void saveLdapConfiguration(LdapCommand ldap) {
        Properties properties = loadProperties()
        updateLdapConfiguration(properties, ldap)
        saveProperties(properties)
    }

    /**
     * Updates the Version Control System configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param vcs The new VCS configuration
     */
    public void saveVcsConfiguration(VcsCommand vcs) {
        Properties properties = loadProperties()
        updateVcsConfiguration(properties, vcs)
        saveProperties(properties)
    }

    /**
     * Updates the Subversion configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param svn The new Svn configuration
     */
    public void saveSvnConfiguration(SvnCommand svn) {
        Properties properties = loadProperties()
        updateSvnConfiguration(properties, svn)
        saveProperties(properties)
    }

    /**
     * Updates the Server configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param svn The new Svn configuration
     */
    public void saveServerConfiguration(ServerCommand server) {
        Properties properties = loadProperties()
        updateServerConfiguration(properties, server)
        saveProperties(properties)
    }

    /**
     * Updates the User registration settings stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param cmd The new User Registration settings
     */
    public void saveUserRegistrationConfiguration(UserRegistrationCommand cmd) {
        Properties properties = loadProperties()
        updateUserRegistrationConfiguration(properties, cmd)
        saveProperties(properties)
    }

    /**
     * Updates the change/reset password settings stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param cmd The new change/reset password settings
     */
    public void saveChangePasswordConfiguration(ChangePasswordCommand cmd) {
        Properties properties = loadProperties()
        updateChangePasswordConfiguration(properties, cmd)
        saveProperties(properties)
    }

    /**
     * Updates the trigger configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param trigger The new trigger configuration
     */
    public void saveTriggerConfiguration(TriggerCommand trigger) {
        Properties properties = loadProperties()
        updateTriggerConfiguration(properties, trigger)
        saveProperties(properties)
    }

    /**
     * Updates the SBML configuration stored in the properties file.
     * Other settings are not changed!
     * It is important to remember that the settings will only be activated after
     * a restart of the application!
     * @param sbml The new SBML configuration
     */
    public void saveSBMLConfiguration(SBMLCommand sbml) {
        Properties properties = loadProperties()
        updateSBMLConfiguration(properties, sbml)
        saveProperties(properties)
    }

   /**
    * Updates the branding configuration stored in the properties file.
    * Other settings are not changed!
    * It is important to remember that the settings will only be activated after
    * a restart of the application!
    * @param branding The new branding configuration
    */
   public void saveBrandingConfiguration(BrandingCommand branding) {
       Properties properties = loadProperties()
       updateBrandingConfiguration(properties, branding)
       saveProperties(properties)
   }

    /**
     * Updates the @p properties with the settings from @p bives.
     * @param properties The existing properties
     * @param bives the BiVeS settings
     */
    private void updateBivesConfiguration(Properties properties, BivesCommand bives) {
        if(!bives.validate()) {
            return
        }
        properties.setProperty("jummp.plugins.bives.diffdir", bives.diffDir)
    }

    /**
     * Updates the @p properties with the settings from @p cms.
     * @param properties The existing properties
     * @param cmsCommand the cms settings
     */
    private void updateCmsConfiguration(Properties properties, CmsCommand cmsCommand) {
        if(!cmsCommand.validate()) {
            return
        }
        properties.setProperty("jummp.security.cms.policy", cmsCommand.policyFile)
    }

    /**
     * Updates the @p properties with the settings from @p database.
     * @param properties The existing properties
     * @param database The new database settings
     */
    private void updateDatabaseConfiguration(Properties properties, DatabaseCommand database) {
        if (!database.validate()) {
            return
        }
        properties.setProperty("jummp.database.type",     database.type.key)
        properties.setProperty("jummp.database.server",   database.server)
        properties.setProperty("jummp.database.port",     database.port.toString())
        properties.setProperty("jummp.database.database", database.database)
        properties.setProperty("jummp.database.username", database.username)
        properties.setProperty("jummp.database.password", database.password)
    }

    /**
     * Updates the @p properties with the settings from @p remote.
     * @param properties The existing properties
     * @param remote The new remote settings
     */
    private void updateRemoteConfiguration(Properties properties, RemoteCommand remote) {
        if (!remote.validate()) {
            return
        }
        if (!remote.jummpRemote) {
        	remote.jummpRemote=false
        }
        properties.setProperty("jummp.remote",   remote.jummpRemote)
        properties.setProperty("jummp.export.jms", remote.jummpExportJms.toString())
    }

    /**
     * Updates the @p properties with the settings from @p ldap.
     * If @p ldap is @c null, the LDAP configuration is deleted from the properties
     * @param properties The existing properties
     * @param ldap The new ldap settings, may be @c null
     */
    private void updateLdapConfiguration(Properties properties, LdapCommand ldap) {
        if (ldap && !ldap.validate()) {
            return
        }
        properties.setProperty("jummp.security.ldap.enabled", ldap ? "true" : "false")
        if (ldap) {
            properties.setProperty("jummp.security.ldap.server",         ldap.ldapServer)
            properties.setProperty("jummp.security.ldap.managerDn",      ldap.ldapManagerDn)
            properties.setProperty("jummp.security.ldap.managerPw",      ldap.ldapManagerPassword)
            properties.setProperty("jummp.security.ldap.search.base",    ldap.ldapSearchBase)
            properties.setProperty("jummp.security.ldap.search.filter",  ldap.ldapSearchFilter)
            properties.setProperty("jummp.security.ldap.search.subTree", ldap.ldapSearchSubtree.toString())
        } else {
            properties.remove("jummp.security.ldap.server")
            properties.remove("jummp.security.ldap.managerDn")
            properties.remove("jummp.security.ldap.managerPw")
            properties.remove("jummp.security.ldap.search.base")
            properties.remove("jummp.security.ldap.search.filter")
            properties.remove("jummp.security.ldap.search.subTree")
        }
    }

    /**
     * Updates the @p properties with the settings from @p vcs.
     * @param properties The existing properties
     * @param vcs The new version control system settings
     */
    private void updateVcsConfiguration(Properties properties, VcsCommand vcs) {
        if (!vcs.validate()) {
            return
        }
        properties.setProperty("jummp.vcs.plugin",            vcs.pluginName())
        properties.setProperty("jummp.vcs.exchangeDirectory", vcs.exchangeDirectory)
        properties.setProperty("jummp.vcs.workingDirectory",  vcs.workingDirectory)
    }

    /**
     * Updates the @p properties with the settings from @p svn.
     * If @p svn is @c null, the subversion configuration is deleted from the properties.
     * @param properties The existing properties
     * @param svn The new subversion settings, may be @c null
     */
    private void updateSvnConfiguration(Properties properties, SvnCommand svn) {
        if (svn) {
            if (!svn.validate()) {
                return
            }
            properties.setProperty("jummp.plugins.subversion.localRepository", svn.localRepository)
        } else {
            properties.remove("jummp.plugins.subversion.localRepository")
        }
    }

    /**
     * Updates the @p properties with the settings from @p firstRun.
     * @param properties The existing properties
     * @param firstRun The new first run settings
     */
    private void updateFirstRunConfiguration(Properties properties, FirstRunCommand firstRun) {
        if (!firstRun.validate()) {
            return
        }
        properties.setProperty("jummp.firstRun", firstRun.firstRun)
    }

    /**
     * Updates the @p properties with the settings from @p server.
     * @param properties The existing properties
     * @param firstRun The new server settings
     */
    private void updateServerConfiguration(Properties properties, ServerCommand server) {
        if (!server.validate()) {
            return
        }
        properties.setProperty("jummp.server.url", server.url)
        properties.setProperty("jummp.server.protection", server.protectEverything ? "true" : "false")
    }

    /**
     * Updates the @p properties with the settings from @p cmd
     * @param properties The existing properties
     * @param cmd The new user registration settings
     */
    private void updateUserRegistrationConfiguration(Properties properties, UserRegistrationCommand cmd) {
        if (!cmd.validate()) {
            return
        }
        /*if (!cmd.url.endsWith("/")) {
            cmd.url = cmd.url + "/"
        }
        cmd.url = cmd.url + "register/validate/{{CODE}}"
        if (!cmd.activationUrl.endsWith("/")) {
            cmd.activationUrl = cmd.activationUrl + "/"
        }
        cmd.activationUrl = cmd.activationUrl + "register/confirmRegistration/{{CODE}}"*/
        properties.setProperty("jummp.security.anonymousRegistration", cmd.registration ? "true" : "false")
        properties.setProperty("jummp.security.curatorByDefault", cmd.curator ? "true" : "false")
        properties.setProperty("jummp.security.registration.email.send", cmd.sendEmail ? "true" : "false")
        properties.setProperty("jummp.security.registration.email.sendToAdmin", cmd.sendToAdmin ? "true" : "false")
        properties.setProperty("jummp.security.registration.email.sender", cmd.senderAddress)
        properties.setProperty("jummp.security.registration.email.adminAddress", cmd.adminAddress)
        properties.setProperty("jummp.security.registration.email.subject", cmd.subject)
        properties.setProperty("jummp.security.registration.email.body", cmd.body)
        properties.setProperty("jummp.security.resetPassword.email.subject", cmd.resetSubject)
        properties.setProperty("jummp.security.resetPassword.email.body", cmd.resetBody)
        /*properties.setProperty("jummp.security.registration.verificationURL", cmd.url)
        properties.setProperty("jummp.security.activation.email.body", cmd.activationBody)
        properties.setProperty("jummp.security.activation.email.subject", cmd.activationSubject)
        properties.setProperty("jummp.security.activation.activationURL", cmd.activationUrl)*/
    }

    /**
     * Updates the @p properties with the settings from @p cmd
     * @param properties The existing properties
     * @param cmd The new change/reset password settings
     */
    private void updateChangePasswordConfiguration(Properties properties, ChangePasswordCommand cmd) {
        if (!cmd.validate()) {
            return
        }
        if (!cmd.url.endsWith("/")) {
            cmd.url = cmd.url + "/"
        }
        cmd.url = cmd.url + "user/resetPassword/{{CODE}}"
        properties.setProperty("jummp.security.ui.changePassword",           cmd.changePassword ? "true" : "false")
        properties.setProperty("jummp.security.resetPassword.email.send",    cmd.resetPassword ? "true" : "false")
        properties.setProperty("jummp.security.resetPassword.email.sender",  cmd.senderAddress)
        properties.setProperty("jummp.security.resetPassword.email.subject", cmd.subject)
        properties.setProperty("jummp.security.resetPassword.email.body",    cmd.body)
        properties.setProperty("jummp.security.resetPassword.url",           cmd.url)
    }

    /**
     * Updates the @p properties with the settings from @p trigger.
     * @param properties The existing properties
     * @param trigger The trigger settings
     */
    private void updateTriggerConfiguration(Properties properties, TriggerCommand trigger) {
        if(!trigger.validate()) {
            return
        }
        properties.setProperty("jummp.authenticationHash.startRemoveOffset", trigger.startRemoveOffset.toString())
        properties.setProperty("jummp.authenticationHash.removeInterval", trigger.removeInterval.toString())
        properties.setProperty("jummp.authenticationHash.maxInactiveTime", trigger.maxInactiveTime.toString())
    }

    /**
     * Updates the @p properties with the settings from @p sbml
     * @param properties The existing properties
     * @param sbml The SBML settings
     */
    private void updateSBMLConfiguration(Properties properties, SBMLCommand sbml) {
        if(!sbml.validate()) {
            return
        }
        properties.setProperty("jummp.plugins.sbml.validation", sbml.validation.toString())
    }

   /**
    * Updates the @p properties with the settings from @p branding
    * @param properties The existing properties
    * @param branding The branding settings
    */
   private void updateBrandingConfiguration(Properties properties, BrandingCommand branding) {
       if(!branding.validate()) {
           return
       }
       properties.setProperty("jummp.branding.internalColor", branding.internalColor.toString())
       properties.setProperty("jummp.branding.externalColor", branding.externalColor.toString())
   }

    /**
    * Updates the @p properties with the settings from @p search
    * @param properties The existing properties
    * @param search The search settings
    */
   private void updateSearchConfiguration(Properties properties, SearchCommand search) {
       if(!search.validate()) {
           return
       }
       properties.setProperty("jummp.search.index", search.indexDirectory.toString())
   }

   /**
    * Updates the @p properties with the settings from @p mail
    * @param properties The existing properties
    * @param mail The mail settings
    */
   private void updateMailConfiguration(Properties properties, MailCommand mail) {
       if(!mail.validate()) {
           return
       }
       properties.setProperty("jummp.security.mailer.host", mail.host.toString())
       properties.setProperty("jummp.security.mailer.port", mail.port.toString())
       if (mail.auth) {
       	   properties.setProperty("jummp.security.mailer.auth", "true")
       	   if (mail.tlsRequired) {
       	   	   properties.setProperty("jummp.security.mailer.tlsrequired", "true")
       	   }
       	   else {
       	   	   properties.setProperty("jummp.security.mailer.tlsrequired", "false")
       	   }
       	   properties.setProperty("jummp.security.mailer.username", mail.username.toString())
       	   properties.setProperty("jummp.security.mailer.password", mail.password.toString())
       }
       else {
       	   properties.setProperty("jummp.security.mailer.auth", "false")
       }
   }


    /**
     * Loads the properties from the configuration file
     * @return The Jummp Configuration Properties
     */
    private Properties loadProperties() {
        Properties properties = new Properties()
        properties.load(new FileInputStream(configurationFile))
        return properties
    }


     /**
     * Stores the @p properties to the configuration file in a thread safe manner.
     * @param properties The new properties
     */
    private void saveProperties(Properties properties) {
        lock.lock()
        try {
            if (!configurationFile) {
                String path = getConfigFilePath()
                if (!path) {
                    // There is no location specified. Using the default location.
                    path= getDefaultConfigFile()
                }
                configurationFile = new File(path)
            }
            FileOutputStream out = new FileOutputStream(configurationFile)
            properties.store(out, "Jummp Configuration")
        } finally {
            lock.unlock()
        }
    }
}
