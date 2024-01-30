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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Log4j (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Log4j used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.plugins.git

import grails.util.Environment
import grails.util.Holders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockServletContext

import javax.servlet.ServletContext
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.core.vcs.VcsNotInitedException

/**
 * @short Factory Class for GitManager.
 *
 * The factory takes care of creating the GitManager and all the required directories based on
 * the current configuration. If the configuration does not satisfy the factory's need it will
 * throw a VcsException.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class GitManagerFactory {
    GitManager git
    private final Logger LOGGER = LoggerFactory.getLogger(GitManagerFactory.class)
    /**
     * Dependency Injection of grailsApplication
     */
    def grailsApplication

    GitManager getInstance() throws Exception {
        if (git) {
            return git
        }

        ServletContext servletContext
        if (Environment.current == Environment.TEST) {
            servletContext = new MockServletContext()
        } else {
            servletContext = Holders.getServletContext()
        }
        ConfigObject config = grailsApplication.config
        File exchangeDirectory
        if (config.jummp.vcs.exchangeDirectory instanceof String && !config.jummp.vcs.exchangeDirectory.isEmpty()) {
            exchangeDirectory = new File(config.jummp.vcs.exchangeDirectory)
        } else {
            exchangeDirectory = new File(servletContext.getRealPath("/resource/exchangeDir"))
        }
        try {
            if (!exchangeDirectory.exists()) {
                exchangeDirectory.mkdirs()
            }
            git = new GitManager()
            git.init(exchangeDirectory)
        } catch (VcsException e) {
            git = null
            LOGGER.error(e.getMessage())
            e.printStackTrace()
            throw new VcsNotInitedException()
        }
        return git
    }
}
