<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License along 
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>











<g:if test="${model.publication}">
	<div>
		<style>
			.hiddenContent {display:none;}
		</style>
    	<ul style="list-style-type: none;margin: 0px;padding:0px;">
		<li><a title="View publication - this will open an external site" class="publicationLink" href="${model.publication.linkProvider.identifiersPrefix?model.publication.linkProvider.identifiersPrefix+model.publication.link:model.publication.link}">
			<b>${model.publication.title}</b></a>
		<a class="expander" title="Click to see more" href="#">
			<span>
				<img style="width:12px;margin:2px;float:none" src="${grailsApplication.config.grails.serverURL}/images/expand.png"/>
			</span>
		</a>
		
			</li>		
		<li>${model.publication.authors.collect{"${it.userRealName}"}.join(", ")}</li>
    	<li><i>${model.publication.journal}</i>${model.publication.month?", ${model.publication.month}/":""}${model.publication.year?"${model.publication.year}":""}${model.publication.volume?", Volume ${model.publication.volume}":""}${model.publication.issue?", Issue ${model.publication.issue}":""}${model.publication.pages?", pages: ${model.publication.pages}":""}</li>
 		<div class="hiddenContent">
 			<g:if test="${model.publication.affiliation}">
 				<li><label>Affiliation: </label> ${model.publication.affiliation}</li>
 			</g:if>
 			<g:if test="${model.publication.synopsis}">
    			<li><label>Abstract: </label>${model.publication.synopsis}</li>
    		</g:if>
    	</div>
    	</ul>
    	<g:javascript contextPath="" src="simple-expand.js"/>
    	<script>
			$('.expander').simpleexpand({'defaultTarget':'div.hiddenContent'});
		</script>
		</div>
</g:if>
<g:else>
	Not provided
</g:else>
