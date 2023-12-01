/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.scms

import grails.persistence.Entity
import grails.validation.Validateable
import net.biomodels.jummp.plugins.security.User

/**
 * Representation of the content of a static page which content is persisted in the database.
 * We don't need to redeploy to update such pages. They are updated via the simple CMS.
 *
 * Author: tnguyen@ebi.ac.uk, nvntung@gmail.com
 */
@Entity
@Validateable
class CmsContent implements Serializable {
    static MAX_CONTENT_SIZE = 500_000

    String title
    String description
    String content
    CmsContent parent
    String aliasURI
    User createdBy
    Date createdOn
    User lastChangedBy
    Date lastChangedOn

    static constraints = {
        content(nullable: true, maxSize: MAX_CONTENT_SIZE)
        parent(nullable: true, lazy: true)
        aliasURI(nullable: false, blank: false, unique: true)
        title(unique: true)
    }

    static mapping = {
        columns {
            content type: "text"
            aliasURI index: "content_aliasURI_Idx"
        }
    }
}
