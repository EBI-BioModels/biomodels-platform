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

package net.biomodels.jummp.core.model.identifier.support

/**
 * @short Responsible for storing information about a part of a model identifier.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
abstract class ModelIdentifierPartition {
    /* The position, starting from zero, in the model identifier where this partition begins. */
    protected int beginIndex
    /* The position in the model identifier where this partition ends. */
    protected int endIndex
    /* The length of this partition. */
    protected int width
    /* The string which this partition holds. */
    protected String value


    int getBeginIndex() {
        return beginIndex
    }

    int getEndIndex() {
        return endIndex
    }

    int getWidth() {
        return width
    }

    String getValue() {
        return value
    }

    void setValue(final String value) {
        this.value = value
    }

    /* No access to servletContext or GORM, hence cannot use Grails validators .*/
    boolean validate() {
        boolean beginIndexOk = validateBeginIndex()
        boolean endIndexOk = validateEndIndex()
        boolean widthOk = validateWidth()
        boolean valueOk = validateValue()
        return beginIndexOk && endIndexOk && widthOk && valueOk
    }

    @Override
    boolean equals(Object o) {
        if (this.getClass() != o.getClass()) {
            return false
        }
        return this.width == o.width && this.value == o.value &&
                    this.beginIndex == o.beginIndex && this.endIndex == o.endIndex
    }

    @Override
    int hashCode() {
        int result = 19
        result = 31 * result + width
        result = 31 * result + value.hashCode()
        result = 31 * result + beginIndex
        result = 31 * result + endIndex
        return result
    }

    protected boolean validateBeginIndex() {
        return beginIndex >= 0
    }

    protected boolean validateEndIndex() {
        return endIndex > beginIndex
    }

    protected boolean validateWidth() {
        return width > 1
    }

    protected boolean validateValue() {
        return value
    }
}
