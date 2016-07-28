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
<%@ page import="net.biomodels.jummp.core.model.RevisionTransportCommand" %>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="main"/>
        <title>Model Information</title>
    </head>
    <body>
        <h2>Model Information</h2>
        <p>Please ensure the following fields are correctly filled in.</p>

        <g:form>
            <div class="dialog">
                <table class="formtable">
                    <tbody>
                        <tr class="prop">
                            <td class="name">
                                <label for="name">Name:</label>
                            </td>
                            <td class="value">
                                <g:if test="${workingMemory['new_name']}">
                                    <g:textField id="name" name="name" style="width: 713px" value="${workingMemory['new_name']}"/>
                                </g:if>
                                <g:else>
                                    <g:textField id="name" name="name" style="width: 713px" value="${(workingMemory.get("RevisionTC") as RevisionTransportCommand).name}"/>
                                </g:else>
                            </td>
                        </tr>
                        <tr class="prop">
                            <td class="name">
                                <jummp:displayModelDescriptionLabel>
                                    <label for="description">${description}:</label>
                                </jummp:displayModelDescriptionLabel>
                            </td>
                            <td class="value">
                                <g:if test="${workingMemory['new_description']}">
                                    <g:textField id="description" name="description" style="width: 700px" value="${workingMemory['new_description']}"/>
                                </g:if>
                                <g:else>
                                    <g:textArea id="description" cols="70" rows="10" style="width: 700px" name="description" value='${(workingMemory.get("RevisionTC") as RevisionTransportCommand).description}'/>
                                </g:else>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <input type='hidden' value='false' name='changed' id="changeStatus"/>
                <div class="buttons">
                    <g:submitButton name="Cancel" value="Abort" />
                    <g:submitButton name="Back" value="Back" />
                    <g:submitButton name="Continue" value="Continue" />
                </div>
            </div>
        </g:form>
        <script>
        
        function associateEventHandlers(id) {
        	var descBox = document.getElementById(id);
        	
        	
        	if ("onpropertychange" in descBox)
        	{
        		descBox.attachEvent("onpropertychange", $.proxy(function () {
        			if (event.propertyName == "value")
        				$("#changeStatus").val(true);
        			}, descBox));
        	}
        	else
        	{
        		descBox.addEventListener("input", function () { 
        			$("#changeStatus").val(true);
        		});
        	}
    	}
    	$( document ).ready(function() {
    		associateEventHandlers("description");
    		associateEventHandlers("name");
    	});
    	</script>
    
     </body>
    <g:render template="/templates/decorateSubmission" />
    <g:render template="/templates/subFlowContextHelp" />

