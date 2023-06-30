/**
 * Copyright (C) 2010-2023 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 **/

package net.biomodels.jummp.core.model;

import net.biomodels.jummp.model.ModellingApproach;

import java.io.File;
import java.util.List;

/**
 * <p>Service interface for handling a specific ModelFormat.</p>
 * <p>The interface needs to be implemented by a plugin providing support for a Model format
 * like SBML. The core application uses this interface to resolve the service which provides
 * the functionality to handle a specific format.</p>
 *
 * <p>Authors:</p>
 * <ul>
 *   <li>Martin Gräßlin &nbsp;<a href="mailto:m.graesslin@dkfz-heidelberg.de">m.graesslin@dkfz-heidelberg.de</a></li>
 *   <li>Raza Ali&nbsp;<a href="mailto:raza.ali@ebi.ac.uk">raza.ali@ebi.ac.uk</a></li>
 *   <li>Mihai Glonț&nbsp;<a href="mailto:mihai.glont@ebi.ac.uk">mihai.glont@ebi.ac.uk</a></li>
 *   <li>Tung Nguyen&nbsp;<a href="mailto:tung.nguyen@ebi.ac.uk">tung.nguyen@ebi.ac.uk</a></li>
 * </ul>
 */
public interface FileFormatService {

    /**
     * Validate the @p model.
     * @param model File handle containing the Model to be validated.
     * @param errors Is populated with a list of errors, if any
     * @return @c true if the Model is valid, @c false otherwise
     */
    boolean validate(final List<File> model, final List<String> errors);

    /**
     * Extracts the name from the @p model.
     * @param model File handle containing the Model whose name should be extracted.
     * @return The name of the Model, if possible, an empty String if not possible
     */
    String extractName(final List<File> model);

    /**
     * Extracts the description from the @p model.
     * @param model File handle containing the Model whose name should be extracted.
     * @return The description of the Model, if possible, an empty String if not possible
     */
    String extractDescription(final List<File> model);

    /**
     * Attempts to set the model name of @p revision to @p name.
     *
     * @param revision The revision that should be updated.
     * @param name The new name that the model should have.
     * @return true if the operation was successful, false otherwise.
     */
    boolean updateName(RevisionTransportCommand revision, final String name);

    /**
     * Attempts to set the model description of @p revision to @p description.
     *
     * @param revision The revision that should be updated.
     * @param description The new description that the model should have.
     * @return true if the operation was successful, false otherwise.
     */
    boolean updateDescription(RevisionTransportCommand revision, final String description);

    /**
     * Retrieves all annotation URNs in the model file referenced by @p revision.
     * @param revision The Revision identifying a model file
     * @return List of all URNs in the model file.
     */
    List<String> getAllAnnotationURNs(RevisionTransportCommand revision);
    /**
     * Retrieves all pubmed annotations in the model file referenced by @p revision.
     * @param revision  The Revision identifying a model file
     * @return List of all pubmeds used in the Revision
     */
    List<String> getPubMedAnnotation(RevisionTransportCommand revision);

    List<String> getPublicationAnnotations(final File modelFile);

    List<String> getPublicationAnnotations(RevisionTransportCommand revision);

    /*
     * Checks whether the files passed comprise a model of this format
     * @param files The files comprising a potential model of this format
     */
    boolean areFilesThisFormat(final List<File> files);

    /**
     * Retrieves the version of a format in which revision @p revision is encoded.
     * @param revision the Revision of a model
     * @return the textual representation of the format's version - e.g. L3V2 for SBML.
     */
    String getFormatVersion(RevisionTransportCommand revision);

    boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand newRevision);

    /**
     * Gets MAMO terms annotated in the model as the modelling approach.

     * The majority of models deposited in BioModels are being annotated with MAMO terms to
     * denote the modelling approach of the model.
     *
     * @param revision  The Revision instance indicating the given model
     * @return  an ModellingApproach object indicating a specified approach
     */
    ModellingApproach getModellingApproach(final RevisionTransportCommand revision);

    ModellingApproach guessModellingApproach(final File modelFile);
}
