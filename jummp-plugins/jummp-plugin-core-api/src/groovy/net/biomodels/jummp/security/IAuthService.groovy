package net.biomodels.jummp.security

interface IAuthService {
    String doGenerateOTP(final String username, final String remoteAddress, final String sessionId)
    boolean doVerifyOTP(final String username, final String otp, final String sessionId)
}
