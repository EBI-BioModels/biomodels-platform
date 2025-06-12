package net.biomodels.jummp.security

interface IAuthService {
    def doGenerateOTP(final String username, final String remoteAddress, final String sessionId)
}
