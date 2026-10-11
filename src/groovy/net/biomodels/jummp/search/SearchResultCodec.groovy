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
 **/

package net.biomodels.jummp.search

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.FacetValue

/**
 * @short Converts the models and facets of a search result to and from the JSON stored in Redis.
 *
 * Only the properties that the search result pages read are kept. Decoding throws if the input
 * cannot be understood, so that callers can treat a stale or corrupt cache entry as a cache miss.
 */
class SearchResultCodec {

    static String encodeModels(Collection<ModelTransportCommand> models) {
        def list = models.collect { ModelTransportCommand m ->
            [
                submissionId    : m.submissionId,
                publicationId   : m.publicationId,
                name            : m.name,
                description     : m.description,
                submitter       : m.submitter,
                submitterUsername: m.submitterUsername,
                submissionDate  : m.submissionDate?.time,
                lastModifiedDate: m.lastModifiedDate?.time,
                state           : m.state?.name(),
                formatName      : m.format?.name,
                formatVersion   : m.format?.formatVersion,
                publicationYear : m.publication?.year,
                searchableLink  : m.searchableLink
            ]
        }
        new JsonBuilder(list).toString()
    }

    static List<ModelTransportCommand> decodeModels(String json) {
        List entries = parseList(json)
        entries.collect { Map e ->
            ModelFormatTransportCommand format = null
            if (e.formatName != null || e.formatVersion != null) {
                format = new ModelFormatTransportCommand(name: e.formatName,
                        formatVersion: e.formatVersion)
            }
            PublicationTransportCommand publication = null
            if (e.publicationYear != null) {
                publication = new PublicationTransportCommand(year: e.publicationYear as Integer)
            }
            new ModelTransportCommand(
                submissionId: e.submissionId,
                publicationId: e.publicationId,
                name: e.name,
                description: e.description,
                submitter: e.submitter,
                submitterUsername: e.submitterUsername,
                submissionDate: e.submissionDate != null ? new Date(e.submissionDate as long) : null,
                lastModifiedDate: e.lastModifiedDate != null ?
                        new Date(e.lastModifiedDate as long) : null,
                state: e.state ? ModelState.valueOf(e.state as String) : null,
                format: format,
                publication: publication,
                searchableLink: e.searchableLink
            )
        }
    }

    static String encodeFacets(Collection<Facet> facets) {
        def list = facets.collect { Facet f ->
            [
                id         : f.id,
                label      : f.label,
                total      : f.total,
                facetValues: f.facetValues?.collect { FacetValue fv ->
                    [label: fv.label, value: fv.value, count: fv.count]
                }
            ]
        }
        new JsonBuilder(list).toString()
    }

    static List<Facet> decodeFacets(String json) {
        List entries = parseList(json)
        entries.collect { Map e ->
            Facet facet = new Facet()
            facet.id = e.id
            facet.label = e.label
            facet.total = e.total as Integer
            List values = e.facetValues as List
            facet.facetValues = (values ?: []).collect { Map v ->
                FacetValue fv = new FacetValue()
                fv.label = v.label
                fv.value = v.value
                fv.count = v.count
                fv
            } as FacetValue[]
            facet
        }
    }

    private static List parseList(String json) {
        def parsed = new JsonSlurper().parseText(json)
        if (!(parsed instanceof List)) {
            throw new IllegalArgumentException("Expected a JSON array but got ${parsed?.getClass()}")
        }
        (List) parsed
    }
}
