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

import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled

/**
 * Possibly the most permissive implementation possible of the file format service
 * interface. Everything is a valid unknown format.
 * @author raza
 */
class UnknownFormatService implements FileFormatService {
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Validate the @p model.
     * @param model File handle containing the Model to be validated.
     * @return @c true if the Model is valid, @c false otherwise
     */
    public final boolean validate(final List<File> model, List<String> errors) {
        return areFilesThisFormat(model)
    }

    /**
     * Extracts the name from the @p model.
     * @param model File handle containing the Model whose name should be extracted.
     * @return The name of the Model, if possible, an empty String if not possible
     */
    public final String extractName(final List<File> model) {
        return ""
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateName(RevisionTransportCommand revision, final String name) {
        if (name.trim() && revision) {
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
    public final String getFormatVersion(RevisionTransportCommand revision) {
        return "*"
    }

    /**
     * Extracts the description from the @p model.
     */
    public final String extractDescription(final List<File> model) {
        return ""
    }

    boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand rev) {
        return true
    }

    @Profiled(tag="unknownFormatService.getModelOntologyTerm")
    String getModelOntologyTerm(RevisionTransportCommand revisionTC) {
        // TODO: replace it by a correct url. Here we keep it similar to PharmML's one
        return "http://www.pharmml.org/ontology/PHARMMLO_0000001"
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateDescription(RevisionTransportCommand revision, final String DESC) {
        if (revision && DESC.trim()) {
            revision.description = DESC.trim()
            return true
        }
        return false
    }

    /**
     * Retrieves all annotation URNs in the model file referenced by @p revision.
     * @param revision The Revision identifying a model file
     * @return List of all URNs in the model file.
     */
    public final List<String> getAllAnnotationURNs(RevisionTransportCommand revision) {
        return new LinkedList<String>()
    }
    /**
     * Retrieves all pubmed annotations in the model file referenced by @p revision.
     * @param revision  The Revision identifying a model file
     * @return List of all pubmeds used in the Revision
     */
    public final List<String> getPubMedAnnotation(RevisionTransportCommand revision) {
        return new LinkedList<String>()
    }
    /*
     * Checks whether the files passed comprise a model of this format
     * @param files The files comprising a potential model of this format
     */
    public final boolean areFilesThisFormat(final List<File> files) {
        if (files && !files.isEmpty()) {
            return true
        }
        return false
    }


    List<ElementAnnotationTransportCommand> fetchGenericAnnotations(RevisionTransportCommand rev) {
        // There can only be model-level annotations for this model format.
        rev.annotations
    }
}

