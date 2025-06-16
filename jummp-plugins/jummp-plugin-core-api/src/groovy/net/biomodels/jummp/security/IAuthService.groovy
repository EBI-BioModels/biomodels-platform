package net.biomodels.jummp.security

interface IAuthService {
    String doGenerateOTP(final String username, final String remoteAddress, final String sessionId)
    Map doVerifyOTP(final String username, final String otp, final String sessionId)
    List findAll(final String username, final String otp, final String sessionId)
    boolean isTrustDeviceExpired(final String strDateTime)

    /**
     * <h4>Validate the trust device of a given user</h4>
     * <p>This service is used to check the device of a given user needing to require two step verification.</p>
     * @param username The username of the user in question
     * @return true|false
     */
    Map validateTrustDevice(final String username, final String deviceInfo)
    /**
     * <h4>Determine a given user enabled 2FA or not</h4>
     * <p>This service is used to check a given user enabling 2FA or not.</p>
     * @param username The username of the given user
     * @param deviceInfo The string representing the information such IP address, Mobile or Desktop, User Agent,...
     * of the user's device.
     * @return a map including two elements: message showing the information and expired indicating true/false
     */

    boolean is2FAEnabled(final String username)
}
