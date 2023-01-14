/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 **/

package net.biomodels.jummp.utils.redis

import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisService implements GrailsConfigurationAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisService.class)

    def configurationService

    //static JedisPool jedisPool
    static String REDIS_SRV_HOST //= grailsApplication.config.jummp.redis.host
    static int REDIS_SRV_PORT //= grailsApplication.config.jummp.redis.host.port
    static int REDIS_SRV_TIMEOUT //= grailsApplication.config.jummp.redis.timeout
    static String BM_SVR_URL //= grailsApplication.config.grails.serverURL
    static String CLASSIFIER_SVR_URL //= grailsApplication.config.jummp.classification.endpoint
    static String EBI_SEARCH_RESTFUL_WS_URL
    static String FIXED_PARAMS
    static Proxy proxy

    @Override
    void setConfiguration(ConfigObject co) {
        LOGGER.debug("Setting the configurations...")
        REDIS_SRV_HOST = co.jummp.redis.host
        REDIS_SRV_PORT = co.jummp.redis.port as int
        REDIS_SRV_TIMEOUT = co.jummp.redis.timeout as int
        BM_SVR_URL = co.grails.serverURL
        CLASSIFIER_SVR_URL = co.jummp.classification.endpoint
        EBI_SEARCH_RESTFUL_WS_URL = "https://www.ebi.ac.uk/ebisearch/ws/rest"
        FIXED_PARAMS = "query=*:*&size=0&facetfields"
        proxy = configurationService.verifyHttpProxy()
    }

    void doRedisHSet(final String key, Map data) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        pool.getResource().withCloseable { Jedis jedis ->
            deleteAllByPattern(jedis, key)
            jedis.hmset(key, data)
        }
        pool.close()
    }

    String doRedisHGet(final String key, final String field) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        String cachedData
        pool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.hget(key, field)
        }
        pool.close()
        cachedData
    }

    Map doRedisHGetAll(final String key) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        Jedis jedis = null
        Map returnedMap = new HashMap()
        try {
            jedis = pool.getResource()
            returnedMap = jedis.hgetAll(key)
        } finally {
            if (jedis) { jedis.close() }
        }
        pool.close()
        returnedMap
    }

    String doRedisGet(final String key) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        Jedis jedis = null
        String retVal = null
        try {
            jedis = pool.getResource()
            retVal = jedis.get(key)
        } finally {
            if (jedis) { jedis.close() }
        }
        pool.close()
        retVal
    }

    void doRedisSet(final String key, final String data) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        Jedis jedis = null
        try {
            jedis = pool.getResource()
            jedis.set(key, data)
        } finally {
            if (jedis) { jedis.close() }
        }
        pool.close()
    }

    synchronized static void deleteAllByPattern(final String pattern) {
        JedisPool pool = new JedisPool(new JedisPoolConfig(),
            REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        pool.getResource().withCloseable { Jedis jedis ->
            deleteAllByPattern(jedis, pattern)
        }
    }

    synchronized static deleteAllByPattern(final Jedis jedis, final String pattern) {
        Set<String> keys = jedis.keys(pattern)
        for (String key : keys) {
            if (jedis.exists(key)) {
                jedis.del(key)
            }
        }
    }
}
