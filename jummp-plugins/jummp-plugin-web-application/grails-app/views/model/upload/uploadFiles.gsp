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











<%@ page contentType="text/html;charset=UTF-8" %>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="main"/>
        <title><g:message code="submission.upload.header"/></title>
        <g:javascript contextPath="" src="jquery/jquery-ui-v1.10.3.js"/>
        <g:if test ="${showProceedWithoutValidationDialog || showProceedAsUnknownFormat}">
            <link rel="stylesheet" href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css" />
        </g:if>
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
        <g:uploadForm id="fileUpload" novalidate="false" autocomplete="false" name="fileUploadForm">
            <div class="dialog">
                <table class="formtable">
                    <tbody>
                        <jummp:displayExistingMainFile main = "${workingMemory['main_file']}"/>
                    </tbody>
                </table>
                <div id="noMains"></div>
                <fieldset>
                    <legend>
                        <jummp:renderAdditionalFilesLegend/>
                    </legend>
                    <a href="#" id="addFile"><jummp:renderAdditionalFilesAddButton/></a>
                    <table class='formtable' id="additionalFiles">
                        <tbody>
                            <g:if test="${workingMemory['additional_repository_files_in_working']}">
                                <jummp:displayExistingAdditionalFiles
                                    additionals = "${workingMemory['additional_repository_files_in_working']}"/>
                            </g:if>
                            <g:else>
                                <jummp:displayExistingAdditionalFiles
                                    additionals = "${workingMemory['additional_files']}"/>
                            </g:else>
                        </tbody>
                    </table>
                    <div id="additionalFilesOnUI" name="additionalFilesOnUI" style="display: none">
                        <g:if test="${workingMemory['additional_repository_files_in_working']}">
                            <jummp:populateExistingAdditionalFilesOnUI
                                additionalsOnUI = "${workingMemory['additional_repository_files_in_working']}"/>
                        </g:if>
                        <g:elseif test="${workingMemory['additional_files']}">
                            <jummp:populateExistingAdditionalFilesOnUI
                                additionalsOnUI = "${workingMemory['additional_files']}"/>
                        </g:elseif>
                        <g:else>
                            empty
                        </g:else>
                    </div>
                    <div id="noAdditionals"></div>
                    <div id="additionalsOnUI"></div>
                </fieldset>
                <div class="buttons">
                    <g:submitButton name="Cancel" value="${g.message(code: 'submission.common.cancelButton')}" />
                    <g:if test="${!isUpdate}">
                        <g:submitButton name="Back" value="${g.message(code: 'submission.common.backButton')}" />
                    </g:if>
                    <g:submitButton name="Upload" value="${g.message(code: 'submission.upload.uploadButton')}" />
                    <g:if test ="${showProceedWithoutValidationDialog || showProceedAsUnknownFormat}">
                        <g:submitButton name="ProceedWithoutValidation" value="ProceedWithoutValidation" hidden="true"/>
                    </g:if>
                    <g:if test ="${showProceedAsUnknownFormat}">
                        <g:submitButton name="ProceedAsUnknown" value="ProceedAsUnknown" hidden="true"/>
                    </g:if>
                </div>
            </div>
        </g:uploadForm>
        <g:javascript>
            var nbExtraFiles = 0;
            var numberOfAdditionalsAtLoadingPage = $('input[id^=description]').size();
            // create a map to store additional files existing on user interface
            var additionalFilesExitingOnUI = new Object();
            function populateDiv() {
                document.getElementById("additionalFilesOnUI").innerHTML = "";
                var str = "";
                $.each(additionalFilesExitingOnUI, function(key, value) {
                    str += key.replace(":","").trim() + " : " + value.trim() + ", ";
                });
                var str = str.substring(0,str.length - 2);
                document.getElementById("additionalFilesOnUI").innerHTML += str;
                document.getElementById("additionalsOnUI").innerHTML = "<input value='" +
                document.getElementById("additionalFilesOnUI").innerHTML + "' name='additionalFilesInWorking' hidden/>";
            }

            function get(k) {
                return additionalFilesExitingOnUI[k];
            }

            $(document).ready(function () {
                var additionals = document.getElementById("additionalFilesOnUI").innerHTML;
                additionals = additionals.trim();
                if (additionals != 'empty') {
                    additionals.slice(0,-1);
                    var arr = additionals.split(", ");
                    $.each(arr, function(k,v) {
                        var row = $.trim(v).split(":");
                        additionalFilesExitingOnUI[row[0].replace(":","").trim()] = row[1].trim();
                    });
                }

                $('.removeMain').click(function(e) {
                    e.preventDefault();
                    var parent = $(this).parent().get(0).innerHTML;
                    var trimmedParent = parent.replace(/^\s+/g,"");
                    var start = "<span id='mainName_".length;
                    var end = trimmedParent.indexOf("\">", start);
                    var name = trimmedParent.substring(start, end);
                    var hi = "<input value='" + name + "' name='deletedMain' hidden>";
                    document.getElementById("noMains").innerHTML += hi;
                    $(this).parent().get(0).innerHTML = "<input type='file' id='mainFile' name='mainFile' class='mainFile' >\n\t</td>\n</tr>";

                });

                $('.replaceMain').click(function(e) {
                    e.preventDefault();
                    $(this).parent().get(0).getElementsByTagName("input")[0].click();
                });

                $('.mainFile').change(function(click) {
                    var oldName = $(this).data("labelname");
                    var hi = "<input type='hidden' value='" + oldName + "' name='deletedMain'/>";
                    document.getElementById("noMains").innerHTML += hi;
                    var id = "mainName_" + oldName;
                    var newValue = this.value;
                    var newName = trimElementName("\\", newValue);
                    document.getElementById(id).innerHTML = newName;
                });

                $("#addFile").click(function (evt) {
                    evt.preventDefault();
                    $('<tr>', {
                        class: 'fileEntry'
                    }).append(
                        $('<td class="name">').append(
                            $('<input/>', {
                                type: 'file',
                                id: 'extraFiles' + nbExtraFiles,
                                name: 'extraFiles'
                            })
                        ),
                        $('</td><td style="width: 285px">').append(
                            $('<input/>', {
                                type: 'text',
                                id: 'description' + ++numberOfAdditionalsAtLoadingPage,
                                name: 'description' + numberOfAdditionalsAtLoadingPage,
                                style: "width: 100%; box-sizing: border-box; -webkit-box-sizing: border-box; -moz-box-sizing: border-box;",
                                placeholder: 'Please enter a description'
                            })
                        ),
                        $('</td><td>&nbsp;').append(
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
                    document.getElementById("additionalsOnUI").innerHTML = "<input value='" +
                    document.getElementById("additionalFilesOnUI").innerHTML + "' name='additionalFilesInWorking' hidden/>";
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
                    delete additionalFilesExitingOnUI[fileName];
                    populateDiv();
                }
                $(tr).empty();
            });

            $(document).on("change", 'input[type=file]', function (e) {
                var id = $(this).attr('id');
                if (id != 'mainFile') {
                    var fileName = $(this)[0].files[0].name;
                    if (additionalFilesExitingOnUI[fileName]) {
                        alert("The file named " + fileName + " already exists. Please rename it or select another file.");
                    } else {
                        additionalFilesExitingOnUI[fileName] = "";
                        $(this).attr('value', fileName);
                        var discardID = "discard" + $(this).attr('id');
                        $("#"+discardID).attr('download', fileName);
                        populateDiv();
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
                    additionalFilesExitingOnUI[fileName] = $(this).val();
                }
                populateDiv();
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

