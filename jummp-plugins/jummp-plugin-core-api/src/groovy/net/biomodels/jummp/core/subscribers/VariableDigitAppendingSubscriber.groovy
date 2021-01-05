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

import net.biomodels.jummp.core.model.identifier.decorator.VariableDigitAppendingDecorator as VDAD
import net.biomodels.jummp.utils.redis.Operations
import redis.clients.jedis.JedisPubSub

/**
 * @short An implementation of a specific subscriber to react depending on updating the variable digits
 * appended to the model identifier.
 *
 * <p>The override method onMessage will update the value of the last counter on Redis Cache.
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class VariableDigitAppendingSubscriber extends JedisPubSub implements AbstractSubscriber {
    void onMessage(final String channel = VDAD.REDIS_MODEL_ID_LAST_COUNT, final String message) {
        super.onMessage(channel, message)
        LOGGER.debug("< Subscribe < channel: $channel > Message received: $message")
        // Update the last counter on Redis Cache
        // Operations.doRedisSet(VDAD.REDIS_MODEL_ID_LAST_COUNT, message)
        // When a quit message is received, the subscription is canceled (passively)
        if (message.equalsIgnoreCase("quit")) {
            this.unsubscribe(channel)
        }
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {

    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {

    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {

    }
}
