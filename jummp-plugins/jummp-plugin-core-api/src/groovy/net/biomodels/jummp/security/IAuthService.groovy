package net.biomodels.jummp.security

interface IAuthService {
    String doGenerateOTP(final String username, final String remoteAddress, final String sessionId)
    Map doVerifyOTP(final String username, final String otp, final String sessionId)
    List findAll(final String username, final String otp, final String sessionId)
    boolean isTrustDeviceExpired(final String strDateTime)
}
