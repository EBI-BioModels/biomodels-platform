<%--
 Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

<%--
  Created by IntelliJ IDEA.
  User: tnguyen@ebi.ac.uk
  Date: 2019-03-25
  Time: 11:40
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="grails.converters.JSON;" %>

<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>${title}</title>
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "css/${style}", file: 'publicationPageStyle.css')}" />
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "css", file: 'toastr.min.css')}"/>

    <g:javascript src="helpers.js" contextPath=""/>
    <g:javascript src="toastr.min.js" contextPath=""/>
    <g:javascript>
        // Indeed, we don't need to check whether the authors is null or not because if case of the model has
        // no publication yet, we always create an empty PersonTransportCommand to maintain authors
        // during the submission flow.
        if (${publication != null}) {
            var authorMap = {"authors": ${publication?.authors?.collect {
                String userRealName = it.userRealName ?: ""
                String institution = it.institution ?: ""
                String orcid = it.orcid ?: ""
                def id = it.id ?: "undefined"
                if (id == "undefined") {
                    [userRealName: userRealName, institution: institution, orcid: orcid]
                } else {
                    [id: id, userRealName: userRealName, institution: institution, orcid: orcid]
                }
            } as JSON}};
            var authorList = authorMap["authors"];
        } /*else {
            var authorMap = {"authors": []};
            var authorList = {};
        }*/
    </g:javascript>
    <g:javascript src="${style}/publicationSubmission.js" contextPath="" />
</head>

<body>
    <h2>Publication Details</h2>
    <g:if test="${publication}">
        <g:render template="/templates/publication/selectPublicationSource"
                  plugin="jummp-plugin-web-application"
                  model="['publication': publication, 'controller': controller,
                          'operation': operation,
                          'linkSourceTypes': linkSourceTypes]" />
        <div id="publicationForm">
            <g:render template="/templates/publication/publicationEditableElements"
                      plugin="jummp-plugin-web-application"
                      model="['publication': publication, 'authorListContainerSize': authorListContainerSize]"/>
            <g:render template="/templates/publication/publicationButtonsForm"
                      plugin="jummp-plugin-web-application"/>
        </div>
        <g:javascript>
            $('#btnSave').on("click", function(event) {
                "use strict";
                event.preventDefault();
                // validate the form
                if ($('#pubLinkProvider').val() === "NoPub") {
                    toastr.clear();
                    toastr.error("Cannot save the publication without choosing a type of publication resource");
                    return;
                }
                validation = validation && validateDataForm('publicationForm');
                if (!validation) {
                    let msg = "The publication source and link do not match. Please verify these values and try again";
                    toastr.clear();
                    toastr.error(msg);
                    showFlashMessages(msg);
                    return;
                }
                $.ajax({
                    type: "POST",
                    url: $.jummp.createLink("publication", "save"),
                    cache: true,
                    processData: true,
                    async: true,
                    data: JSON.stringify(buildPublicationTC()),
                    contentType: "application/json",
                    beforeSend: function() {
                        toastr.info("The publication details are being saved. Please wait...");
                    },
                    success: function(response) {
                        toastr.clear();
                        if (response['status'] === 200) {
                            toastr.success(response['message']);
                        } else if (response['status'] === 500) {
                            toastr.error(response['message']);
                        }
                        showFlashMessages(response['message']);
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        let errMsg = extractErrorMessage(jqXHR);
                        toastr.clear();
                        toastr.error(errMsg);
                        showFlashMessages(errMsg);
                    }
                });
            });
            /* build up a publication transport command object */
            function buildPublicationTC() {
                let id = ${params.id};
                let linkProvider = {
                    "linkType": $('#pubLinkProvider').val(),
                    "pattern": ""
                };
                let link = $('#publicationLink').val();
                let title = $('#title').val();
                let journal = $('#journal').val();
                let affiliation = $('#affiliation').val();
                let synopsis = $('#synopsis').val();
                let year = $('#year').val();
                let month = $('#month').val();
                let day = $('#day').val();
                let volume = $('#volume').val();
                let issue = $('#issue').val();
                let pages = $('#pages').val();
                let opts = $('#authorList option');
                let authors  = $.map(opts, function(opt) {
                    if ($(opt).attr("data-person-id") === "undefined") {
                        return {
                            "userRealName": $(opt).attr("data-person-realname"),
                            "orcid": $(opt).attr("data-person-orcid"),
                            "institution": $(opt).attr("data-person-institution")
                        };
                    } else {
                        return {
                            "id": $(opt).attr("data-person-id"),
                            "userRealName": $(opt).attr("data-person-realname"),
                            "orcid": $(opt).attr("data-person-orcid"),
                            "institution": $(opt).attr("data-person-institution")
                        };
                    }
                });

                let pubCmd = {
                    'id': id,
                    'linkProvider': linkProvider,
                    'link': link,
                    'title': title,
                    'journal': journal,
                    'affiliation': affiliation,
                    'synopsis': synopsis,
                    'year': year,
                    'month': month,
                    'day': day,
                    'volume': volume,
                    'issue': issue,
                    'pages': pages,
                    'authors': authors
                };
                return pubCmd;
            }

            $('input[name="Back"]').on("click", function() {
                // The following function is defined in publicationSubmission.js
                backAway();
            });

            function verifyAndFetchPublicationDetails(pubLinkProvider, pubLink) {
                if (!pubLinkProvider || !pubLink) {
                    toastr.clear();
                    toastr.error("Either of publication provider or link is empty");
                    return;
                }
                $.ajax({
                    type: "POST",
                    url: "${createLink(controller: "publication", action: "fetchPublicationFromPubMedAndRenderPublicationForm")}",
                    data: {
                        pubLinkProvider: pubLinkProvider,
                        pubLink: pubLink,
                        id: ${params.id},
                        pubmed: $('#publicationLink').val(),
                        operation: "edit"
                    }
                }).success(function(res) {
                    validation = res.status !== "Failed";
                    if (!validation) {
                        toastr.error(res.message);
                        showFlashMessages(res.message);
                    } else {
                        let msg = "The publication details have been fetched successfully";
                        toastr.success(msg);
                        showFlashMessages(msg);
                        $('.editablePart').html(res);
                    }
                }).fail(function(jqXHR) {
                    // the method below is defined in helpers.js
                    let msg = extractErrorMessage(jqXHR);
                    toastr.error(msg);
                });
            }
        </g:javascript>
    </g:if>
    <g:else>
        <p>Could not find the publication ${params?.id}</p>
    </g:else>
</body>
</html>
