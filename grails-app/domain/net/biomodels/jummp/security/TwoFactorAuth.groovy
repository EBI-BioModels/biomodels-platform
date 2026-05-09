package net.biomodels.jummp.security

import grails.persistence.Entity
import net.biomodels.jummp.plugins.security.User

@Entity
class TwoFactorAuth implements Serializable {
    String otp
    Date issuedDate
    String sessionId

    static belongsTo = [user: User]

    static constraints = {
        otp nullable: false, blank: false, size: 6..6, unique: true
        issuedDate nullable: false, blank: false
        sessionId nullable: false, blank: false
    }
}
