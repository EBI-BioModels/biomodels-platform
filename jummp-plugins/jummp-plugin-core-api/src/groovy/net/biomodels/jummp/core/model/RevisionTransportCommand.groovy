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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Grails (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Grails used as well as
 * that of the covered work.}
 **/





package net.biomodels.jummp.core.model

import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand
import net.biomodels.jummp.core.certification.QcInfoTransportCommand
import org.springframework.context.ApplicationContext

/**
 * @short Wrapper for a Revision to be transported through JMS.
 *
 * Small wrapper class to decouple the Revision from the Database.
 * Changes to instances of this class are not populated to the database.
 *
 * The object can also be used as a command object for the web interface.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class RevisionTransportCommand implements Serializable {
    private static final long serialVersionUID = 1L
    /**
     * The application context. Populated during bootstrap.
     */
    static ApplicationContext context
    Long id
    ModelState state
    /**
     * Revision number in reference to the Model and not to the VCS.
     */
    Integer revisionNumber
    /**
     * The real name of the user who uploaded the Revision.
     */
    String owner
    /**
     * Whether the revision is a minor change or not.
     */
    Boolean minorRevision
    /**
     * Whether the revision has been validated
     */
    Boolean validated
    /**
     * The name of this revision.
     */
    String name
    /**
     * The description of this revision.
     */
    String description
    /**
     * The "commit message" of this revision.
     */
    String comment
    /**
     * The date when the Revision was uploaded.
     */
    Date uploadDate
    /**
     * The format of the file in the VCS.
     */
    ModelFormatTransportCommand format
    /**
     * The model the revision belongs to
     */
    ModelTransportCommand model
    /**
     * The list of files associated with this revision
     */
     List<RepositoryFileTransportCommand> files = null;
    /**
     * The list of annotations for this revision.
     */
    List<ElementAnnotationTransportCommand> annotations

    ValidationState validationLevel

    String validationReport

    QcInfoTransportCommand qcInfo

     List<RepositoryFileTransportCommand> getFiles() {
         if (!files) {
             files = context.modelDelegateService.retrieveModelFiles(this)
         }
         return files
     }

     String identifier() {
         final PERENNIAL_ID = model.publicationId ?: model.submissionId
         return new StringBuffer(PERENNIAL_ID).append(".").append(revisionNumber).toString()
     }

/*    String [] getValidationStatementList(){
        if(validationReport == null){
            return null;
        }
        return validationReport.split("     ");
    }*/

    String getCertificationMessage(){
        if(qcInfo){
            return qcInfo.comment
        }else
            return "This model is not certified."
    }

    String getValidationLevelMessage(){
        getValidationLevelMessage(validationLevel)
    }

    String getValidationLevelMessage(ValidationState validationLevel){
        switch (validationLevel) {
            case ValidationState.APPROVE:
                return "Annotations have not been checked."
            case ValidationState.CONDITIONALLY_APPROVED:
                return "Annotations have been checked. There are some errors."
            case ValidationState.APPROVED:
                return "Annotations are correct."
            case ValidationState.REJECTED:
                return "Annotations are incorrect"
        }
    }
}
