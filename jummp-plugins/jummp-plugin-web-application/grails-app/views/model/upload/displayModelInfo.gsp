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
<%@ page import="net.biomodels.jummp.model.ModelFormat" %>
<%@ page import="net.biomodels.jummp.core.model.ModelFormatTransportCommand" %>
<%@ page import="net.biomodels.jummp.core.model.RevisionTransportCommand" %>
<%
    List modelFormatsSortedByName = workingMemory['sorted_model_formats']
    ModelFormatTransportCommand format = workingMemory['model_type']
    ModelFormat unknownFormat = ModelFormat.findByIdentifier("UNKNOWN")
    Integer selectedValue = format ? format.id : unknownFormat?.id
    String readmeSubmission = workingMemory['readme_submission']
    String modellingApproach = workingMemory['modelling_approach']
    String otherInfo = workingMemory['other_info']
    List definedModellingApproachNames = workingMemory['defined_modelling_approaches'].collect { it.name }
%>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="${session['branding.style']}/main" />
        <title>Model Information - Submission | BioModels</title>
        <g:javascript>
            let definedModellingApproachNames = [];
            <g:each in="${definedModellingApproachNames}" var="name">
                definedModellingApproachNames.push("${name}");
            </g:each>
            let definedModelFormatNames = [];
            <g:each in="${modelFormatsSortedByName}" var="fmt">
                definedModelFormatNames.push("${fmt?.name + ' ' + fmt?.formatVersion}");
            </g:each>
        </g:javascript>
        <style type="text/css">
            .assistive-example {
                font-size: small;
                font-style: italic;
                color: darkgray
            }
        </style>
    </head>
    <body>
        <h2><g:message code="submission.biomodels.model.information.heading" locale="${Locale.getDefault()}"/></h2>
        <p><g:message code="submission.biomodels.model.information.explanation" locale="${Locale.getDefault()}"/></p>
        <g:form>
            <div class="row">
            <div class="small-12 medium-12 large-12 columns">
                <label for="name">
                    <span class="required">Name</span>&nbsp;
                    <span class="assistive-example">[e.g. Launna2020 - T-Cell signalling model]</span></label>
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
            </div></div>
            <div class="row">
                <div class="small-12 medium-6 large-6 columns">
                    <label for="model_format">
                        <span class="required">Model Format</span>&nbsp;
                        <span class="assistive-example">[e.g. SBML L3V2, Python 2.7, C/C++]</span></label>
                    <g:if test="${workingMemory['model_type']}">
                    </g:if>
                    <g:select name="model_format" id="model_format" required=""
                              from="${modelFormatsSortedByName}"
                              value="${selectedValue}"
                              optionKey="id"
                              optionValue="${{it?.name + ' ' + it?.formatVersion}}"/>
                    <div id="readme_submission_div" style="display: none">
                        <label for="readme_submission" class="required">
                            Describe more exactly your model format (e.g. SBML L3V2, Python 2.7, C/C++)</label>
                        <g:textField name="readme_submission" id="readme_submission"
                                     value="${readmeSubmission}"
                                     placeholder="Please describe here more accurately what is your model format" /></div>
                </div>
                <div class="small-12 medium-6 large-6 columns">
                    <label for="modelling_approach">
                        <span class="required">Modelling Approach</span>&nbsp;
                        <span class="assistive-example">[e.g. Constraint-based modelling, Logical model, Markov model,...]</span></label>
                    <g:textField name="modelling_approach" id="modelling_approach" value="${modellingApproach}"
                                 placeholder="Enter your modelling approach" required="true"
                                 aria-describedby="modellingApproachHelp"/>
                    <p class="help-text" id="modellingApproachHelp">Find the appropriate one by typing a few more
                    first characters of your words. The system will suggest you our defined modelling approaches. If you
                    are not sure your modelling approach, please type Other for now.</p>

                    <div id="model_other_info_div" style="display: none">
                        <label for="other_info" class="required">Describe more exactly your modelling approach</label>
                        <g:textField name="other_info" id="other_info"
                                     value="${otherInfo}"
                                     placeholder="Please enter here what is your modelling approach"/></div>
                </div>
            </div>
            <input type='hidden' value='false' name='changed' id="changeStatus"/>
            <div class="buttons">
                <g:submitButton name="Cancel" class="button" value="Abort" />
                <g:submitButton name="Back" class="button" value="Back" />
                <g:submitButton name="Continue" class="button" value="Continue" />
            </div>
        </div>
        </g:form>

        <!-- warning model popup -->

        <div class="reveal" id="modellingApproachWarningPopup" data-reveal>
            <h3 id="messageTitle"
                style="border-bottom: 1px solid grey">Attention!</h3>
            <button class="close-button" data-close aria-label="Close modal" type="button">
                <span aria-hidden="true">&times;</span>
            </button>
            <div class="contact-panel" id="feedback_panel" data-toggler=".is-active">
                <p class="lead">You have entered a value as your modelling approach which does not exist in our system.
                Please check your spelling or try again. Otherwise, type 'Other' and enter what is your modelling
                approach in the box below.</p>
            </div>
        </div>


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
            $(document).ready(function () {
                associateEventHandlers("description");
                associateEventHandlers("name");
                handleModelFormatBoxState();
                handleShowOrHideModelFormatExtraInfo($('#model_format'));
                handleShowOrHideModellingApproachExtraInfo($('#modelling_approach'), false)
            });

            function handleModelFormatBoxState() {
                let mf = $('#model_format');
                if (${format.name != unknownFormat?.name}) {
                    mf.attr('disabled', true);
                } else {
                    mf.attr('disabled', false);
                }
            }

            $('#modelling_approach').on('keydown', function () {
                $(this).autocomplete({
                    source: function (request, response) {
                        $.ajax({
                            url: $.jummp.createLink('model', 'searchModellingApproach'),
                            type: 'POST',
                            dataType: 'json',
                            data: {
                                search: request.term,
                                request: ${net.biomodels.jummp.webapp.RequestType.SEARCH_TERMS.value}
                            },
                            success: function (data) {
                                response(data);
                            }
                        });
                    },
                    select: function (event, ui) {
                        let label = ui.item.label;
                        $(this).val(label);   // display the selected text
                        let id = ui.item.id; // selected value
                        $.ajax({
                            url: $.jummp.createLink('model', 'searchModellingApproach'),
                            type: 'POST',
                            data: {
                                id: id,
                                name: label,
                                request: ${net.biomodels.jummp.webapp.RequestType.SELECT_VALUE.value}
                            },
                            dataType: 'json',
                            success: function (response) {
                                let len = response.length;
                                if (len > 0) {
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

            $('#modelling_approach').on('change', function () {
                handleShowOrHideModellingApproachExtraInfo(this, true);
            });

            $('#model_format').on("change", function () {
                handleShowOrHideModelFormatExtraInfo(this);
            });


            function handleShowOrHideModellingApproachExtraInfo(selector, flag) {
                let element = $('#model_other_info_div');
                let inputVal = $(selector).val();
                let comparableVal = 'Other';
                if (flag) {
                    showWarningMessageIfNecessary(inputVal, definedModellingApproachNames);
                }
                showOrHideBox(element, inputVal, comparableVal, definedModellingApproachNames);
            }

            function handleShowOrHideModelFormatExtraInfo(selector) {
                let $opt = $(selector).find('option:selected');
                let element = $('#readme_submission_div');
                let selectedFormat = $opt.val();
                let selectedText = $opt.text();
                let comparableText = 'Original code *';
                showOrHideBox(element, selectedText, comparableText, definedModelFormatNames);
            }

            function showWarningMessageIfNecessary(inputVal, definedModellingApproachNames) {
                inputVal = $.trim(inputVal);
                let existed = $.inArray(inputVal, definedModellingApproachNames) >= 0;
                if (!existed) {
                    console.log("show dialog box");
                    let popup = new Foundation.Reveal($('#modellingApproachWarningPopup'));
                    popup.open();
                }
            }

            function showOrHideBox(element, selectedText, comparableText, listOfDefinedValues) {
                selectedText = $.trim(selectedText);
                let existed = $.inArray(selectedText, listOfDefinedValues) >= 0;
                if (selectedText.toLowerCase() === comparableText.toLowerCase() || !existed) {
                    $(element).removeAttr("style").show();
                } else {
                    $(element).hide();
                }
            }
        </g:javascript>

    </body>
    <g:render template="/templates/decorateSubmission" />
    <g:render template="/templates/subFlowContextHelp" />
