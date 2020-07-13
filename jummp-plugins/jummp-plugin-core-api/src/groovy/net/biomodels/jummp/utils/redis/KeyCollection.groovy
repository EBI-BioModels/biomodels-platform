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
    // The key pattern for the model identifier's last used value,
    // for example, MODEL2007030024
    public static final String MODEL_ID_LAST_USED_VALUE = "model-id-last-used-value"

    // The key pattern for the model identifier's last counter,
    // for example, 1, 2, 3...
    public static final String MODEL_ID_LAST_COUNT = "model-id-last-count"

    // The key pattern for the model publication identifier's last used value,
    // for example, BIOMD0000000915
    public static final String PUB_ID_LAST_USED_VALUE = "model-pub-id-last-used-value"
    public static final String REDIS_CHANNEL_MODEL_VIEW = "ModelView"
    public static final String REDIS_CHANNEL_MODEL_ID_LAST_USED_VALUE = "RedisChannelModelIdLastUsedValue"
    public static final String REDIS_CHANNEL_MODEL_ID_LAST_COUNT = "RedisChannelModelIdLastCount"
}
