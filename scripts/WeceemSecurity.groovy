/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





// The policy closure must be assigned to the policy variable
policy = {

  // Here we define a role - this can be anything your authentication
  // system provides, but with Weceem Application edition, ROLE_GUEST, ROLE_USER,
  // and ROLE_ADMIN are used.
  // Note also that Weceem automatically adds the user's login as a special role
  // eg. user "fred" automatically has a role added called "USER_fred" for easy per-user
  // access control
  'ROLE_ADMIN' {
    // We're defining permissions for any space so use '*'. Alternatively
    // specify a list of space alias URIs eg: space 'internal', 'extranet' (no square brackets!)
    space '*'

    // Control whether this role can access Weceem admin functions eg edit/create spaces
    admin true

    // Control whether this role can create new content in this space
    create true

    // Control whether this role can edit content in this space
    edit true

    // Control whether this role can view content in this space
    view true

    // Control whether this role can delete content in this space
    delete true
  }

  'ROLE_USER' {
    space '*'
    admin false
    create true
    edit true
    view true
    delete false
  }

  'ROLE_GUEST' {
    space '*'
    admin false
    create false
    edit false
    view true
    delete false
  }

  'ROLE_ANONYMOUS' {
    space '*'
    admin false
    create false
    edit false
    view true
    delete false
  }
}
