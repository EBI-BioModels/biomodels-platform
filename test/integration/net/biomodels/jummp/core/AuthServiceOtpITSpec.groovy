package net.biomodels.jummp.core

import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.security.TwoFactorAuth
import spock.lang.Unroll

import java.util.concurrent.CopyOnWriteArrayList

/**
 * JBM-790: doGenerateOTP decided whether to reuse the code the user already has by looking only at the minutes and
 * seconds of its age, so a code older than an hour that fell in the first 15 minutes of an hour was sent again instead
 * of a new one being issued. These run the real doGenerateOTP and doVerifyOTP, with their HQL queries, on the in-memory
 * database.
 *
 * <p>The mail is never sent: the user service, whose only use here is looking up the current user and handing the
 * message over, is replaced by a recorder, and the API keys of the mail providers are blanked as a second guard.
 * doGenerateOTP mails the code on a background thread, so every test waits for that thread before it restores anything.
 * </p>
 */
class AuthServiceOtpITSpec extends IntegrationSpec {
    private static final long MINUTE = 60_000L
    private static final String SESSION = "S1"
    private static final String OLD_CODE = "111111"

    def authService
    def grailsApplication

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

        Person person = new Person(userRealName: "Otp Tester")
        person.save(flush: true, failOnError: true)
        user = new User(username: "otp-tester", password: "secret", person: person, email: "otp-tester@example.com",
            enabled: true, accountExpired: false, accountLocked: false, passwordExpired: false)
        user.save(flush: true, failOnError: true)

        originalUserService = authService.userService
        authService.userService = [
            currentUser: user,
            sendEmail  : { String to, String body, String subject ->
                sent << [to: to, body: body, subject: subject]
            }
        ]
    }

    def cleanup() {
        authService.userService = originalUserService
        def mailerConfig = grailsApplication.config.jummp.security.mailer
        mailerConfig.brevoApiKey = originalBrevoKey
        mailerConfig.smtp2goApiKey = originalSmtp2goKey
    }

    private TwoFactorAuth storedCode(final String otp, final long ageMillis, final String session = SESSION) {
        return new TwoFactorAuth(user: user, otp: otp, sessionId: session,
            issuedDate: new Date(System.currentTimeMillis() - ageMillis)).save(flush: true, failOnError: true)
    }

    /** Runs doGenerateOTP and waits for the mail that it hands over on a background thread. */
    private String generate(final String session = SESSION) {
        String otp = authService.doGenerateOTP(user.username, "127.0.0.1", session)
        mailsAwaited++
        long giveUp = System.currentTimeMillis() + 10_000L
        while (sent.size() < mailsAwaited && System.currentTimeMillis() < giveUp) {
            Thread.sleep(10L)
        }
        assert sent.size() == mailsAwaited: "the verification code was never handed over for mailing"
        return otp
    }

    private int codesOf(final String session = SESSION) {
        return TwoFactorAuth.countByUserAndSessionId(user, session)
    }

    void "a user with no code for the session gets a new one, which is mailed to them"() {
        when:
        String otp = generate()

        then:
        otp ==~ /\d{6}/
        codesOf() == 1
        sent.size() == 1
        sent.first().to == "otp-tester@example.com"
        sent.first().subject == "[BioModels] Your Verification Code"
        sent.first().body.contains(otp)
    }

    void "a code issued 5 minutes ago is sent again instead of issuing another one"() {
        given:
        storedCode(OLD_CODE, 5 * MINUTE)

        when:
        String otp = generate()

        then:
        otp == OLD_CODE
        codesOf() == 1
        sent.first().body.contains(OLD_CODE)
    }

    @Unroll
    void "a code issued #age is replaced by a new one, not sent again"() {
        given:
        storedCode(OLD_CODE, ageMillis)

        when:
        String otp = generate()

        then:
        otp ==~ /\d{6}/
        otp != OLD_CODE
        codesOf() == 2
        sent.first().body.contains(otp)
        !sent.first().body.contains(OLD_CODE)

        where:
        age                     | ageMillis
        "20 minutes ago"        | 20 * MINUTE
        "59 minutes ago"        | 59 * MINUTE
        // these were judged by their minutes alone, so they were reused
        "65 minutes ago"        | 65 * MINUTE
        "70 minutes ago"        | 70 * MINUTE
        "2 h 5 min ago"         | 125 * MINUTE
        "a day and 5 min ago"   | (24 * 60 + 5) * MINUTE
    }

    void "the new code verifies and the one it replaced stays expired"() {
        given:
        storedCode(OLD_CODE, 65 * MINUTE)

        when:
        String otp = generate()
        Map fresh = authService.doVerifyOTP(user.username, otp, SESSION)
        Map old = authService.doVerifyOTP(user.username, OLD_CODE, SESSION)

        then:
        fresh.matched
        !old.matched
        old.cause == "OTP expired. You can request a new one."
    }

    void "a code that has been accepted cannot be entered again"() {
        given:
        String otp = generate()

        when:
        Map first = authService.doVerifyOTP(user.username, otp, SESSION)
        Map second = authService.doVerifyOTP(user.username, otp, SESSION)

        then:
        first.matched
        !second.matched
        second.cause == "OTP expired. You can request a new one."
    }

    void "a wrong code does not use up the right one"() {
        given:
        String otp = generate()
        String wrong = otp == "000000" ? "000001" : "000000"

        when:
        Map miss = authService.doVerifyOTP(user.username, wrong, SESSION)
        Map hit = authService.doVerifyOTP(user.username, otp, SESSION)

        then:
        !miss.matched
        miss.cause == "OTP mismatch. Try again or request a new one."
        hit.matched
    }

    void "using a code keeps its row, which is what says that the user has 2FA on"() {
        given:
        String otp = generate()
        assert authService.is2FAEnabled(user.username)

        when:
        authService.doVerifyOTP(user.username, otp, SESSION)

        then:
        codesOf() == 1
        authService.is2FAEnabled(user.username)
    }

    void "after a code was used, asking again issues a new one that works"() {
        given:
        String used = generate()
        authService.doVerifyOTP(user.username, used, SESSION)

        when:
        String fresh = generate()

        then:
        fresh ==~ /\d{6}/
        fresh != used
        codesOf() == 2
        authService.doVerifyOTP(user.username, fresh, SESSION).matched
        !authService.doVerifyOTP(user.username, used, SESSION).matched
    }

    void "a code kept for another session is not reused"() {
        given:
        storedCode(OLD_CODE, 5 * MINUTE, "another-session")

        when:
        String otp = generate()

        then:
        otp != OLD_CODE
        codesOf() == 1
        codesOf("another-session") == 1
    }
}
