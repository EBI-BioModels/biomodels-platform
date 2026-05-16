/**
 * Copyright (C) 2010-2026 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
package net.biomodels.jummp.utils

import org.springframework.validation.Errors
import org.springframework.validation.FieldError

/**
 * @short Formats validation errors from Grails command objects into human-readable strings.
 *
 * Spring's FieldError.defaultMessage contains raw i18n templates with positional
 * placeholders ({0} = field name, {1} = class name). This class resolves those
 * placeholders so that log output and error responses are immediately readable
 * without requiring a running MessageSource.
 *
 * Typical usage in a controller action:
 * <pre>
 *     if (cmd.hasErrors()) {
 *         LOGGER.error("MyCommand validation failed — {}",
 *             CommandErrorFormatter.summarise(cmd.errors))
 *     }
 * </pre>
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
class CommandErrorFormatter {

    /**
     * Returns a single-line summary of all field errors in {@code errors}.
     * Each error is formatted as:
     * <pre>fieldName: resolved message (rejected: 'value')</pre>
     * Multiple errors are separated by {@code "; "}.
     *
     * @param errors the Spring {@link Errors} object from a validated command
     * @return human-readable error summary, or an empty string if there are no field errors
     */
    static String summarise(Errors errors) {
        if (!errors?.hasFieldErrors()) {
            return ""
        }
        errors.fieldErrors.collect { FieldError err ->
            "${err.field}: ${resolveMessage(err)} (rejected: '${err.rejectedValue}')"
        }.join("; ")
    }

    /**
     * Resolves positional placeholders in {@code err.defaultMessage}:
     * <ul>
     *   <li>{0} — field name</li>
     *   <li>{1} — simple class name of the owning object</li>
     *   <li>{2}…{n} — remaining arguments as-is (e.g. rejected value in custom validator messages)</li>
     * </ul>
     */
    private static String resolveMessage(FieldError err) {
        String msg = err.defaultMessage ?: ""
        if (err.arguments) {
            err.arguments.eachWithIndex { arg, i ->
                String replacement = (i == 1 && arg instanceof Class)
                        ? arg.simpleName
                        : arg?.toString() ?: ""
                msg = msg.replace("{$i}", replacement)
            }
        }
        msg
    }
}
