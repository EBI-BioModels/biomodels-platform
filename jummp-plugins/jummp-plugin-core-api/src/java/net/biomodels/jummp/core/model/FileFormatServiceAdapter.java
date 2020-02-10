/*
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

package net.biomodels.jummp.core.model;

import net.biomodels.jummp.model.ModellingApproach;

import java.io.File;
import java.util.List;

public class FileFormatServiceAdapter implements FileFormatService {
    @Override
    public boolean validate(List<File> model, List<String> errors) {
        return false;
    }

    @Override
    public String extractName(List<File> model) {
        return null;
    }

    @Override
    public String extractDescription(List<File> model) {
        return null;
    }

    @Override
    public boolean updateName(RevisionTransportCommand revision, String name) {
        return false;
    }

    @Override
    public boolean updateDescription(RevisionTransportCommand revision, String description) {
        return false;
    }

    @Override
    public List<String> getAllAnnotationURNs(RevisionTransportCommand revision) {
        return null;
    }

    @Override
    public List<String> getPubMedAnnotation(RevisionTransportCommand revision) {
        return null;
    }

    @Override
    public boolean areFilesThisFormat(List<File> files) {
        return false;
    }

    @Override
    public String getFormatVersion(RevisionTransportCommand revision) {
        return null;
    }

    @Override
    public boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand newRevision) {
        return false;
    }

    @Override
    public ModellingApproach getModellingApproach(RevisionTransportCommand revision) {
        return null;
    }
}
