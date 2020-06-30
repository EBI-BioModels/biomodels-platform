package net.biomodels.jummp.utils.redis

import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import redis.clients.jedis.Jedis

class PublishClient {
    private MTC model
    private RTC revision

    PublishClient() {

    }

    PublishClient(final MTC model, final RTC revision) {
        this.model = model
        this.revision = revision
    }

    static void publish(String channel, String message){
        println("> Publish> channel: $channel  > Message sent: $message")
        Operations.instantiateJedisPool().getResource().withCloseable { Jedis jedis ->
            jedis.publish(channel, message)
        }
    }
    void close(String channel){
        println(">>> PUBLISH End > Channel: $channel > Message:quit")
        // The message publisher stops sending by sending a “quit” message
        Operations.instantiateJedisPool().getResource().withCloseable { Jedis jedis ->
            jedis.publish(channel, "quit")
        }
    }
}
