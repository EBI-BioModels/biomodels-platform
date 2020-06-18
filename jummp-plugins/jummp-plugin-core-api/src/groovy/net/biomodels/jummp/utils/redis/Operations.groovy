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
class Operations implements GrailsConfigurationAware {
    static String REDIS_SRV_HOST
    static Integer REDIS_SRV_PORT
    static Integer REDIS_SRV_TIMEOUT

    @Override
    void setConfiguration(ConfigObject co) {
        REDIS_SRV_HOST = REDIS_SRV_HOST ?: co.jummp.redis.host as String
        REDIS_SRV_PORT = REDIS_SRV_PORT ?: co.jummp.redis.port as Integer
        REDIS_SRV_TIMEOUT = REDIS_SRV_TIMEOUT ?: co.jummp.redis.timeout as Integer
    }

    static String doRedisHGet(final String key, final String field) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        String cachedData
        pool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.hget(key, field)
        }
        pool.close()
        cachedData
    }

    static String doRedisGet(final String key) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        String cachedData
        pool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.get(key)
        }
        pool.close()
        cachedData
    }

    static void doRedisHSet(final String key, Map data) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        pool.getResource().withCloseable { Jedis jedis ->
            deleteAllByPattern(jedis, key)
            jedis.hmset(key, data)
        }
        pool.close()
    }

    static void doRedisSet(final String key, final String value) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        pool.getResource().withCloseable { Jedis jedis ->
            jedis.set(key, value)
        }
        pool.close()
    }

    static void deleteAllByPattern(final String pattern) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        pool.getResource().withCloseable { Jedis jedis ->
            deleteAllByPattern(jedis, pattern)
        }
        pool.close()
    }

    static void deleteAllByPattern(final Jedis jedis, final String pattern) {
        Set<String> keys = jedis.keys(pattern)
        for (String key : keys) {
            if (jedis.exists(key)) {
                jedis.del(key)
            }
        }
    }
}
