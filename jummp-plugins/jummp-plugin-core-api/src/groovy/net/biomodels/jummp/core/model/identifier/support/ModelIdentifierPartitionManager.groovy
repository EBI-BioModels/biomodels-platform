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

package net.biomodels.jummp.core.model.identifier.support

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Class that partitions model identifiers based on the settings for generating them.
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
                            boolean dateFormatMissing = config.format instanceof ConfigObject ||
                                        config.format.isEmpty()
                            if (!dateFormatMissing) {
                                dateFormat = config.format
                            }
                            partition = new DateModelIdentifierPartition()
                            partition.format = dateFormat
                            break
                        case 'literal':
                            String suffix
                            boolean suffixMissing = config.suffix instanceof ConfigObject ||
                                        config.suffix.isEmpty()
                            if (!suffixMissing) {
                                suffix = config.suffix
                            }
                            String isFixed = config.fixed ?: 'true'
                            partition = new LiteralModelIdentifierPartition(isFixed, suffix)
                            break
                        case 'numerical':
                            String width
                            boolean widthMissing = config.width instanceof ConfigObject ||
                                        config.width.isEmpty()
                            if (!widthMissing) {
                                width = config.width
                            }
                            String isFixed = config.fixed ?: 'true'
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

    /** Class for registering and updating ModelIdentifierPartition objects. */
    static class ModelIdentifierCursorPartitionRegistry {
        /* Associates character positions within a model id with corresponding partitions. */
        Map<Integer, ModelIdentifierPartition> registry
        /* The identifier to against which to match ModelIdentifierPartition objects. */
        final String ID
        /* the class logger */
        private static final Log log = LogFactory.getLog(this)

        /** default constructor */
        ModelIdentifierCursorPartitionRegistry() {
            registry = new HashMap<Integer, ModelIdentifierPartition>()
        }

        /**
         * Constructs a registry of a size proportionate to the supplied identifier.
         * Specifying this value will also trigger an update of the registered
         * ModelIdentifierParts to ensure that they use this identifier as a starting point
         * for deciding their next values.
         */
        ModelIdentifierCursorPartitionRegistry(String id) {
            if (!id) {
                registry = new HashMap<Integer, ModelIdentifierPartition>()
            } else {
                ID = id
                registry = new HashMap<Integer, ModelIdentifierPartition>(ID.length())
            }
        }

        /** Computes the partition's start- and end indices and then adds it to the registry. */
        boolean registerPartition(ModelIdentifierPartition partition) {
            int pWidth = partition.width
            String pValue = partition.value
            assert pWidth != null
            assert pValue != null
            final int OFFSET = registry.size()
            pWidth.times { v ->
                final int KEY = v + OFFSET
                registry[KEY] = partition
            }
            return incrementPartitionIndices(partition, OFFSET)
        }

        /**
         * Finds the starting position of a partition relative to the whole identifier.
         * Returns -1 in case of partition @p p not being in the registry.
         */
        int findPartitionStartIndex(ModelIdentifierPartition p) {
            Map.Entry<Integer, ModelIdentifierPartition> result = registry.sort{ it.key }.find{
                it.value == p
            }
            return result ? result.key : -1
        }

        /**
         * Returns the end position of a partition within the model identifier.
         * If the given partition @p p is not found in the registry, this method returns -1.
         */
        int findPartitionEndIndex(ModelIdentifierPartition p) {
            Map.Entry<Integer, ModelIdentifierPartition> result = registry.findAll{
                it.value == p
            }.sort{ it.key }.entrySet().last()
            return result ? result.key : -1
        }

        /** Updates the start and end indices of the supplied partition. */
        boolean updatePartitionIndices(ModelIdentifierPartition partition) {
            if (!partition) {
                log.error "Need a partition in order to update its indices."
                return false
            }
            final int START = findPartitionStartIndex(partition)
            if (-1 == START) {
                return registerPartition(partition)
            }
            return incrementPartitionIndices(partition, START)
        }

        /** Update the value of a partition based on the value of the ID constructor */
        boolean updatePartitionValue(ModelIdentifierPartition p) {
            if (!ID || p instanceof ChecksumModelIdentifierPartition) {
                //do nothing
                return true
            }
            final int START = findPartitionStartIndex(p)
            final int END  = findPartitionEndIndex(p)
            boolean incorrectWidth = END - START != p.width - 1
            if (incorrectWidth) {
                return false
            }
            log.info "Processing partition ${p.value} for $ID; width ${p.width} (${p.beginIndex} -> ${p.endIndex})"
            final String NEW_VALUE = ID[START..END]
            p.value = NEW_VALUE
            if (log.isDebugEnabled()) {
                log.debug "Model identifier partition $p has new value $NEW_VALUE"
            }
            return true
        }

        /* Helper method to synchronise the partition's indices with the registry. */
        private boolean incrementPartitionIndices(ModelIdentifierPartition p, int start) {
            if (start < 0) {
                log.error "Cannot accept a negative value for the offset of a partition."
                return false
            }
            p.beginIndex = start
            final int END = start + p.width - 1
            ModelIdentifierPartition actualPartition = registry[END]
            if (p != actualPartition) {
                return false
            }
            p.endIndex = END
            return true
        }
    }
}
