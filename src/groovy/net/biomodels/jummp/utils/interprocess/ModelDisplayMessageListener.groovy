package net.biomodels.jummp.utils.interprocess

import redis.clients.jedis.JedisPubSub

class ModelDisplayMessageListener extends JedisPubSub {
    void onMessage(final String channel, final String message) {
        println("< Subscribe< channel: $channel > Message received: $message")
        // When a quit message is received, the subscription is canceled (passively)
        if (message.equalsIgnoreCase("quit")) {
            this.unsubscribe(channel)
        }
    }
}
