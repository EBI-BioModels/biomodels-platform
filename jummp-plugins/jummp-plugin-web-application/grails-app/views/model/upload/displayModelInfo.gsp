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
<%@ page import="net.biomodels.jummp.core.model.ModelFormatTransportCommand" %>
<%@ page import="net.biomodels.jummp.core.model.RevisionTransportCommand" %>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="${session['branding.style']}/main" />
        <title>Model Information - Submission | BioModels</title>
    </head>
    <body>
        <h2><g:message code="submission.biomodels.model.information.heading" locale="${Locale.getDefault()}"/></h2>
        <p><g:message code="submission.biomodels.model.information.explanation" locale="${Locale.getDefault()}"/></p>
        <g:form>
        <div class="small-12 medium-12 columns">
            <div class="row">
                <div class="small-12 medium-6 large-6 columns">
                    <label for="model_format" class="required">Model Format</label>
                    <g:if test="${workingMemory['model_type']}">
                    </g:if>
                    <%
                        String fmtStr
                        Integer selectedValue
                        ModelFormatTransportCommand format = workingMemory['model_type']
                        if (format) {
                            String fmtVersion = format.formatVersion!="*" ? format.formatVersion : ""
                            fmtStr = "${format.name} ${fmtVersion}"
                            selectedValue = format.id
                        } else {
                            fmtStr = 'Original code'
                            selectedValue = ModelFormat.findByName("UNKNOWN")?.id
                        }
                    %>
                    <g:select name="model_format" id="model_format" required=""
                              from="${net.biomodels.jummp.model.ModelFormat.list()}"
                              value="${selectedValue}"
                              optionKey="id"
                              optionValue="${{it?.name + ' ' + it?.formatVersion}}"
                               />
                    <g:if test="${fmtStr.trim() == 'Original code'}">
                    <label for="readme_submission" class="required">Describe more exactly your model format</label>
                    <g:textField name="readme_submission" id="readme_submission" required=""
                                 placeholder="Please describe here more accurately what is your model format" /></g:if>
                </div>
                <div class="small-12 medium-6 large-6 columns">
                    <label for="modelling_approach" class="required">Modelling Approach</label>
                    <g:textField name="modelling_approach" id="modelling_approach" required=""
                                 placeholder="Enter your modelling approach"/>


                </div>
            </div>

            <label for="name" class="required">Name</label>
            <g:if test="${workingMemory['new_name']}">
                <g:textField id="name" name="name" required=""
                             value="${workingMemory['new_name']}"
                             placeholder="Enter a simple sentence summarising title for your model or leave the title of the publication."/>
            </g:if>
            <g:else>
                <g:textField id="name" name="name" required=""
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

        <g:javascript>
            function associateEventHandlers(id) {
                let descBox = document.getElementById(id);

                if ("onpropertychange" in descBox) {
                    descBox.attachEvent("onpropertychange", $.proxy(function () {
                        if (event.propertyName == "value")
                            $("#changeStatus").val(true);
                        }, descBox));
                } else {
                    descBox.addEventListener("input", function () {
                        $("#changeStatus").val(true);
                    });
                }
            }
            $( document ).ready(function() {
                associateEventHandlers("description");
                associateEventHandlers("name");
            });

            $('#modelling_approach').on('keydown', function() {
                $(this).autocomplete({
                    source: function(request, response) {
                        $.ajax({
                            url: $.jummp.createLink('model', 'searchModellingApproach'),
                            type: 'POST',
                            dataType: 'json',
                            data: {
                                search: request.term,
                                request: 1
                            },
                            success: function(data) {
                                response(data);
                            }
                        });
                    },
                    select: function(event, ui) {
                        let label = ui.item.label;
                        $(this).val(label);   // display the selected text
                        let id = ui.item.id; // selected value
                        let accession = label.substring(0,label.indexOf(":"));
                        $.ajax({
                            url: $.jummp.createLink('model', 'searchModellingApproach'),
                            type: 'POST',
                            data: {
                                id: id,
                                accession: accession,
                                request: 2
                            },
                            dataType: 'json',
                            success: function(response) {
                                let len = response.length;
                                if(len > 0){
                                    let id = response[0]['id'];
                                    let accession = response[0]['accession'];
                                    let name = response[0]['name'];
                                    let resource = response[0]['resource'];
                                }
                            }
                        });
                        return false;
                    }
                });
            });
        </g:javascript>

    </body>
    <g:render template="/templates/decorateSubmission" />
    <g:render template="/templates/subFlowContextHelp" />

