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











<%@ page contentType="text/html;charset=UTF-8" %>
    <head>
        <meta name="layout" content="main"/>
        <g:javascript src="underscore-min.js"/>
        <g:javascript src="handlebars.min.js"/>
        <g:javascript src="backbone-min.js"/>
        <script id="collaborator-list-template" type="text/x-handlebars-template">

        		<div id="currentCollabs">
				<h2>Collaborators</h2>
    			{{#if hasCollabs}}
				<table class='table'>
    			<thead>
    				<tr>
    					<td class="tableEL bold">Name</td>
    					<td class="tableEL  bold">Read</td>
    					<td class="tableEL bold">Write</td>
    			<tbody>
    		 		{{#each collabsList}}
    		 			{{#if this.show}}
							<tr class='collaborator'>
								<td class="tableEL">{{this.name}}</td>
								<td class="tableEL"><input id=checkRead-{{this.id}} data-field="read" data-person={{this.id}} class="updateCollab" type="radio" name="accesstype-{{this.id}}" {{setChecked "read"}} {{#if this.disabledEdit}}disabled=true title="This user cannot be modified"{{/if}}></td>
								<td class="tableEL"><input id=checkWrite-{{this.id}} data-field="write" data-person={{this.id}} class="updateCollab" type="radio" name="accesstype-{{this.id}}" {{setChecked "write"}} {{#if this.disabledEdit}}disabled=true title="This user cannot be modified"{{/if}}></td>
								<td class="tableEL"><button id=removebutton-{{this.id}} data-name={{this.name}} data-person={{this.id}} {{#if this.disabledEdit}}disabled=true title="This user cannot be modified"{{/if}} class="remove">Remove</button></td>
							</tr>
						{{/if}}
    		 		{{/each}}
    		 	</tbody>
    		 	</table>
    			{{else}}
    		 		This model is not shared with anyone.
    		 	{{/if}}
    		 	</div>
    		 </script>
    		 <link rel="stylesheet" href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: '/css', file: 'share.css')}" />

    </head>
    <body>
    		<div id="ui">
    			<div id="collabUI">
    				<div id="collabCreate">
    					<h2>Add New Collaborator</h2>
    					<form id="collaboratorAddForm">
    						<div id="formElements" class="formElements">
    							<div class="formElement">
    								<label for="nameSearch">User</label>
    								<input placeholder="Name, username or email" id="nameSearch" name="name" type="text">
    							</div>
    							<div class="formElement">
									<label for="radioReader">Read</label>
									<input id="radioReader" type="radio" name="read"
                                           checked>
								</div>
								<div class="formElement">
									<label for="radioWriter">Write</label>
									<input id="radioWriter" type="radio"
                                           name="write">
								</div>
                                <div class="formElement">
                                    <button id="AddButton">Add</button>
                                </div>
							</div>

						<g:if test="${teams.size() > 0}">
							<div id="teamFormElements" class="formElements">
									<div class="formElement">
										<label for="teamSearch">Team</label>
										<select id="teamSearch" name="team">
											<g:each in="${teams}">
												<option value="${it.id}">${it.name} (Created by ${it.owner.person.userRealName})</option>
											</g:each>
										</select>
									</div>
									<div class="formElement">
										<label for="teamRadioReader">Read</label>
										<input id="teamRadioReader" type="radio" name="teamRead" checked>
									</div>
									<div class="formElement">
										<label for="teamRadioWriter">Write</label>
										<input id="teamRadioWriter" type="radio"
											   name="teamWrite">
									</div>
                                <div class="formElement">
                                    <button id="TeamAddButton">Add</button>
                                </div>
                            </div>

						</g:if>
					   </form>
					</div>
					<div class="containUI">
					</div>
					</div>
					<hr style="visibility:hidden;"/>
					<a href="${createLink(action:"show", id:revision.identifier())}">Back to
                    Model</a>
    		   </div>
			 <g:javascript contextPath="" src="share.js"/>
    		<g:javascript>
    			$( document ).ready(function() {
    					main(JSON.parse('${permissions}'),
    						'<g:createLink controller="jummp" action="lookupUser"/>',
    						'<g:createLink controller="model" action="shareUpdate" id="${revision.identifier()}"/>',
                            '<g:createLink controller="jummp" action="autoCompleteUser"/>',
                            '<g:createLink controller="jummp" action="teamLookup"/>')
    			});
    		</g:javascript>
    </body>
    <content tag="title">
		Sharing: ${revision.model.name}
	</content>
	<content tag="contexthelp">
		sharing
	</content>

