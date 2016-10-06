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





package net.biomodels.jummp.webapp

import net.biomodels.jummp.core.annotation.QualifierTransportCommand
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand

class DDMoReResourceReferenceTagLib {
    static namespace = "ddmore"

    def metadataDelegateService

    /**
     * Simple mechanism for deciding how to render resource references depending
     * on whether they point to a URI that exists or not.
     *
     * MetadataDelegateService contains a "black list" of namespaces which is used to test this.
     *
     * @attr reference REQUIRED the resource reference to render.
     */
    def filterHyperlink = { attrs, body ->
        ResourceReferenceTransportCommand xref = attrs.reference
        if (!xref) {
            return
        }
        final String uri = xref.uri
        def ignoredNamespaces = metadataDelegateService.getMetadataNamespaces()
        boolean hasBlacklistedNs = null != ignoredNamespaces.find { String ns ->
            uri?.startsWith(ns)
        }
        if (hasBlacklistedNs) {
            xref.uri = null
        }
        out << body(xref: xref)
    }

    def coalesceQualifiers = { attrs, body ->
        def annotations = attrs.annotations
        if (!annotations) {
            out << body("mergedAnnotations": [:])
        }
        TreeMap<String, List<ResourceReferenceTransportCommand>> result = new TreeMap<>()
        annotations.each { QualifierTransportCommand q,
                           List<ResourceReferenceTransportCommand> xrefs ->
            final String accession = q.accession
            if (result.containsKey(accession)) {
                result.get(accession).addAll(xrefs)
            } else {
                result.put(accession, xrefs)
            }
        }
        out << body("mergedAnnotations": result)
    }
}
