package net.biomodels.jummp.core.subscribers

import net.biomodels.jummp.core.model.identifier.decorator.VariableDigitAppendingDecorator as VDAD
import net.biomodels.jummp.utils.redis.Operations
import redis.clients.jedis.JedisPubSub

class VariableDigitAppendingSubscriber extends JedisPubSub implements AbstractSubscriber {
    void onMessage(final String channel, final String message) {
        LOGGER.debug("< Subscribe < channel: $channel > Message received: $message")
        // Update the last counter on Redis Cache
        // Operations.doRedisSet(VDAD.REDIS_MODEL_ID_LAST_COUNT, message)
        // When a quit message is received, the subscription is canceled (passively)
        if (message.equalsIgnoreCase("quit")) {
            this.unsubscribe(channel)
        }
    }
}
