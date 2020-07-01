package net.biomodels.jummp.utils.redis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub

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
