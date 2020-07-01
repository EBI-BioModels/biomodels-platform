package net.biomodels.jummp.core.subscribers

import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator as DMIG
import net.biomodels.jummp.utils.redis.Operations
import redis.clients.jedis.JedisPubSub

class ModelIdGenerationListener extends JedisPubSub implements AbstractSubscriber {
    void onMessage(final String channel, final String message) {
        LOGGER.debug("< Subscribe < channel: $channel > Message received: $message")
        Operations.doRedisSet(DMIG.REDIS_MODEL_ID_LAST_USED_VALUE, message)
        // When a quit message is received, the subscription is canceled (passively)
        if (message.equalsIgnoreCase("quit")) {
            this.unsubscribe(channel)
        }
    }
}
