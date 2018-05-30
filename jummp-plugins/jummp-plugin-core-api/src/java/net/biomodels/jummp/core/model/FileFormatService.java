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
*
**/





package net.biomodels.jummp.core.model;

import java.io.File;
import java.util.List;

/**
 * Service interface for handling a specific ModelFormat.
 * The interface needs to be implemented by a plugin providing support for a Model format
 * like SBML. The core application uses this interface to resolve the service which provides
 * the functionality to handle a specific format.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
public interface FileFormatService {

    /**
     * Validate the @p model.
     * @param model File handle containing the Model to be validated.
     * @param errors Is populated with a list of errors, if any
     * @return @c true if the Model is valid, @c false otherwise
     */
    public boolean validate(final List<File> model, final List<String> errors);

    /**
     * Extracts the name from the @p model.
     * @param model File handle containing the Model whose name should be extracted.
     * @return The name of the Model, if possible, an empty String if not possible
     */
    public String extractName(final List<File> model);

    /**
     * Extracts the description from the @p model.
     * @param model File handle containing the Model whose name should be extracted.
     * @return The description of the Model, if possible, an empty String if not possible
     */
    public String extractDescription(final List<File> model);

    /**
     * Attempts to set the model name of @p revision to @p name.
     *
     * @param revision The revision that should be updated.
     * @param name The new name that the model should have.
     * @return true if the operation was successful, false otherwise.
     */
    public boolean updateName(RevisionTransportCommand revision, final String name);

    /**
     * Attempts to set the model description of @p revision to @p description.
     *
     * @param revision The revision that should be updated.
     * @param description The new description that the model should have.
     * @return true if the operation was successful, false otherwise.
     */
    public boolean updateDescription(RevisionTransportCommand revision, final String description);

    /**
     * Retrieves all annotation URNs in the model file referenced by @p revision.
     * @param revision The Revision identifying a model file
     * @return List of all URNs in the model file.
     */
    public List<String> getAllAnnotationURNs(RevisionTransportCommand revision);
    /**
     * Retrieves all pubmed annotations in the model file referenced by @p revision.
     * @param revision  The Revision identifying a model file
     * @return List of all pubmeds used in the Revision
     */
    public List<String> getPubMedAnnotation(RevisionTransportCommand revision);
    /*
     * Checks whether the files passed comprise a model of this format
     * @param files The files comprising a potential model of this format
     */
    public boolean areFilesThisFormat(final List<File> files);

    /**
     * Retrieves the version of a format in which revision @p revision is encoded.
     * @param revision the Revision of a model
     * @return the textual representation of the format's version - e.g. L3V2 for SBML.
     */
    public String getFormatVersion(RevisionTransportCommand revision);

    public boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand newRevision);
}
