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

import net.biomodels.jummp.core.IPharmMlRenderer
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Factory class for IPharmMlRenderer implementations.
 *
 * This class manages the association between versions of PharmML and corresponding
 * IPharmMlRenderer implementations that handle them.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
final class PharmMlVersionAwareRendererFactory {
    /* Class logger */
    private static final Log log = LogFactory.getLog(this)
    /* Semaphore for the log's verbosity threshold */
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /*
     * This map typically caches singleton instances of IPharmMlRenderer implementations.
     */
    private static Map<String, IPharmMlRenderer> renderers = [:]

    /* private default constructor */
    private PharmMlVersionAwareRendererFactory() {}

    /**
     * Get an instance of a renderer for a specific PharmML version.
     *
     * @param version the version of PharmML in which the model being rendered was encoded.
     * @return an implementation of IPharmMlRenderer that can parse the specified @p version,
     * or the default one if no renderer exists for the version that was provided.
     */
    static IPharmMlRenderer getRenderer(String version) {
        if (!version) {
            return getDefaultRenderer()
        }
        if (renderers[version] == null) {
            synchronized(PharmMlVersionAwareRendererFactory.class) {
                if (renderers[version] == null) {
                    switch(version) {
                        case "0.1":
                            //fall through
                        case "0.2.1":
                            renderers[version] = PharmMl0_2AwareRenderer.getInstance()
                            if (IS_INFO_ENABLED) {
                                log.info "Cached the PharmMl0_2AwareRenderer instance."
                            }
                            break
                        case "0.3":
                            //fall through
                        case "0.3.1":
                            renderers[version] = PharmMl0_3AwareRenderer.getInstance()
                            if (IS_INFO_ENABLED) {
                                log.info "Cached the PharmMl0_3AwareRenderer instance."
                            }
                            break
                        case "0.6":
                        default:
                            renderers[version] = PharmMl0_6AwareRenderer.getInstance()
                            if (IS_INFO_ENABLED) {
                                log.info "Cached the PharmMl0_6AwareRenderer instance."
                            }
                            break
                    }
                }
            }
        }
        return renderers[version]
    }

    /**
     * @return the default PharmML renderer that should be used.
     */
    static IPharmMlRenderer getDefaultRenderer() {
        return getRenderer("0.6")
    }
}
