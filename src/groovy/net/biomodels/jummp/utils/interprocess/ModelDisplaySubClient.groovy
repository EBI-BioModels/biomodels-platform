package net.biomodels.jummp.utils.interprocess

import net.biomodels.jummp.utils.redis.Operations
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub

class ModelDisplaySubClient extends Thread {
    private String channel
    private JedisPubSub listener

    void setChannelAndListener(final String channel, final JedisPubSub listener){
        this.listener = listener
        this.channel = channel
    }

    private void subscribe(){
        if (listener==null || channel==null){
            println("Error: SubClient> listener or channel is null")
        }
        println(">>> SUBSCRIBE > Channel: $channel")
        // When the recipient is listening for subscribed messages, the process is blocked until the quit message is
        // received (passively) or the subscription is canceled actively
        Operations.instantiateJedisPool().getResource().withCloseable { Jedis jedis ->
            jedis.subscribe(listener, channel)
        }
    }
    void unsubscribe(final String channel) {
        println(">>> UNSUBSCRIBE > Channel: " + channel)
        Operations.instantiateJedisPool().getResource().withCloseable {
            listener.unsubscribe(channel)
        }
    }

    @Override
    void run() {
        try {
            println("---------Subscription begins-------")
            subscribe()
            println("---------Subscription ends-------")
        } catch (Exception e){
            e.printStackTrace()
        }
    }
}
