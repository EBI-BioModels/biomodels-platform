package net.biomodels.jummp.security

import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.plugins.security.BioModelsAuthSuccessHandler
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User

import spock.lang.Unroll

import java.util.concurrent.CopyOnWriteArrayList

/**
 * The one-time passcode step of a login, as the OTP form drives it: the real generateOTP and verifyOTP actions, the real
 * AuthService and its queries, on the in-memory database. It covers what a user meets (JBM-790): the code that was asked
 * for works once, a wrong one and an expired one are refused with the message the form shows, and asking again after an
 * expired code gives a code that works.
 *
 * <p>The mail is never sent: the user service, which the actions only use to find the current user and to hand the
 * message over, is replaced by a recorder, and the API keys of the mail providers are blanked as a second guard.
 * Requesting a code mails it on a background thread, so every request waits for that thread before anything is
 * restored.</p>
 */
class AuthControllerOtpITSpec extends IntegrationSpec {
    private static final long MINUTE = 60_000L
    private static final String EXPIRED = "OTP expired. You can request a new one."
    private static final String MISMATCH = "OTP mismatch. Try again or request a new one."

    def authService
    def grailsApplication

    /** A new instance from the real application context, so that it is wired with the real beans. */
    AuthController controller
    User user
    private def originalUserService
    private def originalBrevoKey
    private def originalSmtp2goKey
    private final List<Map> sent = new CopyOnWriteArrayList<Map>()
    private int mailsAwaited = 0

    def setup() {
        def mailerConfig = grailsApplication.config.jummp.security.mailer
        originalBrevoKey = mailerConfig.brevoApiKey
        originalSmtp2goKey = mailerConfig.smtp2goApiKey
        mailerConfig.brevoApiKey = ""
        mailerConfig.smtp2goApiKey = ""

        Person person = new Person(userRealName: "Otp Form Tester")
        person.save(flush: true, failOnError: true)
        user = new User(username: "otp-form-tester", password: "secret", person: person,
            email: "otp-form-tester@example.com", enabled: true, accountExpired: false, accountLocked: false,
            passwordExpired: false)
        user.save(flush: true, failOnError: true)

        controller = grailsApplication.mainContext.getBean(AuthController.name) as AuthController

        def stand = [
            currentUser: user,
            sendEmail  : { String to, String body, String subject -> sent << [to: to, body: body, subject: subject] }
        ]
        originalUserService = authService.userService
        authService.userService = stand
        controller.userService = stand
        // a login that is waiting for its code
        session.setAttribute("enabled2FA", true)
    }

    def cleanup() {
        authService.userService = originalUserService
        def mailerConfig = grailsApplication.config.jummp.security.mailer
        mailerConfig.brevoApiKey = originalBrevoKey
        mailerConfig.smtp2goApiKey = originalSmtp2goKey
    }

    private def getRequest() { controller.request }
    private def getResponse() { controller.response }
    private def getSession() { controller.session }

    private void storedCode(final String otp, final long ageMillis) {
        new TwoFactorAuth(user: user, otp: otp, sessionId: session.id,
            issuedDate: new Date(System.currentTimeMillis() - ageMillis)).save(flush: true, failOnError: true)
    }

    /** Presses "Request again": runs generateOTP, waits for the mailing thread and returns the code that was issued. */
    private String requestCode() {
        response.reset()
        controller.generateOTP()
        mailsAwaited++
        long giveUp = System.currentTimeMillis() + 10_000L
        while (sent.size() < mailsAwaited && System.currentTimeMillis() < giveUp) {
            Thread.sleep(10L)
        }
        assert sent.size() == mailsAwaited: "the verification code was never handed over for mailing"
        assert response.json.status == 200
        String body = sent.last().body
        List<TwoFactorAuth> mine = TwoFactorAuth.findAllByUserAndSessionId(user, session.id)
        return mine.find { body.contains(it.otp) && it.otp != "111111" }.otp
    }

    /** Presses "Verify" with the given code and returns what the form receives. */
    private Map verify(final String otp) {
        response.reset()
        request.method = "POST"
        request.json = [otp: otp, deviceInfo: "", isTrustDeviceChecked: "false"]
        controller.verifyOTP()
        return response.json as Map
    }

    void "the code that was asked for is accepted once and the login is no longer waiting"() {
        given:
        String otp = requestCode()

        when:
        Map first = verify(otp)
        boolean waitingAfterFirst = session.getAttribute("enabled2FA")
        Map second = verify(otp)

        then:
        first.matched
        first.postUrl == "/"
        !waitingAfterFirst
        !second.matched
        second.cause == EXPIRED
    }

    void "the login ends on the model page kept at the start, when there is one"() {
        given:
        session.setAttribute(BioModelsAuthSuccessHandler.POST_LOGIN_TARGET_URL, "https://www.biomodels.org/MODEL2609010001")
        String otp = requestCode()

        when:
        Map result = verify(otp)

        then:
        result.matched
        result.postUrl == "https://www.biomodels.org/MODEL2609010001"
    }

    void "a wrong code is refused with the mismatch message and the login keeps waiting"() {
        given:
        String otp = requestCode()
        String wrong = otp == "000000" ? "000001" : "000000"

        when:
        Map result = verify(wrong)

        then:
        !result.matched
        result.cause == MISMATCH
        session.getAttribute("enabled2FA")
        verify(otp).matched
    }

    @Unroll
    void "a code issued #minutesAgo minutes ago is refused with the expiry message and the login keeps waiting"() {
        given:
        storedCode("111111", minutesAgo * MINUTE)

        when:
        Map result = verify("111111")

        then:
        !result.matched
        result.cause == EXPIRED
        session.getAttribute("enabled2FA")

        where:
        minutesAgo << [20, 65, 125]
    }

    void "asking again after an expired code gives a code that works, and the expired one stays refused"() {
        given:
        storedCode("111111", 65 * MINUTE)

        when:
        Map expired = verify("111111")
        String fresh = requestCode()
        Map stillExpired = verify("111111")
        Map accepted = verify(fresh)

        then:
        !expired.matched
        expired.cause == EXPIRED
        fresh != "111111"
        !stillExpired.matched
        stillExpired.cause == EXPIRED
        accepted.matched
        !session.getAttribute("enabled2FA")
    }
}
