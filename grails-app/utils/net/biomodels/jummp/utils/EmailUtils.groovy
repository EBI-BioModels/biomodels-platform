/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import java.util.regex.Pattern

/**
 * Utility class for parsing and validating email addresses.
 *
 * <p>Centralises the logic for handling both plain addresses
 * ({@code user@example.com}) and RFC 5322 display-name formatted addresses
 * ({@code Display Name <user@example.com>}), which are commonly produced by
 * application configuration values and mailing-list aliases.</p>
 *
 * <p>All methods are static; this class is not meant to be instantiated.</p>
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
class EmailUtils {

    /**
     * Pattern that matches a minimal valid email address.
     *
     * <p>Requires at least one non-whitespace, non-{@code @} character before
     * the {@code @}, a domain label, a dot, and a TLD. This is intentionally
     * permissive — strict RFC 5321 validation is left to the sending provider
     * (e.g. Brevo).</p>
     */
    static final Pattern VALID_EMAIL = ~/^[^@\s]+@[^@\s]+\.[^@\s]+$/

    /**
     * Pattern that matches a display-name formatted address such as
     * {@code The BioModels Curation Team <curation@biomodels.org>}.
     * Capture group 1 is the bare address inside the angle brackets.
     */
    private static final Pattern NAME_EMAIL = ~/^.+?\s*<([^>]+)>$/

    /**
     * Extracts a bare email address from the supplied string.
     *
     * <p>If {@code addr} is in {@code "Display Name <address>"} format the
     * address inside the angle brackets is returned. Otherwise the trimmed
     * input is returned as-is. A {@code null} or empty input is returned
     * unchanged.</p>
     *
     * @param addr a plain or display-name formatted address, may be {@code null}
     * @return the bare email address, or {@code null}/{@code ""} if the input
     *         was {@code null} or empty
     */
    static String extractEmail(String addr) {
        if (!addr) return addr
        def m = addr =~ NAME_EMAIL
        return m ? (m[0][1] as String).trim() : addr.trim()
    }

    /**
     * Normalises a list of addresses and returns only the valid ones.
     *
     * <p>Each entry is passed through {@link #extractEmail(String)} to strip
     * any display-name prefix. Entries that are blank or do not match
     * {@link #VALID_EMAIL} after extraction are silently discarded.</p>
     *
     * @param addresses a list of plain or display-name formatted addresses;
     *                  may be {@code null} or empty
     * @return a new list containing only the valid bare addresses, never
     *         {@code null}
     */
    static List<String> extractValidEmails(List<String> addresses) {
        if (!addresses) return []
        return addresses.collect { extractEmail(it) }
                        .findAll { it && (it ==~ VALID_EMAIL) }
    }
}
