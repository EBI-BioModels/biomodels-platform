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


package net.biomodels.jummp.core.model

import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand as EATC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC
import org.perf4j.aop.Profiled
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Possibly the most permissive implementation possible of the file format service
 * interface. Everything is a valid unknown format.
 * @author raza
 */
class UnknownFormatService extends FileFormatServiceAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(this)
    /**
     * Validate the @p model.
     * @param model File handle containing the Model to be validated.
     * @return @c true if the Model is valid, @c false otherwise
     */
    final boolean validate(final List<File> model, List<String> errors) {
        return areFilesThisFormat(model)
    }

    /**
     * Extracts the name from the @p model.
     * @param model File handle containing the Model whose name should be extracted.
     * @return The name of the Model, if possible, an empty String if not possible
     */
    final String extractName(final List<File> model) {
        return ""
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean updateName(RevisionTC revision, final String name) {
        if (name?.trim() && revision) {
            revision.name = name
            return true
        }
        return false
    }

    /**
     * Implementation of {@link net.biomodels.jummp.core.model.FileFormatService}
     * Does not make any attempt extract the version of an unknown model as it is undefined.
     * @param revision a revision of a model in a format that has not been recognised.
     * @return an empty String.
     */
    final String getFormatVersion(RevisionTC revision) {
        return "*"
    }

    /**
     * Extracts the description from the @p model.
     */
    final String extractDescription(final List<File> model) {
        return ""
    }

    boolean doBeforeSavingAnnotations(File annoFile, RevisionTC rev) {
        return true
    }

    @Profiled(tag = "unknownFormatService.getModelOntologyTerm")
    String getModelOntologyTerm(RevisionTC revisionTC) {
        // TODO: replace it by a correct url. Here we keep it similar to PharmML's one
        if (revisionTC.format.identifier == "PharmML") {
            return "http://www.pharmml.org/ontology/PHARMMLO_0000001"
        }
        return ""
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean updateDescription(final RevisionTC revision, final String DESC) {
        if (revision && DESC?.trim()) {
            revision.description = DESC.trim()
            return true
        }
        return false
    }

    /*
     * Checks whether the files passed comprise a model of this format
     * @param files The files comprising a potential model of this format
     */

    final boolean areFilesThisFormat(final List<File> files) {
        if (files && !files.isEmpty()) {
            return true
        }
        return false
    }


    List<EATC> fetchGenericAnnotations(RevisionTC rev) {
        // There can only be model-level annotations for this model format.
        rev.annotations
    }
}

