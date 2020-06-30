package net.biomodels.jummp.utils.redis

import redis.clients.jedis.JedisPubSub

class ModelIdGenerationListener extends JedisPubSub {
    void onMessage(final String channel, final String message) {
        println("< Subscribe< channel: $channel > Message received: $message")
//        Operations.doRedisSet("last-model-identifier", message)
        Operations.doRedisSet("model-id-last-used-value", message)
        // When a quit message is received, the subscription is canceled (passively)
        if (message.equalsIgnoreCase("quit")) {
            this.unsubscribe(channel)
        }
    }
}
