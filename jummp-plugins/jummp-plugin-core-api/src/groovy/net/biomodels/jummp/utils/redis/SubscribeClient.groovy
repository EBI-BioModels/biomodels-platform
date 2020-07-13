
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

import net.biomodels.jummp.core.subscribers.ModelIdGenerationListener
import net.biomodels.jummp.core.subscribers.ModelViewSubscriber
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * @short An implementation for Subscribers in Redis Pub/Sub mechanism
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class SubscribeClient {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass())
    private String channel
    JedisPubSub listener = new ModelViewSubscriber()

    ExecutorService executor
    Future future = null

    void setChannelAndListener(final String channel, final JedisPubSub listener) {
        this.listener = listener
        this.channel = channel
    }

    void subscribe() {
        if (listener==null || channel==null){
            LOGGER.error("Error: SubClient > listener or channel is null")
        }
        LOGGER.debug(">>> SUBSCRIBE > Channel: $channel")
        // When the recipient is listening for subscribed messages, the process is blocked until the quit message is
        // received (passively) or the subscription is canceled actively
        Operations.jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.subscribe(listener, channel)
        }
    }

    void unsubscribe(final String channel) {
        LOGGER.debug(">>> UNSUBSCRIBE > Channel: $channel")
        Operations.jedisPool.getResource().withCloseable {
            listener.unsubscribe(channel)
        }
    }

    synchronized void init() {
        executor = Executors.newFixedThreadPool(2)
        if (!executor.isTerminated()) {
            LOGGER.debug("Start background thread for getting responses from backend through Redis")
            executor = Executors.newFixedThreadPool(2)
            Runnable task = new RedisListenerTask(new ModelViewSubscriber(), KeyCollection.REDIS_CHANNEL_MODEL_VIEW)
            executor.execute(task)
            executor.execute(new RedisListenerTask(new ModelIdGenerationListener(),
                KeyCollection.REDIS_CHANNEL_MODEL_ID_LAST_USED_VALUE))
        } else {
            LOGGER.debug("Cannot start threading subscribers to poll Redis responses")
        }
    }

    synchronized void destroy() {
        if (future != null) {
            try {
                listener.unsubscribe()
                executor.shutdownNow()
                future = null
                executor = null
                LOGGER.debug("Background thread for getting responses through Redis closed")
            } catch (Exception ex) {
                LOGGER.error("Error in destroying Redis listener", ex)
            }
        }
    }

    class RedisListenerTask implements Runnable {
        private final Logger LOGGER = LoggerFactory.getLogger(this.getClass())
        private JedisPubSub listener
        private String channel

        RedisListenerTask(final JedisPubSub listener, final String channel) {
            this.listener = listener
            this.channel = channel
        }

        void run() {
            Jedis jedis = null
            try {
                Operations.jedisPool.getResource().withCloseable { Jedis jedis1 ->
                    jedis = jedis1
                    LOGGER.debug("Subscribing to the channel $channel")
                    jedis.subscribe(listener, channel)
                }
            } catch (Exception ex) {
                LOGGER.error("Error", ex)
            } finally {
                if (jedis != null)
                    jedis.quit()
                jedis = null
            }
        }
    }
}
