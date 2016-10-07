package net.biomodels.jummp.core

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken as UPAT
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails

/**
 * Created by tnguyen on 30/06/16.
 */
class MyDaoAuthenticationProvider extends DaoAuthenticationProvider {
    protected void additionalAuthenticationChecks(UserDetails ud, UPAT auth) throws AuthenticationException {
    String hashedPassword = passwordEncoder.encodePassword(
         auth.credentials, null)
    boolean ok = passwordEncoder.isPasswordValid(
         ud.password, auth.credentials, null)
    //println "Cleartext: '$auth.credentials' from DB: '$ud.password' hashed '$hashedPassword' Valid: $ok"
    super.additionalAuthenticationChecks ud, auth
  }
}
