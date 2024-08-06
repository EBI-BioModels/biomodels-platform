/**
 * Copyright (C) 2010-2023 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
class RedisService implements GrailsConfigurationAware, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisService.class)

    def configurationService
    def grailsApplication


    static JedisPool jedisPool
    static String REDIS_SRV_HOST //= grailsApplication.config.jummp.redis.host
    static int REDIS_SRV_PORT //= grailsApplication.config.jummp.redis.host.port
    static int REDIS_SRV_TIMEOUT //= grailsApplication.config.jummp.redis.timeout
    static String BM_SVR_URL //= grailsApplication.config.grails.serverURL
    static String CLASSIFIER_SVR_URL //= grailsApplication.config.jummp.classification.endpoint
    static String EBI_SEARCH_RESTFUL_WS_URL
    static String FIXED_PARAMS
    static Proxy proxy

    @Override
    void destroy() throws Exception {
        try {
            jedisPool.close()
            LOGGER.debug("Jedis Pool is being destroyed within 5 seconds...")
            Thread.sleep(5000)
        } catch(InterruptedException ex) {
            LOGGER.error("Errors while destroying Jedis Pool ${ex.message}. Closed ${jedisPool.isClosed()}")
            Thread.currentThread().interrupt()
        } finally {
            LOGGER.debug("Jedis Pool has been shutdown successfully")
        }
    }

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
        println "${new Date().format("yyyy-MM-dd HH:mm:ss")} ${this.getClass().name} LOADING CONFIG SERVICE..."

        proxy = configurationService.verifyHttpProxy()

        def config = new JedisPoolConfig()
        config.setJmxEnabled(true)
        config.setMaxTotal(50)
        config.setMaxIdle(50)
        jedisPool = new JedisPool(config, REDIS_SRV_HOST, REDIS_SRV_PORT, REDIS_SRV_TIMEOUT)
        if (jedisPool) {
            LOGGER.debug("Jedis Pool has been initialised successfully")
        } else {
            // Grails Runtime throws BeanCreationException preventing from starting the application
        }
    }

    synchronized static void doRedisHSet(final String key, final Map<String, String> data) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.hmset(key, data)
        }
    }

    synchronized static void doRedisHSetNX(final String key, final String field, final String value) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.hsetnx(key, field, value)
        }
    }

    synchronized static String doRedisHGet(final String key, final String field) {
        String cachedData = ""
        jedisPool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.hget(key, field)
        }
        cachedData
    }

    synchronized static Map doRedisHGetAll(final String key) {
        Map returnedMap = [:]
        jedisPool.getResource().withCloseable { Jedis jedis ->
            returnedMap = jedis.hgetAll(key)
        }
        returnedMap
    }

    synchronized static String doRedisGet(final String key) {
        String cachedData = ""
        jedisPool.getResource().withCloseable { Jedis jedis ->
            cachedData = jedis.get(key)
        }
        cachedData
    }

    synchronized static void doRedisSet(final String key, final String value) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.set(key, value)
        }
    }

    synchronized static void doRedisSAdd(final String key, final String... values) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.sadd(key, values)
        }
    }

    synchronized static Set<String> doRedisSMembers(final String key) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
            jedis.smembers(key)
        }
    }

    synchronized static void deleteAllByPattern(final String pattern) {
        jedisPool.getResource().withCloseable { Jedis jedis ->
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

    /**
     * This method mainly aims to remove the keys which the prefix is "spring:session:sessions:" created by
     * spring-session. This package is used to ensure session-based login worked as expected.
     *
     * @param pattern   A String indicating the given key
     * @return          A boolean value indicating the successful status of the deleting action
     */
    synchronized static boolean delNonExistedKeysByPattern(final String pattern) {
        boolean success = false
        jedisPool.getResource().withCloseable  {Jedis jedis ->
            Set<String> result = new HashSet<>()
            result = jedis.keys(pattern)
            Set<String> deletedKeys = new HashSet<>()
            if (!result?.isEmpty()) {
                for (String key: result) {
                    Long remainingTTL = jedis.ttl(key)
                    if (-2 == remainingTTL) {
                        jedis.del(key)
                        deletedKeys.add(key)
                        LOGGER.debug("deleting the key $key")
                    }
                }
            }
            result = jedis.keys(pattern)
            Set comItems = result.intersect(deletedKeys)
            success = 0 == comItems?.size()
        }
        return success
    }

    /**
     * Finds all keys by giving a pattern.
     *
     * @param pattern   A string indicating the given pattern
     * @return          A set of String objects indicating the existing keys
     */
    synchronized static Set<String> findKeyByPattern(final String pattern) {
        Set<String> result = null
        jedisPool.getResource().withCloseable {
            result = it.keys(pattern)
        }
        result
    }

    /**
     * Gets the remaining time to live (TTL) of a key that has a timeout.
     *
     * @param key   A string denoting the given key
     * @return      A Long positive value indicating the number of seconds a given key will continue to be part of
     * the dataset. It could be either -2 or -1 if and only if the key does not exist or exists but has no associated
     * expire.
     */
    synchronized static Long getRemainingTTL(final String key) {
        Long ttl = -1
        jedisPool.getResource().withCloseable {
            ttl = it.ttl(key)
        }
        ttl
    }

    synchronized static Boolean exists(final String key) {
        jedisPool.getResource().withCloseable {
            return it.exists(key)
        }
    }

    synchronized static Boolean hexists(final String key, final String field) {
        jedisPool.getResource().withCloseable {
            return it.hexists(key, field)
        }
    }
}
