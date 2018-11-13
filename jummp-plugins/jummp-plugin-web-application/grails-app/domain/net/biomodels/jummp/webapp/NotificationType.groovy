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





package net.biomodels.jummp.webapp

/**
 * @short Representation of one NotificationType.
 * This enum represents the types of notifications available
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tnguyen@ebi.ac.uk>
 * @date updated on 20/09/2017
 */
enum NotificationType {
   PUBLISH (1, "Publish"),
   VERSION_CREATED (2, "Version Created"),
   ACCESS_GRANTED (3, "Another user is granted access to a model"),
   DELETED (4, "Model Deleted"),
   ACCESS_GRANTED_TO (5, "Access is granted to you"),
   FEEDBACK_ARRIVED (6, "A feedback has been sent to you"),
   SUBMIT_FOR_PUBLICATION (7, "A model has been submitted to you for publication")

   final int id
   final String textRepresentation

   private NotificationType(int id, String textRepresentation) {
      this.id = id
      this.textRepresentation = textRepresentation
   }

   static NotificationType getById(int id) {
   	   switch(id) {
   	   	   case 1: return PUBLISH
   	   	   case 2: return VERSION_CREATED
   	   	   case 3: return ACCESS_GRANTED
   	   	   case 4: return DELETED
           case 5: return ACCESS_GRANTED_TO
           case 6: return FEEDBACK_ARRIVED
           case 7: return SUBMIT_FOR_PUBLICATION
   	   }
   }

   String toString() { textRepresentation }
}
