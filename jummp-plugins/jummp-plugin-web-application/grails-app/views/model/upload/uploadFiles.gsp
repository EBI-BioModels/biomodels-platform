<%--
 Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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










<%@page expressionCodec="none" %>
<%@ page import="net.biomodels.jummp.core.model.RepositoryFileTransportCommand; grails.converters.JSON"
        contentType="text/html;charset=UTF-8" %>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="${session['branding.style']}/main" />
        <title><g:message code="submission.upload.header"/></title>
        <g:javascript contextPath="" src="jquery/jquery-ui-v1.10.3.js"/>
        <g:if test ="${showProceedWithoutValidationDialog || showProceedAsUnknownFormat}">
            <link rel="stylesheet" href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}",
                dir: '/css/jqueryui/smoothness', file: 'jquery-ui-1.10.3.custom.min.css')}" />
        </g:if>
        <script type="text/javascript">
            var descriptionMainMap = { "files": ${workingMemory['main_files'].collect {
                            RepositoryFileTransportCommand rf ->
                                String key = new File(rf.path).name
                                String value = rf.description
                                [ filename: key, description: value ]
                        } as JSON}
            };
            var existingMainFiles = descriptionMainMap["files"];
            var descriptionAdditionalMap = { "files": ${workingMemory['additional_files'].collect {
                            RepositoryFileTransportCommand rf ->
                                String key = new File(rf.path).name
                                String value = rf.description
                                [ filename: key, description: value ]
                        } as JSON}
            };
            var extraFiles = descriptionAdditionalMap["files"];
            var existingAdditionalFiles = [];
            $.each(extraFiles, function (index) {
                var filename = extraFiles[index].filename;
                var description = extraFiles[index].description;
                var element = {'inputId': 'extraFiles'.concat(index), 'filename': filename, 'description': description}
                existingAdditionalFiles.push(element);
            });
        </script>
    </head>
    <body>
        <g:if test="${showProceedAsUnknownFormat}">
            <div id="dialog-confirm" title="Model Format Error">
                <p>The model was detected as ${modelFormatDetectedAs} but is not a
                supported version. You can proceed with the submission but the model
                will be stored as an unknown model. Would you like to proceed?</p>
            </div>
        </g:if>
        <g:if test ="${showProceedWithoutValidationDialog}">
            <div id="dialog-confirm" title="Validation Error">
                <p>The model files did not pass validation, with errors as below. Would you like to proceed?</p>
                <ul><g:each in="${workingMemory['validationErrorList']}">
                    <li>${it}</li>
                    </g:each>
                </ul></p>
            </div>
        </g:if>
        <g:render template="/templates/errorMessage"/>
        <h2><g:message code="submission.upload.header"/></h2>
        <p style="padding-bottom:1em"><g:message code="submission.upload.explanation"/></p>
        <g:uploadForm id="fileUpload" novalidate="false" autocomplete="false" name="fileUploadForm"
                      onsubmit="return validate()">
            <div class="dialog">
                <g:if test="${workingMemory['main_repository_files_in_working']}">
                    <g:set var="mainfiles" value="${workingMemory['main_repository_files_in_working']}"/>
                </g:if>
                <g:elseif test="${workingMemory['main_files']}">
                    <g:set var="mainfiles" value="${workingMemory['main_files']}" />
                </g:elseif>
                <g:else>
                    <g:set var="mainfiles" value="${[]}" />
                </g:else>
                <jummp:displayExistingMainFile main="${mainfiles}"/>
                <div id="noMains" style="display: none;"></div>

                <jummp:renderAdditionalFilesLegend/>
                <div id="additionalFilesExplanation"><jummp:renderAdditionalFilesExplanation/></div>
                <a href="#" id="addFile"><jummp:renderAdditionalFilesAddButton/></a>
                <g:if test="${workingMemory['additional_repository_files_in_working']}">
                    <g:set var="resource" value="${workingMemory['additional_repository_files_in_working']}" />
                </g:if>
                <g:elseif test="${workingMemory['additional_files']}">
                    <g:set var="resource" value="${workingMemory['additional_files']}" />
                </g:elseif>
                <g:else>
                    <g:set var="resource" value="${[]}" />
                </g:else>
                <jummp:displayExistingAdditionalFiles additionals="${resource}"/>
                <div id="noAdditionals" style="display: none"></div>

                <div class="buttons">
                    <g:submitButton name="Cancel" class="button" value="${g.message(code: 'submission.common.cancelButton')}" />
                    <g:if test="${!isUpdate}">
                        <g:submitButton name="Back" class="button" value="${g.message(code: 'submission.common.backButton')}" />
                    </g:if>
                    <g:submitButton name="Upload" class="button" value="${g.message(code: 'submission.upload.uploadButton')}" />
                    <g:if test="${showProceedWithoutValidationDialog || showProceedAsUnknownFormat}">
                        <g:submitButton name="ProceedWithoutValidation" class="button" value="ProceedWithoutValidation" hidden="true"/>
                    </g:if>
                    <g:if test="${showProceedAsUnknownFormat}">
                        <g:submitButton name="ProceedAsUnknown" class="button" value="ProceedAsUnknown" hidden="true"/>
                    </g:if>
                </div>
            </div>
        </g:uploadForm>
        <g:javascript>
            $('#additionalFilesExplanation').hide();
            $('#howAboutThis').click(function () {
                if ($("div#additionalFilesExplanation").is(":hidden")) {
                    $("div#additionalFilesExplanation").show("slow");
                } else {
                    $("div#additionalFilesExplanation").slideUp();
                }
            });
            $('.flashNotificationDiv').click(function() {
                $(this).hide();
            });

            var nbExtraFiles = $('input[id^=description]').size();
            var clickBack = false;
            var clickCancel = false;

            function validate() {
                if (clickBack || clickCancel) {
                    // click Back or Cancel button in either updating or creating process
                    return true;
                } else {
                    // click Upload button in either updating or creating process
                    var message = "";
                    if (${isUpdate}) {
                        message = "Updating the model process";
                    } else {
                        message = "Creating the model process";
                    }

                    /* validate the upload form */
                    // validate the descriptions of the additional files
                    var hasEmptyAddFileDesc = $("input[id^=description]").filter(function() {
                        var element = $(this);
                        return $.trim(this.value) === "";
                    });
	                // validate the descriptions of the model/main files
                    var hasEmptyMainFileDesc = $("input[id^=mainFileDescription]").filter(function() {
                        var element = $(this);
                        return $.trim(this.value) === "";
                    });

                    var isFileDescValid = hasEmptyAddFileDesc.length == 0 && hasEmptyMainFileDesc.length == 0;
	                var hasAtLeastMainFile = existingMainFiles.length > 0

                    // validate the descriptions of the main files in the main files map
                    var containsEmptyMainDescInMap = existingMainFiles.filter(function(v) {
                        return v.description === "";}).length > 0;
                    if (isFileDescValid && hasAtLeastMainFile && !containsEmptyMainDescInMap) {
                        return true;
                    } else {
                        var flashDiv = $('.flashNotificationDiv');
                        $(flashDiv).html("Please fill in all required fields");
                        $(flashDiv).show();
                        return false;
                    }
                }
            }

            $(document).ready(function () {
                var isMainFileReplaced = false;
                $('.replaceMain').click(function(e) {
                    e.preventDefault();
                    $('#mainFile').click();
                    isMainFileReplaced = true;
                });

                $('.removeMain').click(function(e) {
                    e.preventDefault();
                    var td = $(this).parent().get(0);
                    var tr = $(td).parent().get(0);
                    var parent = $(tr).find("td:first").html();
                    var span = $($.parseHTML(parent))[1];
                    var id = span.id;
                    if ($('#'+id).is("span")) {
                        var mainFileName = $('#'+id).text();
                        $('#'+id).text('');
                        for (var index = 0; index < existingMainFiles.length; index++)
                            if (existingMainFiles[index].filename === mainFileName) {
                                existingMainFiles.splice(index, 1);
                            }
                        $('#'+id).remove();
                        // the former main file will be deleted
                        var hi = "<input value='" + mainFileName + "' name='deletedMain'>";
                        document.getElementById("noMains").innerHTML += hi;
	                }
                    // hidden Replace button to avoid being confused
                    $(td).text("");

                    // reshow the file upload
                    if (existingMainFiles.length === 0) {
                        $('#mainFile').show();
                    } else {
                        $(tr).remove();
                    }
                });
                var formerMainFileName = "";
                $('#mainFile').change(function(event) {
                    var newModelFile = $(this)[0].files[0];
                    if (newModelFile) {
                        var newFileName = newModelFile.name;
                        var mainFileDescription = $('input[name=mainFileDescription]').val();
                        if (existingMainFiles.length === 0) {
                            // new submission or update but all the main files has been removed
                            // so retain the working main file
                            formerMainFileName = newFileName;
                            // add the new file to existingMainFiles
                            var newFile = {filename: newFileName, description: mainFileDescription}
                            existingMainFiles.push(newFile);
                            // display it on the page
                            $(this).attr("value", newFileName);
                            $(this).css("display", "inline");
                            //var discardID = "discard" + $(this).attr('id');
                            //$("#"+discardID).attr('download', fileName);
                            $('#mainFileDescription').val('');

                        } else {
                            var td = $(this).parent().get(0);
                            var span = $($.parseHTML($(td).html()))[1];
                            if ($('#' + span.id).is("span")) {
                                formerMainFileName = $('#' + span.id).text();
                            } else {
                                formerMainFileName = existingMainFiles.last()["filename"];
                            }

                            var hi = "<input value='" + formerMainFileName + "' name='deletedMain'>";
                            document.getElementById("noMains").innerHTML += hi;

                            if (isMainFileReplaced) { // update process
                                /* remove the old/current one */
                                for (var index = 0; index < existingMainFiles.length; index++)
                                if (existingMainFiles[index].filename === formerMainFileName) {
                                    existingMainFiles.splice(index, 1);
                                    // update the former main on GUI
                                }

                                isMainFileReplaced = false;
                                /* update the new file */
                                // display the new file to gui
                                span = $(span)[0].id;
                                $('#' + span).text(newFileName);
                            } else { // submission process
                                for (var index = 0; index < existingMainFiles.length; index++)
                                    if (existingMainFiles[index].filename === formerMainFileName) {
                                        existingMainFiles.splice(index, 1);
                                        // update the former main on GUI
                                    }
                            }
                            // add the new file to existingMainFiles
                            var newFile = {filename: newFileName, description: mainFileDescription}
                            existingMainFiles.push(newFile);
                            $(this).attr('value', newFileName);
                        }
                    }
                });

                $('input[id^=mainFileDescription]').change(function() {
                    var parent = $(this).parent().parent();
                    var content = parent.find("td:first").html();
                    var span = $.parseHTML(content);
                    var span = $(span)[1];
                    if (existingMainFiles.length > 0) {
                        var id = span.id;
                        var fileName = "";
                        if ($('#' + id).is("span")) {
                            // case: there are existing main files in the submission or update process
                            fileName = $('#' + id).text();
                        } else {
                            // case: only happen in the submission process
                            fileName = $('input[id^=mainFile]')[0].files[0].name;
                        }
                        var description = $(this).val();
                        if (fileName) {
                            if (existingMainFiles.length > 0) {
                                existingMainFiles.filter(function(v) {
                                    return v.filename === fileName;
                                })[0].description = description;
                            } else if (fileName) {
                                var newFile = {filename: fileName, description: fileName}
                                existingMainFiles.push(newFile);
                            }
                        }
                    } else {
                        var flashDiv = $('.flashNotificationDiv');
                        $(flashDiv).html("The main file cannot be empty");
                        $(flashDiv).show();
                    }
                });

                $("#addFile").click(function (evt) {
                    var index = nbExtraFiles;
                    evt.preventDefault();
                    $('<tr>', {
                        class: 'fileEntry'
                    }).append(
                        $('<td class="name" style="width: 20%">').append(
                            $('<input/>', {
                                type: 'file',
                                id: 'extraFiles' + index,
                                name: 'extraFiles'
                            })
                        ),
                        $('</td><td style="width: 70%">').append(
                            $('<input/>', {
                                type: 'text',
                                id: 'description' + index,
                                name: 'description',
                                style: "width: 100%; box-sizing: border-box; -webkit-box-sizing: border-box; -moz-box-sizing: border-box;",
                                placeholder: 'Please enter a description'
                            }).prop('required', true)
                        ),
                        $('</td><td style="width: 10%; display: table-cell; vertical-align: middle; text-align: center">&nbsp;').append(
                            $('<a>', {
                                href: "#",
                                class: 'killer',
                                text: 'Discard',
                                id: 'discardextraFiles' + index
                            })
                        ).append("</a>")
                    ).appendTo('table#additionalFiles');
                    nbExtraFiles++;
                });

                $("#_eventId_Back").click( function() {
                    clickBack = true;
                });

                $("#_eventId_Cancel").click( function() {
                    clickCancel = true;
                });
            });

            $(document).on("click", 'a.killer', function (e) {
                e.preventDefault();
                var tr = $(this).parent().parent().get(0);
                var td = tr.getElementsByClassName("name")[0];
                if (td) {
                    // collect the additional files that the user has just discarded
                    var fileName = td.innerHTML.substring(0,td.innerHTML.indexOf("<")).trim();
                    var hi = "<input type='text' value='" + fileName + "' name='deletedAdditional'/>";
                    document.getElementById("noAdditionals").innerHTML += hi;
                    // update the map
                    var fileName = td.innerHTML.substring(0,td.innerHTML.indexOf("<"));
                    if (fileName === '') { // this file has just added in extraFiles division
                        var id = $(this).attr('id');
                        var idFileUpload = id.substr('discard'.length);
                        fileNameAbsolutePath = $("#"+idFileUpload).val();
                        // 12 = ('C:\fakepath\').length
                        if (fileNameAbsolutePath) {
                            fileName = fileNameAbsolutePath.substr(12);
                        }
                    }
                    // find and delete the object having the filename property equals to fileName
                    if (fileName) {
                        for (var index = 0; index < existingAdditionalFiles.length; index++)
                            if (existingAdditionalFiles[index].filename === fileName.trim()) {
                                existingAdditionalFiles.splice(index, 1);
                            }
                    }
                }
                $(tr).empty();
            });

            $(document).on("change", 'input[id^=extraFiles]', function (e) {
                // this copes with amending the additional files back and forth
                if ($(this)[0].files[0]) {
                    var inputId = $(this).attr("id").trim();
                    var fileName = $(this)[0].files[0].name;
                    var regex = new RegExp(/[0-9]+$/g);
                    var id = regex.exec(inputId);
                    var description = $('#description'+id).val();
                    var element = {inputId: inputId, filename: fileName, description: description}
                    // add the new file to the map existingAdditionalFiles
                    /* allow to change additional files flexibly */
                    var existed = false;
                    for (var index = 0; index < existingAdditionalFiles.length; index++)
                    if (existingAdditionalFiles[index].inputId === inputId) {
                        existingAdditionalFiles[index].filename = fileName;
                        existed = true;
                    }
                    if (!existed) {
                        existingAdditionalFiles.push(element);
                    }
                }
            });

            $(document).on("change", "input[id^=description]", function() {
                // get the id of the current input tag, then get its counter for the next step
                // because each of the additional files is displayed in table row
                var tr = $(this).parent().parent().get(0);
                // the table has three columns. The file name of the additional file is in the first column.
                var td = tr.getElementsByClassName("name")[0];
                if (td) {
                    var hi = td.innerHTML;
                    // update Files Existing on UI
                    // there is an input tag set hidden to store RFTC object. We need only the file name.
                    var posLessThan = hi.indexOf("<");
                    var fileName = "";
                    if (posLessThan > 0) {
                        // this case is we are updating an existing file persisted in the database
                        fileName = hi.substring(0,posLessThan);
                    } else {
                        // this case is we are editing the file just added by clicking "Add an additional file"
                        var tempDiv = document.createElement('div');
                        tempDiv.innerHTML = hi;
                        var elements = tempDiv.childNodes;
                        var inputId = elements[0].getAttribute("id");
                        var newFile = $('#' + inputId)[0].files[0];
                        fileName = newFile ? newFile.name : "";
                    }
                    if (fileName) {
                        fileName = fileName.trim();
                        var extraFile = existingAdditionalFiles.filter(function(v) {
                            return v.filename === fileName;
                        })[0];
                        if (extraFile) {
                            extraFile.description = $(this).val();
                        }
                    }
                }
            });

            $( "#dialog-confirm" ).dialog({
                resizable: false,
                height:300,
                width:500,
                modal: true,
                buttons: {
                    "Proceed Without Validation": function() {
                        var eventID = '_eventId_ProceedWithoutValidation';
                        <g:if test='${showProceedAsUnknownFormat}'>
                            eventID = '_eventId_ProceedAsUnknown';
                        </g:if>
                        document.getElementById(eventID).click();
                        $( this ).dialog( "close" );
                    },
                    Cancel: function() {
                        $( this ).dialog( "close" );
                    }
                }
            });
        </g:javascript>
    </body>
   <g:render template="/templates/decorateSubmission" />
   <g:render template="/templates/subFlowContextHelp" />
