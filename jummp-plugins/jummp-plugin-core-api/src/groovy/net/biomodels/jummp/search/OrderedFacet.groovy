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





package net.biomodels.jummp.search

import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.FacetValue

/**
 * This class wraps a facet associating with the order. It is implemented Comparable interface
 * which has compareTo(T obj) method that is used by sorting methods
 *
 * Our purpose is to sort searchable facets based on specific benchmarks which are dissimilar to
 * OmicsDI's specification
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @created on 23/05/17.
 */
class OrderedFacet implements Comparable<OrderedFacet>, Serializable {
    private static final long serialVersionUID = 1L

    /**
     * Facet comes from the ddi-ebe-ws-dao library and is not Serializable, so it is written by hand
     * in writeObject/readObject. This lets the search results cache overflow to disk.
     */
    transient Facet facet
    int order

    OrderedFacet(Facet facet, int order) {
        this.facet = facet
        this.order = order
    }
    public int compareTo(OrderedFacet orderedFacet) {
        this.order - orderedFacet.order
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject()
        out.writeBoolean(facet != null)
        if (facet == null) {
            return
        }
        out.writeObject(facet.id)
        out.writeObject(facet.label)
        out.writeObject(facet.total)
        FacetValue[] values = facet.facetValues
        out.writeInt(values == null ? -1 : values.length)
        values?.each { FacetValue v ->
            out.writeBoolean(v != null)
            if (v != null) {
                out.writeObject(v.label)
                out.writeObject(v.value)
                out.writeObject(v.count)
            }
        }
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject()
        if (!input.readBoolean()) {
            return
        }
        Facet f = new Facet()
        f.id = input.readObject() as String
        f.label = input.readObject() as String
        f.total = input.readObject() as Integer
        int n = input.readInt()
        if (n >= 0) {
            FacetValue[] values = new FacetValue[n]
            for (int i = 0; i < n; i++) {
                if (input.readBoolean()) {
                    FacetValue v = new FacetValue()
                    v.label = input.readObject() as String
                    v.value = input.readObject() as String
                    v.count = input.readObject() as String
                    values[i] = v
                }
            }
            f.facetValues = values
        }
        this.facet = f
    }
}
