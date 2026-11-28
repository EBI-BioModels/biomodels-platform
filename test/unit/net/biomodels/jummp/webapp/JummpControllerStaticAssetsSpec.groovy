package net.biomodels.jummp.webapp

import grails.test.mixin.TestFor
import net.biomodels.jummp.utils.redis.RedisService
import spock.lang.Specification
import spock.lang.Unroll

/** The pages showing static assets get their base URL from the controller, set while it is created. */
@TestFor(JummpController)
class JummpControllerStaticAssetsSpec extends Specification {
    static class NoRedisService extends RedisService {
        @Override
        void setConfiguration(ConfigObject co) {}

        @Override
        void destroy() {}
    }

    static doWithConfig = { c ->
        c.grails.serverURL = "http://localhost:8080/biomodels"
        c.jummp.branding.style = "biomodels"
        c.jummp.context.help.root = "http://localhost:8080/biomodels/help"
        c.jummp.model.ftp.location = ""
        c.jummp.web.staticAssets.server = "https://assets.example.org/assets/"
    }
    static doWithSpring = {
        redisService(NoRedisService)
    }

    @Unroll
    void "#action passes the configured static assets URL to its view"() {
        when:
        controller."$action"()

        then:
        view == "/jummp/$action"
        model.bmStaticAssetsURL == "https://assets.example.org/assets"

        where:
        action << ["acknowledgements", "annotationInfo", "annotationTips"]
    }
}
