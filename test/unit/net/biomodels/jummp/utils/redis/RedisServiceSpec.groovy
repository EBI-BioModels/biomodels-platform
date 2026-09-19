package net.biomodels.jummp.utils.redis

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Covers JBM-776: the tests must not share Redis keys with an application running against the same server, so the
 * logical database of the connection pool is configurable (Config.groovy gives the test environment its own).
 */
class RedisServiceSpec extends Specification {
    private int originalDatabase
    private def originalPool

    void setup() {
        originalDatabase = RedisService.REDIS_SRV_DATABASE
        originalPool = RedisService.jedisPool
    }

    void cleanup() {
        // setConfiguration() replaces static state that other specs (and the beans they define) rely on
        RedisService.jedisPool?.close()
        RedisService.jedisPool = originalPool
        RedisService.REDIS_SRV_DATABASE = originalDatabase
    }

    private static ConfigObject configWithDatabase(def database) {
        ConfigObject co = new ConfigObject()
        co.jummp.redis.host = "localhost"
        co.jummp.redis.port = 6379
        co.jummp.redis.timeout = 2000
        if (database != null) {
            co.jummp.redis.database = database
        }
        co
    }

    @Unroll
    void "databaseIndexFrom reads #configured as database #expected"() {
        expect:
        RedisService.databaseIndexFrom(configWithDatabase(configured)) == expected

        where:
        configured | expected
        null       | 0
        0          | 0
        15         | 15
        "7"        | 7
    }

    void "setConfiguration makes the connection pool work on the configured database"() {
        when:
        new RedisService().setConfiguration(configWithDatabase(15))

        then:
        RedisService.REDIS_SRV_DATABASE == 15
        RedisService.jedisPool != null
    }

    void "setConfiguration falls back to Redis' default database when none is configured"() {
        given:
        RedisService.REDIS_SRV_DATABASE = 15

        when:
        new RedisService().setConfiguration(configWithDatabase(null))

        then:
        RedisService.REDIS_SRV_DATABASE == 0
    }
}
