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
 
 package net.biomodels.jummp.core.model.identifier.support

import net.biomodels.jummp.core.model.identifier.decorator.ChecksumAppendingDecorator
import net.biomodels.jummp.core.model.identifier.support.date.DateFormatRegion
import net.biomodels.jummp.core.model.identifier.support.date.DateFormatRegionType

import java.util.regex.Pattern

/**
 * Factory methods for generating regular expressions for model identifier partitions.
 *
 * Created by mglont on 28/09/17.
 */
final class ModelIdentifierPartitionRegexFactory {
    private ModelIdentifierPartitionRegexFactory() { /* static only */}

    /**
     * Returns the regular expression for the given token.
     *
     * @param token the non-null suffix of a {@link LiteralModelIdentifierPartition}.
     * @return the supplied suffix as a quoted pattern, so the suffix can't be evaluated as a regex
     * @throws NullPointerException if @p token is null
     */
    static String forLiteral(String token) {
        Objects.requireNonNull token
        Pattern.quote(token)
    }

    /**
     * Creates the regular expression for a NumericalModelIdentifierPartition of a given size.
     *
     * @param width the size of the partition
     * @return a regular expression denoting @p width digits
     */
    static String forNumericalPartition(int width) {
        if (width < 1)
            throw new IllegalArgumentException("Unexpected width $width for model id partition")
        getRegexForNumericRegion(width)
    }

    /**
     * Creates the regular expression for a DateModelIdentifierPartition.
     *
     * @param dateFormat a URL-safe date format complying with DateModelIdentifierPartition
     *        requirements.
     * @return the regular expression that matches the supplied date format.
     */
    static String forDatePartition(String dateFormat) {
        Objects.requireNonNull dateFormat
        def regions = buildDateFormatRegions(dateFormat, 0, new DateFormatRegion(), [])
        regions.inject(new StringBuilder(), { sb, r ->
            sb.append getRegexForRegion(r)
        }).toString()
    }

    /**
     * Creates the regular expression for a ChecksumModelIdentifierPartition.
     *
     * @param the separator used by the ChecksumAppendingDecorator for which we are inferring the
     *        regular expression pattern
     * @return the regular expression comprising alphanumerical characters as well as the separator
     */
    static String forChecksumPartition(String separator) {
        int suffixWidth = ChecksumAppendingDecorator.TOTAL_WIDTH - 1
        "${Pattern.quote(separator)}\\p{Alnum}{$suffixWidth}".toString()
    }

    /*
     * Simple parser for accepted date formats that helps converting them to the corresponding
     * regular expressions.
     *
     * Splits the date format into regions -- streams of similar characters, for example yyyy-MM-dd
     * would be converted into a region for the year (yyyy), one for the month (MM), one for the day
     * (dd) and two regions for the separator characters.
     * Once we know a region's type and width, we know how its output will be formatted, so we can
     * produce the matching regex.
     */
    private static List<DateFormatRegion> buildDateFormatRegions(String format, int index,
            DateFormatRegion previousRegion, List<DateFormatRegion> formatRegions) {
        Objects.requireNonNull format
        int formatLength = format.length()
        if (index < 0 )
            throw new IllegalArgumentException("DateFormat index cannot be a negative number")
        if (index == formatLength) {
            if (previousRegion.contents) {
                formatRegions.add previousRegion
            }
            return formatRegions
        }

        DateFormatRegion regionForNextIteration = previousRegion
        char thisCharacter = format.charAt(index)
        if (!previousRegion.add(thisCharacter)) {
            formatRegions.add(previousRegion)
            DateFormatRegion currentRegion = new DateFormatRegion()
            if (!currentRegion.add(thisCharacter)) {
                throw new IllegalStateException("Internal error while attempting to parse $format")
            }
            regionForNextIteration = currentRegion
        }
        buildDateFormatRegions(format, ++index, regionForNextIteration, formatRegions)
    }

    /* core method for generating the regex for a given DateFormatRegion */
    private static String getRegexForRegion(DateFormatRegion region) {
        DateFormatRegionType type = region?.type
        Objects.requireNonNull type
        String contents = region.contents
        int regionLength = contents.length()
        int regexLength
        String result
        switch (type) {
            case DateFormatRegionType.MISC: // escape miscellaneous characters
                result = Pattern.quote(contents)
                break
            case DateFormatRegionType.y: // 2 digits if range is y, yy or yyy
                regexLength = regionLength > 3 ?: 2
                result = getRegexForNumericRegion(regexLength)
                break
            case DateFormatRegionType.F:
            case DateFormatRegionType.W:
                result = "[1-5]{$regionLength}?".toString()
                break

            case DateFormatRegionType.D: // 3 digits max
            case DateFormatRegionType.S:
                result = getRegexForVariableWidthNumericRegion(regionLength, 3)
                break

            case DateFormatRegionType.d: // 2 digits max
            case DateFormatRegionType.k:
            case DateFormatRegionType.H:
            case DateFormatRegionType.h:
            case DateFormatRegionType.M: // we don't allow MMM+, can only be M or MM
            case DateFormatRegionType.m:
            case DateFormatRegionType.s:
            case DateFormatRegionType.w:
                result = getRegexForVariableWidthNumericRegion(regionLength, 2)
                break
            default:
                throw new IllegalStateException("Unexpected region type $type for $region")
        }
        result
    }

    /* support methods for producing regular expressions for numeric regions */

    private static String getRegexForVariableWidthNumericRegion(int regionLength, int maxLength) {
        if (regionLength >= maxLength) {
            return getRegexForNumericRegion(regionLength)
        }
        return getRegexForNumericRegion(regionLength, maxLength)
    }

    private static String getRegexForNumericRegion(int width) {
        "\\d{$width}?".toString()
    }

    private static String getRegexForNumericRegion(int minWidth, int maxWidth) {
        "\\d{$minWidth,$maxWidth}?".toString()
    }
}
