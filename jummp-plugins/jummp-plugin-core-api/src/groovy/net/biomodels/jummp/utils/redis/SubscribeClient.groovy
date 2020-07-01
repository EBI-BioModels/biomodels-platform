
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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub

/**
 * @short An implementation for Subscribers in Redis Pub/Sub mechanism
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class SubscribeClient extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(this.getClass().name)

    private String channel
    private JedisPubSub listener

    void setChannelAndListener(final String channel, final JedisPubSub listener){
        this.listener = listener
        this.channel = channel
    }

    private void subscribe() {
        if (listener==null || channel==null){
            LOGGER.error("Error: SubClient > listener or channel is null")
        }
        LOGGER.debug(">>> SUBSCRIBE > Channel: $channel")
        // When the recipient is listening for subscribed messages, the process is blocked until the quit message is
        // received (passively) or the subscription is canceled actively
        Operations.instantiateJedisPool().getResource().withCloseable { Jedis jedis ->
            jedis.subscribe(listener, channel)
        }
    }

    void unsubscribe(final String channel) {
        LOGGER.debug(">>> UNSUBSCRIBE > Channel: $channel")
        Operations.instantiateJedisPool().getResource().withCloseable {
            listener.unsubscribe(channel)
        }
    }

    @Override
    void run() {
        try {
            LOGGER.debug("---------Subscription begins-------")
            subscribe()
            LOGGER.debug("---------Subscription ends-------")
        } catch (Exception e){
            e.printStackTrace()
        }
    }
}
