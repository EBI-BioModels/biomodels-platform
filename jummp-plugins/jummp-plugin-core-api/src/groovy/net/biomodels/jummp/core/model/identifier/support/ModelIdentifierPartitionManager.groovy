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
 */

package net.biomodels.jummp.core.model.identifier.support

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Class that partitions model identifiers based on the settings for generating them.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class ModelIdentifierPartitionManager {
    /** The model identifier to use for generating the partitions. */
    String modelIdentifier
    /** A list of ModelIdentifierPartition objects that make up the given model identifier. */
    ArrayList<ModelIdentifierPartition> partitions
    /** The settings used for specifying a generator, or a type of model identifier. */
    ConfigObject settings
    /** the class logger */
    private static final Log log = LogFactory.getLog(this)
    /** the registry */
    private ModelIdentifierCursorPartitionRegistry registry

    /** public constructor */
    ModelIdentifierPartitionManager(ConfigObject config, String lastIdentifier) {
        partitions = new ArrayList<ModelIdentifierPartition>()
        if (!config) {
            String MSG = "The model identification scheme settings are not defined."
            log.error MSG
            throw new Exception(MSG)
        }
        settings = config
        if (lastIdentifier) {
            modelIdentifier = lastIdentifier
        }
        registry = lastIdentifier ? new ModelIdentifierCursorPartitionRegistry(lastIdentifier) :
                        new ModelIdentifierCursorPartitionRegistry()
        parseSettings(settings, modelIdentifier)
    }

    /*
     * Validates model identifier settings and builds command objects for storing them.
     */
    private void parseSettings(ConfigObject s, String id) {
        final String CHECKSUM_KEY = "useChecksum"
        s.sort{it.key}.eachWithIndex { entry, idx ->
            ModelIdentifierPartition partition
            String actualValue = entry.key
            if (actualValue != CHECKSUM_KEY) {
                String expectedValue = "part${idx + 1}"
                boolean validPartOrder = actualValue == expectedValue
                if (!validPartOrder) {
                    log.error("Invalid model id part order: ${settings.inspect()}")
                    def errMsg = new StringBuilder("Model id part order invalid: ")
                    errMsg.append("Expected $expectedValue, not $actualValue.")
                    errMsg.append(" Please review the settings for jummp.model.id")
                    errMsg.append(" and ensure that the defined identifier parts")
                    errMsg.append(" are in consecutive order.")
                    String M = errMsg.toString()
                    log.error M
                    throw new Exception(M)
                } else {
                    ConfigObject config = entry.value
                    final String TYPE = config.type
                    switch(TYPE) {
                        case 'date':
                            String dateFormat
                            boolean dateFormatMissing = !config.containsKey('format')
                            if (!dateFormatMissing) {
                                dateFormat = config.format
                            } // TODO else throw an error!!!
                            partition = new DateModelIdentifierPartition()
                            partition.format = dateFormat
                            break
                        case 'literal':
                            String suffix
                            boolean suffixMissing = !config.containsKey('suffix')
                            if (!suffixMissing) {
                                suffix = config.suffix
                            }
                            String isFixed = config.fixed ?: 'true'
                            partition = new LiteralModelIdentifierPartition(isFixed, suffix)
                            break
                        case 'numerical':
                            String width = null
                            boolean widthMissing = !config.containsKey('width')
                                if (!widthMissing) {
                                width = config.width
                            }
                            String isFixed = config.containsKey('fixed') ? config.fixed : 'true'
                            partition = new NumericalModelIdentifierPartition(isFixed, width)
                            break
                        default:
                            final String M = "Unknown model id part type for $actualValue: $TYPE"
                            log.error M
                            throw new Exception(M)
                            break
                    }
                }
            } else { // checksum
                partition = new ChecksumModelIdentifierPartition()
            }
            boolean partitionAdded = addPartition(partition)
            if (!partitionAdded) {
                String err = "Partition ${partition.inspect()} was not added to the registry!"
                 log.error err
                throw new Exception(err)
            }
        }
    }

    /* Updates the indices and the value of the partition, then adds it to the registry. */
    boolean addPartition(ModelIdentifierPartition partition) {
        boolean haveRegistered = registry.registerPartition(partition)
        if (!haveRegistered) {
            return false
        }
        boolean haveUpdated = registry.updatePartitionValue(partition)
        if (!haveUpdated) {
            return false
        }
        return partitions.add(partition)
    }
}
