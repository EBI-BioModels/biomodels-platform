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
* groovy, Spring Framework, Perf4j, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0 the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of groovy, Spring Framework, Perf4j, Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import net.biomodels.jummp.core.miriam.IMiriamService
import org.springframework.beans.factory.InitializingBean
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled

/**
 * Service for handling MIRIAM resources.
 *
 * This component fetches the export of the Identifiers.org registry and stores it
 * in the working directory.
 * @author Martin Gräßlin <m.graesslin@dkfz.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class MiriamService implements IMiriamService, InitializingBean {
    /**
     * Disable automatic transactional behaviour of Grails services.
     */
    static transactional = false
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(this.getClass())
    /**
     * Flag for logger verbosity.
     */
    static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * The URL for obtaining the export from the identifiers.org registry.
     */
    final String DEFAULT_EXPORT_URL = "http://www.ebi.ac.uk/miriam/main/export/xml/"
    /**
     * The name of the file containing the export of the identifiers.org registry.
     */
    final String EXPORT_FILE_NAME = "miriam.xml"
    /**
     * The file containing the export of the identifiers.org registry.
     */
    File registryExport
    /**
     * Dependency injection of Grails Application.
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    /**
     * Initialisation for the registry export instance variable.
     */
    void afterPropertiesSet() {
        String folderPath = grailsApplication.config.jummp.vcs.workingDirectory
        registryExport = new File(folderPath, EXPORT_FILE_NAME)
        log.info("Finished the bean initialisation")
    }

    @Profiled(tag="MiriamService.updateMiriamResources")
    public void updateMiriamResources(String url = DEFAULT_EXPORT_URL) {
        if (IS_INFO_ENABLED) {
            log.info "Started updating the identifiers.org registry export."
        }
        def out = new BufferedOutputStream(new FileOutputStream(registryExport))
        // default left shift only works for text streams
        try {
            out << new URL(url).openStream()
        } catch (IOException e) {
            log.error("Cannot update identifiers.org registry: ${e.message}", e)
        } finally {
            out?.close()
        }
        if (IS_INFO_ENABLED) {
            log.info "Finished updating the identifiers.org registry export."
        }
    }
}
