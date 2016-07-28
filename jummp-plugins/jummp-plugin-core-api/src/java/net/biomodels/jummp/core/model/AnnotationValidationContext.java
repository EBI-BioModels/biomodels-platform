/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core.model;

import java.io.Serializable;
import java.util.List;

import static net.biomodels.jummp.core.model.ValidationState.*;

/**
 * Simple class holding information about the validity of a revision's annotations.
 *
 * @author Sarala Wimalaratne
 */
public class AnnotationValidationContext implements Serializable {
    private ValidationState validationLevel;

    private String validationReport;

    public void setValidationLevel(ValidationState validationLevel) {
        this.validationLevel = validationLevel;
    }

    public void setValidationReport(String validationReport) {
        this.validationReport = validationReport;
    }

    public ValidationState getValidationLevel() {
        return validationLevel;
    }

    public String getValidationReport() {
        return validationReport;
    }
}
