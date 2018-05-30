/**
* Copyright (C) 2010-2017 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import net.biomodels.jummp.Feedback
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short A light-weight feedback mechanism
 * This class is introduced to handle users' rating/feedback on the site.
 * Basically the star ranges from 1 to 5 stars.
 * The email and additional comment are optional.
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @date 14/09/2017
 */

class FeedbackService {
    static final Log log = LogFactory.getLog(FeedbackService.class)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_ERROR_ENABLED = log.isErrorEnabled()
    boolean persist(byte star, String email, String comment) {
        Feedback feedback = new Feedback(star: star, email: email, comment: comment)
        if (!feedback.save(flush: true)) {
            String logMsg = "${feedback.errors.allErrors.inspect()}"
            if (IS_ERROR_ENABLED) {
                log.error(logMsg)
            } else {
                println(logMsg)
            }
            return false
        }
        return true
    }
}
