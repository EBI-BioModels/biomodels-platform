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


package net.biomodels.jummp.core.model.audit

enum AccessType {
   CREATE (1, "Create"),
   UPDATE (2, "Update"),
   READ (3, "Read"),
   PUBLISH (4, "Publish"),
   ARCHIVE (5, "Archive"),
   DOWNLOAD (6, "Download"),
   SHARE (7, "Share"),
   FILES (8, "Files"),
   VALIDATE(9, "Validate");


   final int id
   final String name

   private AccessType(int id, String name) {
      this.id = id
      this.name = name
   }

   static AccessType fromAction(String action) {
   	   action=action.toLowerCase();
   	   switch(action) {
   	   	   case "create": return CREATE;
   	   	   case "update": return UPDATE;
   	   	   case "show": return READ;
   	   	   case "publish": return PUBLISH;
   	   	   case "delete": return ARCHIVE;
   	   	   case "download": return DOWNLOAD;
   	   	   case "shareupdate": return SHARE;
   	   	   case "files": return FILES;
           case "validate": return VALIDATE;
   	   }
   	   return null;
   }


   String toString() { name }
}
