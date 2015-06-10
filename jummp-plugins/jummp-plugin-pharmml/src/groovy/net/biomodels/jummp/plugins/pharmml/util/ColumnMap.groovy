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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Apache Tika, Apache Commons, LibPharmml, Perf4j (or a modified version of these
 * libraries), containing parts covered by the terms of Apache License v2.0,
 * the licensors of this Program grant you additional permission to convey the
 * resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Tika, Apache Commons,
 * LibPharmml, Perf4j used as well as that of the covered work.}
 **/

package net.biomodels.jummp.plugins.pharmml.util

import eu.ddmore.libpharmml.dom.dataset.ColumnMapping
import org.perf4j.aop.Profiled

class ColumnMap {
    private final LinkedHashMap<String, ColumnMapValue> columns

    protected ColumnMap() {}

    public ColumnMap(List<ColumnMapping> mapping) {
        final int SIZE = mapping ? mapping.size() : 0
        columns = new LinkedHashMap<>(SIZE + 1, 1.0f)
        if (mapping) {
            for (ColumnMapping m: mapping) {
                String key = m.columnRef?.columnIdRef
                if (!key) {
                    throw new IllegalArgumentException("Column references cannot be null.")
                }
                ColumnMapValue value = new ColumnMapValue(m.symbRef, m.categoryMapping)
                columns.put(key, value)
            }
        }
    }

    @Profiled(tag = "columnMap.get")
    ColumnMapValue get(String columnId) {
        if (!columnId || !columnId?.trim()) {
            throw new IllegalArgumentException("Column identifier cannot be null")
        }
        return columns[columnId]
    }

    @Override
    @Profiled(tag = "columnMap.toString")
    String toString() {
        return "ColumnMap(${columns.toString()})"
    }
}
