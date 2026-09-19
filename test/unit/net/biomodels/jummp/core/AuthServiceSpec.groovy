/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
**/

package net.biomodels.jummp.core

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.security.TwoFactorAuth
import spock.lang.Specification
import spock.lang.Unroll

/**
 * JBM-790: a one-time passcode is valid for 15 minutes after it was issued. The check used to look only at the minutes
 * and seconds of the elapsed time, so a code came back to life for the first 15 minutes of every hour after that.
 */
@TestFor(AuthService)
@Mock(TwoFactorAuth)
class AuthServiceSpec extends Specification {
    private static final Date NOW = new Date(1_800_000_000_000L)
    private static final long MINUTE = 60_000L

    private static Date issuedAgo(final long millis) {
        return new Date(NOW.time - millis)
    }

    @Unroll
    void "a code issued #age is #expected"() {
        expect:
        AuthService.isOtpValid(issuedAgo(millis), NOW) == expected

        where:
        age                    | millis                    || expected
        "just now"             | 0L                        || true
        "5 minutes ago"        | 5 * MINUTE                || true
        "14 min 59 s ago"      | 15 * MINUTE - 1_000L      || true
        "15 minutes ago"       | 15 * MINUTE               || false
        "15 min 1 s ago"       | 15 * MINUTE + 1_000L      || false
        "30 minutes ago"       | 30 * MINUTE               || false
        "59 minutes ago"       | 59 * MINUTE               || false
        // these came back to life: only the minutes and seconds of the elapsed time were compared
        "65 minutes ago"       | 65 * MINUTE               || false
        "70 minutes ago"       | 70 * MINUTE               || false
        "2 h 5 min ago"        | 125 * MINUTE              || false
        "a day and 5 min ago"  | (24 * 60 + 5) * MINUTE    || false
        "a week ago"           | 7 * 24 * 60 * MINUTE      || false
    }

    void "a code stamped a second in the future, from clock skew between servers, is still usable"() {
        expect:
        AuthService.isOtpValid(new Date(NOW.time + 1_000L), NOW)
    }

    void "a code without an issue date is never valid"() {
        expect:
        !AuthService.isOtpValid(null, NOW)
    }

    void "the clock defaults to the current time"() {
        expect:
        AuthService.isOtpValid(new Date())
        !AuthService.isOtpValid(new Date(System.currentTimeMillis() - 65 * MINUTE))
    }

    /* The service looks the code up with an HQL query, which the in-memory GORM of a unit test cannot run, so the two
     * static calls it makes are answered directly. */
    private static void storedCode(final Date issuedDate) {
        // the database returns the date as a java.sql.Timestamp
        def stored = new TwoFactorAuth(otp: "123456", sessionId: "S1", issuedDate: new java.sql.Timestamp(issuedDate.time))
        TwoFactorAuth.metaClass.static.executeQuery = { String query, Map args -> [1L] }
        TwoFactorAuth.metaClass.static.get = { Serializable id -> stored }
    }

    @Unroll
    void "verifying a code issued #age ends with matched=#matched"() {
        given:
        // doVerifyOTP reads the real clock, so the age is taken from it and not from the fixed one above
        storedCode(new Date(System.currentTimeMillis() - millis))

        when:
        Map result = service.doVerifyOTP("curator", "123456", "S1")

        then:
        result.matched == matched
        result.cause == cause

        where:
        age                | millis                 || matched | cause
        "5 minutes ago"    | 5 * MINUTE             || true    | null
        "20 minutes ago"   | 20 * MINUTE            || false   | "OTP expired. You can request a new one."
        "65 minutes ago"   | 65 * MINUTE            || false   | "OTP expired. You can request a new one."
        "2 h 5 min ago"    | 125 * MINUTE           || false   | "OTP expired. You can request a new one."
    }

    void "a code that has been accepted cannot be entered again"() {
        given:
        storedCode(new Date(System.currentTimeMillis() - 5 * MINUTE))

        when:
        Map first = service.doVerifyOTP("curator", "123456", "S1")
        Map second = service.doVerifyOTP("curator", "123456", "S1")

        then:
        first.matched
        !second.matched
        second.cause == "OTP expired. You can request a new one."
    }

    void "verifying a code that is not stored for this session is a mismatch"() {
        given:
        TwoFactorAuth.metaClass.static.executeQuery = { String query, Map args -> [] }

        when:
        Map result = service.doVerifyOTP("curator", "654321", "S1")

        then:
        !result.matched
        result.cause == "OTP mismatch. Try again or request a new one."
    }
}
