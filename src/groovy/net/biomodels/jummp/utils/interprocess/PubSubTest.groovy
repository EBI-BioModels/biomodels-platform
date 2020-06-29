package net.biomodels.jummp.utils.interprocess

import groovyx.gpars.GParsPool
import redis.clients.jedis.JedisPubSub

class PubSubTest {
    static void main() {
        ModelDisplayPubClient pubClient = new ModelDisplayPubClient()
        final String channel = "Channel: Model Identifier Generator"
        // The message sender starts sending messages, but there are no subscribers,
        // so the messages will not be received
        String msg = "Redis Server MESSAGE 1: (No subscribers. This message will not be received)"
        pubClient.publish(channel, msg)

        // Message recipient
        ModelDisplaySubClient subClient = new ModelDisplaySubClient()
        JedisPubSub listener = new ModelDisplayMessageListener()
        subClient.setChannelAndListener(channel, listener)
        // Message recipient starts subscribing
        subClient.start()
        // The message sender continues sending messages
        final int POOL_SIZE = 8
        GParsPool.withPool(POOL_SIZE) {
            (1..7).eachParallel {
                synchronized (this) {
                    String message = UUID.randomUUID().toString()
                    pubClient.publish(channel, message)
                }
            }
        }

        // The message recipient cancels the subscription
        subClient.unsubscribe(channel)
        msg = "Redis Server MESSAGE 2: (Subscription canceled. This message will not be received)"
        pubClient.publish(channel, msg)
        // The message publisher stops sending by sending a “quit” message
        // When other message recipients, if any, receive "quit" in listener.onMessage(), the "unsubscribe" operation is performed.
        pubClient.close(channel)
    }
}
