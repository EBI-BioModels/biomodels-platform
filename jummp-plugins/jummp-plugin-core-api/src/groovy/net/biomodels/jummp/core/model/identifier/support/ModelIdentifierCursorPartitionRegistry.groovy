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
 * Class for registering and updating ModelIdentifierPartition objects.
 *
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class ModelIdentifierCursorPartitionRegistry {
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
