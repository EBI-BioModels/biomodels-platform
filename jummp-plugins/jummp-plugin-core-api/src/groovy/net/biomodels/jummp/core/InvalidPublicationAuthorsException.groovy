/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core

import grails.util.Holders
import net.biomodels.jummp.plugins.security.PersonTransportCommand
import org.springframework.validation.ObjectError

/**
 * Exception class used for handling invalid authors provided at editing publication details.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class InvalidPublicationAuthorsException extends JummpValidationException {
    def messageSource = Holders.grailsApplication.mainContext.getBean("messageSource")

    List<PersonTransportCommand> invalidAuthors

    List<PersonTransportCommand> getInvalidAuthors() {
        return invalidAuthors
    }

    InvalidPublicationAuthorsException() {
        super()
        invalidAuthors = new ArrayList<>()
    }
    void setInvalidAuthors(List<PersonTransportCommand> invalidAuthors) {
        this.invalidAuthors = invalidAuthors
    }

    InvalidPublicationAuthorsException(String msg, List invalidAuthors) {
        super(msg)
        this.invalidAuthors = invalidAuthors
    }

    void addInvalidPublicationAuthor(PersonTransportCommand author) {
        invalidAuthors.add(author)
    }

    String getI18nErrorMessage4InvalidAuthor() {
        invalidAuthors.collect { PersonTransportCommand author ->
            author.errors.allErrors.collect { ObjectError error ->
                if (error.field == "orcid") {
                    // the following code is equivalent to error.codes[15]
                    String code = "personTransportCommand.orcid.validator.invalid"
                    messageSource.getMessage(code, [author.userRealName, author.orcid] as Object[], Locale.default)
                } else {
                    messageSource.getMessage(error, Locale.default)
                }
            }?.join("<br/>").toString()
        }?.join("<br/>").toString()
    }
}
