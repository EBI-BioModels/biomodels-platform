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
        <meta name="layout" content="${session['branding.style']}/main" />
        <title>Model Information</title>
    </head>
    <body>
        <div class="row">
        <h2>Model Information</h2>
        <p>Please ensure the following fields are correctly filled in.</p>
        <g:form>
        <div class="small-12 medium-12 columns">
            <label for="name" class="required">Name</label>
            <g:if test="${workingMemory['new_name']}">
                <g:textField id="name" name="name"
                             value="${workingMemory['new_name']}"
                             placeholder="Enter a simple sentence summarising title for your model or leave the title of the publication."/>
            </g:if>
            <g:else>
                <g:textField id="name" name="name"
                             value="${(workingMemory.get("RevisionTC") as RevisionTransportCommand).name}"
                             placeholder="Enter a simple sentence summarising title for your model or leave the title of the publication."/>
            </g:else>

            <jummp:displayModelDescriptionLabel>
                <label for="description">${description}</label>
            </jummp:displayModelDescriptionLabel>
            <g:if test="${workingMemory['new_description']}">
                <g:textArea id="description" cols="70" rows="10" name="description"
                            value="${workingMemory['new_description']}"
                            placeholder="Enter a brief description for your model revision, for example: what are the  differences to the previous ones"/>
            </g:if>
            <g:else>
                <g:textArea id="description" cols="70" rows="10" name="description"
                            value='${(workingMemory.get("RevisionTC") as RevisionTransportCommand).description}'
                            placeholder="Enter a brief description for your model revision, for example: what are the  differences to the previous ones"/>
            </g:else>

            <input type='hidden' value='false' name='changed' id="changeStatus"/>
            <div class="buttons">
                <g:submitButton name="Cancel" class="button" value="Abort" />
                <g:submitButton name="Back" class="button" value="Back" />
                <g:submitButton name="Continue" class="button" value="Continue" />
            </div>
        </div>
        </g:form>
        </div>

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

