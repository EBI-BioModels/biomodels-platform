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
* Apache Commons (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.webapp


class ContextHelpTagLib {
	static namespace="ContextHelp"
	def grailsApplication
	
	private String computeLocation(String location) {
		
		String helpRoot=grailsApplication.config.jummp.context.help.root
		if (location=="manual") {
			return helpRoot+"manual.html";
		}
		String defined=grailsApplication.config.jummp.context.help."${location}"
		if (defined) {
			return helpRoot+defined;
		}
		return null;
	}
	
	def getURL = { attrs ->
		if (attrs.location!=null && attrs.location) {
			String url=computeLocation(attrs.location)
			if (url) {
				out<<url
			}
		}
	}
	
	def getLink = { attrs ->
		if (attrs.location!=null) {
			StringBuilder builder=new StringBuilder("<iframe id='helpFrame' src='");
			String url=computeLocation(attrs.location);
			if (url) {
				builder.append(url+"#contextSpecific/"+attrs.width);
				builder.append("'></iframe>");
				out<<builder.toString();
			}
		}
		else {
			out<<"NO LOCATION PROVIDED!"
		}
	}
	
	
}
