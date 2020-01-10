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





package net.biomodels.jummp.core;

import java.nio.file.Path;

/**
 * Interface for a service handling the structure of the folder where all models are stored.
 *
 * VcsManager implementations should rely on it
 *
 * @author  Mihai Glonț <mglont@ebi.ac.uk>
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
public interface IFileSystemService {
    //public createModelFolderStructure(File parent, long id);
    /**
     * Returns the path of the subfolder where models are currently created.
     *
     * Most filesystems struggle to handle more than a few thousand entries in a single folder.
     * Therefore, in the interest of scalability, we should divide the main folder for storing
     * models into sub-directories called containers.
     *
     * Implementations of this service are free to choose the naming convention for containers,
     * as well as the maximum number of entries that should be available in
     *
     * Given the following folder structure for the models
     *      /
     *          container1
     *              model1, model2, model3 ...
     *          container2
     *              model1001, model1002, model1003 ...
     *          container3
     *              model2001
     * this method would return the absolute path to container3
     */
    String findCurrentModelContainer();

    /**
     * Deletes a directory recursively
     * This method will delete the directory in the location indicated by the argument path
     *
     * @param path  A Path object indicating the place where the directory is
     */
    void deleteDirectory(Path path);
}
