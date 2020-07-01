package net.biomodels.jummp.utils.redis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis

class PublishClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(this.getClass().name)

    static void publish(String channel, String message){
        LOGGER.debug("> Publish> channel: $channel  > Message sent: $message")
        Operations.instantiateJedisPool().getResource().withCloseable { Jedis jedis ->
            jedis.publish(channel, message)
        }
    }

    void close(String channel) {
        LOGGER.debug(">>> PUBLISH End > Channel: $channel > Message:quit")
        // The message publisher stops sending by sending a "quit" message
        Operations.instantiateJedisPool().getResource().withCloseable { Jedis jedis ->
            jedis.publish(channel, "quit")
        }
    }
}
