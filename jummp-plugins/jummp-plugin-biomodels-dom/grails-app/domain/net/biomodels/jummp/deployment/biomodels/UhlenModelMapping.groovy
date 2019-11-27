/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.deployment.biomodels

/**
 * Domain class for mapping a representative model with other models in the akin disease
 *
 * These models were auto-generated in the large submission deposited to BioModels on 11.07.2017. Because of its
 * complexity, we cluster them into smaller groups based on disease name. A cluster should have a representative
 * model which is chosen from the list of models describing the same disease. Without losing generality,
 * the first model in the alphabetical order is nominated.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class UhlenModelMapping extends AutoGenModelMapping implements Serializable {
    private static final long serialVersionUID = 1L

    static mapping = {
        id composite: ['representative', 'member']
        version false
        /**
         * There is an issue in the currently being used version of the database migration plugin which Hibernate
         * Database does not support to generate some types of scripts (e.g. creating index for a given column). This
         * was fixed in version 3.0.0.BUILD-SNAPSHOT as stated in the discussion [1]. We leave mappings below so as
         * use for the future if we upgrade the plugin. Therefore, the migration scripts for creating indexes will be
         * added to manually.
         *
         * [1] https://github.com/grails-plugins/grails-database-migration/issues/74
         */
        representative(indexColumn: [name: "idx_uhlen_rep", type: String])
        member(indexColumn: [name: "idx_uhlen_mem", type: String])
    }

    static constraints = {
        representative(size: 15..32)
        member(size: 15..32)
    }
}



