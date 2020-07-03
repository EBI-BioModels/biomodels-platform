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

import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 * @short Implementations of basic operations on Redis Server with Jedis library
 *
 * <p>This class gathers essential operations, for example, get and set values with their
 * keys from and on Redis Server via using Jedis library.
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
class Operations implements GrailsConfigurationAware, DisposableBean {
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
    void setConfiguration(ConfigObject co) {
        REDIS_SRV_HOST = co.jummp.redis.host
        REDIS_SRV_PORT = co.jummp.redis.port
        REDIS_SRV_TIMEOUT = co.jummp.redis.timeout
        jedisPool = new JedisPool(new JedisPoolConfig(), REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        if (jedisPool) {
            LOGGER.debug("Jedis Pool has been initialised successfully")
        } else {
            // Grails Runtime throws BeanCreationException preventing from starting the application
        }
    }

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
