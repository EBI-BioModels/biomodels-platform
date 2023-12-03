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
 */

package net.biomodels.jummp.scms

import grails.validation.Validateable
import net.biomodels.jummp.plugins.security.User

/**
 * @short Data Transfer Object (DTO) for the instance of the {@link CmsContent} domain class.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Validateable
class CmsContentTransportCommand implements Serializable {
    Long id
    String title
    String description
    String content
    String aliasURI
    String createdBy
    Date createdOn
    String lastChangedBy
    Date lastChangedOn
    String parentAliasURI

    static constraint = {
        //importFrom(CmsContent)
        id nullable: true
        parentAliasURI nullable: true
    }

    Long getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
    }

    String getTitle() {
        return title
    }

    void setTitle(String title) {
        this.title = title
    }

    String getDescription() {
        return description
    }

    void setDescription(String description) {
        this.description = description
    }

    String getContent() {
        return content
    }

    void setContent(String content) {
        this.content = content
    }

    String getAliasURI() {
        return aliasURI
    }

    void setAliasURI(String aliasURI) {
        this.aliasURI = aliasURI
    }

    String getCreatedBy() {
        return createdBy
    }

    void setCreatedBy(String createdBy) {
        this.createdBy = createdBy
    }

    Date getCreatedOn() {
        return createdOn
    }

    void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn
    }

    String getLastChangedBy() {
        return lastChangedBy
    }

    void setLastChangedBy(String lastChangedBy) {
        this.lastChangedBy = lastChangedBy
    }

    Date getLastChangedOn() {
        return lastChangedOn
    }

    void setLastChangedOn(Date lastChangedOn) {
        this.lastChangedOn = lastChangedOn
    }

    static CmsContentTransportCommand toCommandObject(CmsContent obj) {
        new CmsContentTransportCommand(id: obj.id, title: obj.title, description: obj.description,
            content: obj.content, aliasURI: obj.aliasURI, parentAliasURI: obj.parent?.aliasURI,
            createdBy: obj.createdBy.username, createdOn: obj.createdOn,
            lastChangedBy: obj.lastChangedBy.username, lastChangedOn: obj.lastChangedOn)
    }

    String toString() {
        """[$id] $title: $aliasURI: $description, created on $createdOn by $createdBy, \
updated on $lastChangedOn by $lastChangedBy""".toString()
    }
}
