package net.biomodels.jummp

import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.utils.redis.RedisService
import redis.clients.jedis.Jedis

/**
 * Covers JBM-776: an integration test run must not share resources with an application running on the same machine.
 * These check the effective, environment-specific configuration - the one Config.groovy really gives the test
 * environment - rather than a hand-made one.
 */
class TestEnvironmentIsolationITSpec extends IntegrationSpec {
    def grailsApplication
    def mailingService

    private static String valueIn(final int database, final String key) {
        new Jedis(RedisService.REDIS_SRV_HOST, RedisService.REDIS_SRV_PORT).withCloseable { Jedis jedis ->
            jedis.select(database)
            jedis.get(key)
        }
    }

    void "the tests work on a Redis database of their own, not the default one an application uses"() {
        given: "a key nothing else uses"
        String key = "jbm776-isolation-probe-" + UUID.randomUUID()

        when:
        RedisService.doRedisSet(key, "probe")

        then:
        grailsApplication.config.jummp.redis.database == 15
        RedisService.REDIS_SRV_DATABASE == 15
        valueIn(15, key) == "probe"
        valueIn(0, key) == null

        cleanup:
        RedisService.doRedisDel(key)
    }

    void "Spring Session stores the sessions on that same Redis database, not on database 0"() {
        expect:
        grailsApplication.config.springsession.redis.connectionFactory.dbIndex == 15
        grailsApplication.mainContext.getBean("redisConnectionFactory").database == 15
    }

    void "the tests do not send mail, whatever mail account the developer has configured"() {
        given: "the real Brevo/smtp2go keys blanked, so that a guard that fails cannot send a real message either"
        def mailerConfig = grailsApplication.config.jummp.security.mailer
        def originalBrevoKey = mailerConfig.brevoApiKey
        def originalSmtp2goKey = mailerConfig.smtp2goApiKey
        mailerConfig.brevoApiKey = ""
        mailerConfig.smtp2goApiKey = ""
        def originalMailService = mailingService.mailService
        List dispatched = []
        mailingService.mailService = [sendMail: { Closure mail -> dispatched << mail }]

        when:
        mailingService.send([to: "someone@example.com", subject: "JBM-776 probe", text: "must not be sent"])

        then:
        !mailingService.mailingEnabled
        dispatched.isEmpty()

        cleanup:
        mailingService.mailService = originalMailService
        mailerConfig.brevoApiKey = originalBrevoKey
        mailerConfig.smtp2goApiKey = originalSmtp2goKey
    }
}
