package net.biomodels.jummp.core

import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper

/**
 * Created by tnguyen on 08/07/16.
 */
class MyUserDetailsContextMapper implements UserDetailsContextMapper {
    UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                   Collection authorities) {

        String fullname = "Tung Nguyen" //ctx.originalAttrs.attrs['name'].values[0]
        String email = "tnguyen@ebi.ac.uk" //ctx.originalAttrs.attrs['mail'].values[0].toString().toLowerCase()
        def title = "Software Engineer" //ctx.originalAttrs.attrs['title']

        new MyUserDetails(username, null, true, true, true, true, authorities)
//                        title == null ? '' : title.values[0])
   }

   void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new IllegalStateException("Only retrieving data from AD is currently supported")
   }
}
