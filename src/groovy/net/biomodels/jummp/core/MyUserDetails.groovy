package net.biomodels.jummp.core

import net.biomodels.jummp.plugins.security.User
import org.springframework.security.core.GrantedAuthority

/**
 * Created by tnguyen on 08/07/16.
 */
class MyUserDetails extends User {
    // extra instance variables

   MyUserDetails(String username, String password, boolean enabled, boolean accountNonExpired,
                 boolean credentialsNonExpired, boolean accountNonLocked,
                 Collection<GrantedAuthority> authorities) {

      super(username, password, enabled, accountNonExpired, credentialsNonExpired,
            accountNonLocked, authorities)
   }
}
