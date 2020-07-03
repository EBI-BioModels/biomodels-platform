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
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 * @short Implementations of basic operations on Redis Server with Jedis
 * <p>This class gathers essential operations to get and set values with their
 * keys from and on Redis Server.
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
class Operations implements InitializingBean, DisposableBean {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass())
    private static String REDIS_SRV_HOST
    private static Integer REDIS_SRV_PORT
    private static Integer REDIS_SRV_TIMEOUT
    static JedisPool jedisPool
    def grailsApplication

    static String doRedisHGet(final String key, final String field) {
        String cachedData
        jedisPool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.hget(key, field)
        }
        cachedData
    }

    static String doRedisGet(final String key) {
        String cachedData
        jedisPool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.get(key)
        }
        cachedData
    }

    static void doRedisHSet(final String key, Map data) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
            deleteAllByPattern(jedis, key)
            jedis.hmset(key, data)
        }
    }

    static void doRedisSet(final String key, final String value) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.set(key, value)
        }
    }

    static void deleteAllByPattern(final String pattern) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
            deleteAllByPattern(jedis, pattern)
        }
    }

    static void deleteAllByPattern(final Jedis jedis, final String pattern) {
        Set<String> keys = jedis.keys(pattern)
        for (String key : keys) {
            if (jedis.exists(key)) {
                jedis.del(key)
            }
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        REDIS_SRV_HOST = grailsApplication.config.jummp.redis.host ?: "localhost"
        REDIS_SRV_PORT = grailsApplication.config.jummp.redis.port ?: 6379
        REDIS_SRV_TIMEOUT = grailsApplication.config.jummp.redis.timeout ?: 3600
    @Override
    void destroy() throws Exception {
        try {
            jedisPool.close()
            LOGGER.debug("Jedis Pool is being destroyed within 5 seconds...")
            Thread.sleep(5000)
        } catch(InterruptedException ex) {
            LOGGER.error("Errors while trying to destroy Jedis Pool ${ex.message}")
            Thread.currentThread().interrupt()
        }
    }
}
