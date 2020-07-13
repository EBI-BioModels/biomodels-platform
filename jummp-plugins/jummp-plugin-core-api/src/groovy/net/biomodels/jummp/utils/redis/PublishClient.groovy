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

/**
 * @short An implementation for Publishers in Redis Pub/Sub mechanism
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class PublishClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    synchronized void publish(String channel, String message){
        LOGGER.debug("> Publish > channel: $channel  > Message sent: $message")
        Operations.jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.publish(channel, message)
        }
    }

    synchronized void close(String channel) {
        LOGGER.debug(">>> PUBLISH End > Channel: $channel > Message:quit")
        // The message publisher stops sending by sending a "quit" message
        Operations.jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.publish(channel, "quit")
        }
    }
}
