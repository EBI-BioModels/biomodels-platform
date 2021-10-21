package net.biomodels.jummp.core.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPubSub

class ModelViewSubscriber extends JedisPubSub {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    void onMessage(final String channel, final String message) {
        super.onMessage(channel, message)
        //LOGGER.debug("< SUBSCRIBE < channel: $channel > Message received: $message")
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {

    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {

    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {

    }
}
