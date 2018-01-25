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
<%@ page import="net.biomodels.jummp.core.model.RepositoryFileTransportCommand; grails.converters.JSON" contentType="text/html;charset=UTF-8" %>
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
            var descriptionMap = { "files": ${workingMemory['additional_files'].collect {
                            RepositoryFileTransportCommand rf ->
                                String key = new File(rf.path).name
                                String value = rf.description
                                [ filename: key, description: value ]
                        } as JSON}
            };
            var existingAdditionalFiles = descriptionMap["files"];
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
            <ul>
            	<g:each in="${workingMemory['validationErrorList']}">
            		<li>${it}</li>
            	</g:each>
            </ul>
            </p>
          </div>
        </g:if>
        <g:render template="/templates/errorMessage"/>
        <h2><g:message code="submission.upload.header"/></h2>
        <p style="padding-bottom:1em"><g:message code="submission.upload.explanation"/></p>
        <g:uploadForm id="fileUpload" novalidate="false" autocomplete="false" name="fileUploadForm" onsubmit="return validate()">
            <div class="dialog">
                <jummp:displayExistingMainFile main="${workingMemory['main_file']}"/>
                <div id="noMains"></div>
                <jummp:renderAdditionalFilesLegend/>
                <div id="additionalFilesExplanation"><jummp:renderAdditionalFilesExplanation/></div>
                <fieldset>
                    <a href="#" id="addFile"><jummp:renderAdditionalFilesAddButton/></a>
                    <table class='formtable responsive-table' id="additionalFiles">
                        <tbody>
                            <g:if test="${workingMemory['additional_repository_files_in_working']}">
                                <g:set var="resource" value="${workingMemory['additional_repository_files_in_working']}" />
                            </g:if>
                            <g:elseif test="${workingMemory['additional_files']}">
                                <g:set var="resource" value="${workingMemory['additional_files']}" />
                            </g:elseif>
                            <g:else>
                                <g:set var="resource" value="${[]}" />
                            </g:else>
                            <jummp:displayExistingAdditionalFiles additionals = "${resource}"/>
                        </tbody>
                    </table>
                    <div id="noAdditionals"></div>
                    <!-- This div stores input element which value is assigned to JSON string -->
                    <div id="additionalsOnUI" style="display: none;"></div>
                </fieldset>
                <div class="buttons">
                    <g:submitButton name="Cancel" class="button" value="${g.message(code: 'submission.common.cancelButton')}" />
                    <g:if test="${!isUpdate}">
                        <g:submitButton name="Back" class="button" value="${g.message(code: 'submission.common.backButton')}" />
                    </g:if>
                    <g:submitButton name="Upload" class="button" value="${g.message(code: 'submission.upload.uploadButton')}" />
                    <g:if test ="${showProceedWithoutValidationDialog || showProceedAsUnknownFormat}">
                        <g:submitButton name="ProceedWithoutValidation" class="button" value="ProceedWithoutValidation" hidden="true"/>
                    </g:if>
                    <g:if test ="${showProceedAsUnknownFormat}">
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
            var nbExtraFiles = 0;
            var numberOfAdditionalsAtLoadingPage = $('input[id^=description]').size();

            function updateAdditionalFilesOnUI() {
                descriptionMap.files = [];
                $.each(existingAdditionalFiles, function(index, fileEntry) {
                    // key here is the index, value is the actual value we are interested in
                    var fileName = fileEntry["filename"];
                    var fileDescription  = fileEntry["description"];
                    descriptionMap.files.push({'filename': fileName, 'description': fileDescription});
                });
                // update the hidden input element containing the latest additional files
                // the map should be converted to json string that will be transfered to controller
                var input = "<input name='additionalFilesInWorking' size='220' value='";
                    input += JSON.stringify(descriptionMap) + "'/>";
                document.getElementById("additionalsOnUI").innerHTML = input;
            }

            function validate() {
                // validate the upload form
                var result = $("input[id^=description]").filter(function() {
                    var element = $(this);
                    console.log(element.val());
                    return $.trim(this.value) === "";
                });
                var mainValid = $("input[id^=mainFileDescription]").filter(function() {
                    var element = $(this);
                    console.log(element.val());
                    return $.trim(this.value) === "";
                });
                var isValid = result.length == 0 && mainValid.length == 0;
                if (isValid) {
                    console.log("All required fields have been filled in");
                    return true;
                } else {
                    var flashDiv = $('.flashNotificationDiv');
                    $(flashDiv).html("Please fill in all required fields");
                    $(flashDiv).show();
                    console.log("Some required fields cannot be empty");
                    return false;
                }
            }

            $(document).ready(function () {
                populateDiv();
                $('.replaceMain').click(function(e) {
                    e.preventDefault();
                    // firing a click event on the main file upload element
                    $('#mainFile').click();
                });

                $('.removeMain').click(function(e) {
                    e.preventDefault();
                    var td = $(this).parent().get(0);
                    var tr = $(td).parent().get(0);
                    console.log($(tr).find("td:first").html());
                    var tbody = $(td).parent().parent().get(0);
                    // update the temporary container's content
                    var parent = $(tr).find("td:first").html();
                    var trimmedParent = parent.replace(/^\s+/g,"");
                    var start = "<span id='mainName_".length;
                    var end = trimmedParent.indexOf("\">", start);
                    var name = trimmedParent.substring(start, end);
                    var hi = "<input value='" + name + "' name='deletedMain' hidden>";
                    document.getElementById("noMains").innerHTML += hi;
                    // get rid of the current row where Remove button is placed
                    $(td).closest("tr").remove();
                    // generate a new row in order to allow browsing a new file
                    var row = "<jummp:renderRowInMainFileTable />";
                    $(tbody).append(row);
                });

                $('.mainFile').change(function(click) {
                    var oldName = $(this).data("labelname");
                    var hi = "<input type='hidden' value='" + oldName + "' name='deletedMain'/>";
                    document.getElementById("noMains").innerHTML += hi;
                    var id = "mainName_" + oldName;
                    var newValue = this.value;
                    var newName = trimElementName("\\", newValue);
                    document.getElementById(id).innerHTML = newName;
                    $('#mainFileDescription').val('');
                });

                $("#addFile").click(function (evt) {
                    evt.preventDefault();
                    $('<tr>', {
                        class: 'fileEntry'
                    }).append(
                        $('<td class="name" style="width: 20%">').append(
                            $('<input/>', {
                                type: 'file',
                                id: 'extraFiles' + nbExtraFiles,
                                name: 'extraFiles'
                            })
                        ),
                        $('</td><td style="width: 70%">').append(
                            $('<input/>', {
                                type: 'text',
                                id: 'description' + ++numberOfAdditionalsAtLoadingPage,
                                name: 'description',
                                style: "width: 100%; box-sizing: border-box; -webkit-box-sizing: border-box; -moz-box-sizing: border-box;",
                                placeholder: 'Please enter a description'
                            }).prop('required', true)
                        ),
                        $('</td><td style="width: 10%; display: table-cell; vertical-align: middle; text-align: right">&nbsp;').append(
                            $('<a>', {
                                href: "#",
                                class: 'killer',
                                text: 'Discard',
                                id: 'discardextraFiles' + nbExtraFiles++
                            })
                        ).append("</a>")
                    ).appendTo('table#additionalFiles');
                });

                $("#_eventId_Upload").click(function() {
                    var input = "<input name='additionalFilesInWorking' value='";
                    input += JSON.stringify(descriptionMap) + "'/>";
                    document.getElementById("additionalsOnUI").innerHTML = input;
                });

                $("#uploadButton").click( function() {
                    $("#fileUpload").submit();
                });

                $("#cancelButton").click( function() {
                    $("#fileUpload").reset();
                });
            });

            $(document).on("click", 'a.killer', function (e) {
                e.preventDefault();
                var tr = $(this).parent().parent().get(0);
                var td = tr.getElementsByClassName("name")[0];
                if (td) {
                    // collect the additional files existing we want to delete
                    var hi = "<input type='hidden' value='" + td.innerHTML + "' name='deletedAdditional'/>";
                    document.getElementById("noAdditionals").innerHTML += hi;

                    // update the map
                    var fileName = td.innerHTML.substring(0,td.innerHTML.indexOf("<"));
                    if (fileName == '') { // this file has just added in extraFiles division
                        var id = $(this).attr('id');
                        var idFileUpload = id.substr('discard'.length);
                        fileNameAbsolutePath = $("#"+idFileUpload).val();
                        // 12 = ('C:\fakepath\').length
                        fileName = fileNameAbsolutePath.substr(12);
                    }
                    // find and delete the object having the filename property equals to fileName
                    for (index in existingAdditionalFiles)
                        if (existingAdditionalFiles[index].filename == fileName) {
                            existingAdditionalFiles.splice(index, 1);
                        }
                    updateAdditionalFilesOnUI();
                }
                $(tr).empty();
            });

            $(document).on("change", 'input[type=file]', function (e) {
                var id = $(this).attr('id');
                if (id != 'mainFile') {
                    var fileName = $(this)[0].files[0].name;
                    if (existingAdditionalFiles.filter(function(v) {
                        return v.filename === fileName;
                    })[0]) {
                        alert("The file named " + fileName + " already exists. Please rename it or select another file.");
                    } else {
                        // add the new file to existingAdditionalFiles
                        var newFile = {filename: fileName, description: ""}
                        existingAdditionalFiles.push(newFile);
                        // display it on the page
                        $(this).attr('value', fileName);
                        var discardID = "discard" + $(this).attr('id');
                        $("#"+discardID).attr('download', fileName);
                        updateAdditionalFilesOnUI();
                    }
                }
            });

            $(document).on("change", "input[id^=description]", function() {
                // get the id of the current input tag, then get its counter for the next step
                // because each of the additional files is displayed in table row
                var tr = $(this).parent().parent().get(0);
                // the table has three columns. The file name of the additional file is in the first cell
                var td = tr.getElementsByClassName("name")[0];
                if (td) {
                    var hi = td.innerHTML;
                    // update Files Existing on UI
                    // there is an input tag set hidden to store RFTC object. We need only the file name.
                    var posLessThan = hi.indexOf("<");
                    if (posLessThan > 0) {
                        // this case is the existing file on database
                        fileName = hi.substring(0,posLessThan);
                    } else {
                        // this case is the file just added by clicking "Add an additional file"
                        var tempDiv = document.createElement('div');
                        tempDiv.innerHTML = hi;
                        var elements = tempDiv.childNodes;
                        fileName = elements[0].getAttribute("value");
                    }
                    existingAdditionalFiles.filter(function(v) {
                        return v.filename === fileName;
                    })[0].description = $(this).val();
                }
                updateAdditionalFilesOnUI();
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
            /*
             * Greedy removal of a string's prefix.
             *
             * This method does not change the original string. If it contains the supplied
             * separator, this method will return a new string that starts from the character
             * that follows the last occurrence of the separator. Otherwise, the string is returned
             * as-is.
             * @param sep The character that marks the end of the prefix to be removed.
             * @param elemName The string that should be trimmed
             * @return a new string stripped of the specified prefix.
             */
            function trimElementName(sep, elemName) {
                if (elemName.indexOf(sep) > -1) {
                    var idx = elemName.lastIndexOf(sep) + 1;
                    var stopIdx = elemName.length;
                    var trimmedName = elemName.substring(idx, stopIdx);
                    return trimmedName;
                }
                return elemName;
            }
        </g:javascript>
    </body>
   <g:render template="/templates/decorateSubmission" />
   <g:render template="/templates/subFlowContextHelp" />
