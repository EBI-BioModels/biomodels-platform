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
* Apache Tika, Apache Commons, LibPharmml, Perf4j (or a modified version of these
* libraries), containing parts covered by the terms of Apache License v2.0,
* the licensors of this Program grant you additional permission to convey the
* resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Tika, Apache Commons,
* LibPharmml, Perf4j used as well as that of the covered work.}
**/

package net.biomodels.jummp.plugins.pharmml

import net.biomodels.jummp.core.IPharmMlService
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Factory class for IPharmMlService implementations.
 *
 * This class manages the association between versions of PharmML and corresponding
 * IPharmMlService implementations that handle them.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
final class PharmMlVersionAwareHandlerFactory {
    /* Class logger */
    private static final Log log = LogFactory.getLog(PharmMlVersionAwareHandlerFactory.class)
    /* Semaphore for the log's verbosity threshold */
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /*
     * This map typically caches singleton instances of IPharmMlService implementations.
     */
    private static Map<String, IPharmMlService> handlers = [:]
    /* private default constructor */
    private PharmMlVersionAwareHandlerFactory() {}

    /**
     * Get an instance of a handler for a specific PharmML version.
     *
     * @param version the version of PharmML in which the model being handled was encoded.
     * @return an implementation of IPharmMlService that can parse the specified @p version,
     * or the default one if no handler exists for the version that was provided.
     */
    public static IPharmMlService getHandler(String version) {
        if (!version) {
            if (IS_INFO_ENABLED) {
                log.info "Returning the default IPharmMlService implementation."
            }
            return getDefaultHandler()
        }

        if (handlers[version] == null) {
            synchronized(PharmMlVersionAwareHandlerFactory.class) {
                if (handlers[version] == null) {
                    switch(version) {
                        case '0.1':
                            // fall through
                        case "0.2.1":
                            handlers[version] = PharmMl0_2AwareHandler.getInstance()
                            if (IS_INFO_ENABLED) {
                                log.info "Cached the PharmMl0_2AwareHandler instance."
                            }
                            break
                        case "0.3":
                        case "0.3.1":
                            handlers[version] = PharmMl0_3AwareHandler.getInstance()
                            if (IS_INFO_ENABLED) {
                                log.info "Cached the PharmMl0_3AwareHandler instance."
                            }
                            break
                        case "0.6":
                        default:
                            handlers[version] = PharmMl0_6AwareHandler.getInstance()
                            if (IS_INFO_ENABLED) {
                                log.info "Cached the PharmMl0_6AwareHandler instance."
                            }
                    }
                }
            }
        }
        return handlers[version]
    }

    /**
     * @return the default PharmML handler that should be used.
     */
    public static IPharmMlService getDefaultHandler() {
        return getHandler("0.6")
    }
}
