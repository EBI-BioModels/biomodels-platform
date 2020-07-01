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

package net.biomodels.jummp.core.subscribers

import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator as DMIG
import net.biomodels.jummp.utils.redis.Operations
import redis.clients.jedis.JedisPubSub

/**
 * @short An implementation of a specific subscriber to subscribe the event a newly generated model identifier
 * comes into existence
 *
 * <p>The override method onMessage will update the last used model identifier on Redis Cache.
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class ModelIdGenerationListener extends JedisPubSub implements AbstractSubscriber {
    void onMessage(final String channel, final String message) {
        LOGGER.debug("< Subscribe < channel: $channel > Message received: $message")
        Operations.doRedisSet(DMIG.REDIS_MODEL_ID_LAST_USED_VALUE, message)
        // When a quit message is received, the subscription is canceled (passively)
        if (message.equalsIgnoreCase("quit")) {
            this.unsubscribe(channel)
        }
    }
}
