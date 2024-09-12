/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.utils.redis

/**
 * @short Gathers all key patterns existing and being used in Redis Server
 *
 * <p>The class sets up a house where the constants of the Redis keys are defined and initialised.
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
final class KeyCollection {
    /**
     * The suffix of the Redis key storing the last value produced by an identifier
     * generator of a given type - e.g. MODEL2007030024, BIOMD0000000915
     */
    public static final String MODEL_ID_LAST_USED_VALUE_KEY_SUFFIX = "-id-last-used-value"
    /**
     * The suffix of the Redis key for the last count produced by the VariableDigitAppendingDecorator
     * of a given model id generator
     */
    public static final String MODEL_ID_LAST_COUNT_VALUE_KEY_SUFFIX = "-id-last-count"

    // The key pattern for the model identifier's last counter,
    // for example, 1, 2, 3...
    public static final String MODEL_ID_LAST_COUNT = "model-id-last-count"

    public static final String REDIS_CHANNEL_MODEL_VIEW = "ModelView"
    public static final String REDIS_CHANNEL_MODEL_ID_LAST_USED_VALUE = "RedisChannelModelIdLastUsedValue"
    public static final String REDIS_CHANNEL_MODEL_ID_LAST_USED_VALUE_SUFFIX = ":lastUsedValue"
    public static final String REDIS_CHANNEL_MODEL_ID_LAST_COUNT = "RedisChannelModelIdLastCount"
    public static final String REDIS_CHANNEL_MODEL_ID_LAST_COUNT_SUFFIX = ":lastIdCount"

    // We can use true|false; yes|no; 1|0 for this key's value. All these pairs work like a charm!
    public static final String DEBUGGING_MODE = "debugging-mode-enabled"
    public static final String SHOW_MODEL_LEVEL_ANNOTATIONS = "show-model-level-annotations"

    private KeyCollection() {}

    static String getLastModelIdValueChannelForType(String idType) {
        appendIdTypeToKeySuffix(idType, REDIS_CHANNEL_MODEL_ID_LAST_USED_VALUE_SUFFIX)
    }

    static String getLastModelIdCountForType(String idType) {
        appendIdTypeToKeySuffix(idType, REDIS_CHANNEL_MODEL_ID_LAST_COUNT_SUFFIX)
    }

    /**
     *
     * @param idType
     * @return never null;
     */
    static String getLastUsedIdValueKey(String idType) {
        appendIdTypeToKeySuffix idType, MODEL_ID_LAST_USED_VALUE_KEY_SUFFIX
    }

    static String getLastUsedIdCountKey(String idType) {
        appendIdTypeToKeySuffix idType, MODEL_ID_LAST_COUNT_VALUE_KEY_SUFFIX
    }

    private static String appendIdTypeToKeySuffix(String idType, String keySuffix) {
        if (null == idType || idType.isAllWhitespace()) {
            throw new IllegalArgumentException("No empty or null id type allowed")
        }
        if (null == keySuffix) {
            throw new IllegalArgumentException("Key suffix cannot be null")
        }
        return "${idType}${keySuffix}"
    }
}
