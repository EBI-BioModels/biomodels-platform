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
<%
    def style = grailsApplication.config.jummp.branding.style
    def serverUrl = grailsApplication.config.grails.serverURL
%>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>Show a publication</title>
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "css/${style}", file: 'publicationPageStyle.css')}" />
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "css", file: 'toastr.min.css')}"/>

    <g:javascript src="toastr.min.js" contextPath=""/>
</head>

<body>
    <h2>Publication Details</h2>
    <g:if test="${publication}">
        <g:render template="/templates/publicationEditorForm"
                  plugin="jummp-plugin-web-application"
                  model="['publication': publication, 'authorListContainerSize': authorListContainerSize]"/>
        <g:javascript>
            $('#btnSave').on("click", function(event) {
                "use strict";
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: $.jummp.createLink("publication", "save"),
                    cache: true,
                    processData: true,
                    async: true,
                    data: {
                        'id': ${params.id}
                    },
                    beforeSend: function() {
                        toastr.info("The publication details are being saved. Please wait...");
                    },
                    success: function(response) {
                        console.log("Success!");
                        var href = window.location.href;
                        if (href.indexOf("&id=") < 0 && typeof(response['id']) != 'undefined') {
                            var newHref = href + "&id=" + response['id'];
                            if (window.history.pushState) {
                                window.history.pushState({}, null, newHref);
                            } else {
                                document.location.hash = newHref;
                            }
                        }
                        toastr.clear();
                        toastr.success(response['message']);
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        toastr.clear();
                        toastr.error("Error: ", jqXHR.responseText + textStatus + errorThrown + JSON.stringify(jqXHR));
                    }
                });
            });
        </g:javascript>
    </g:if>
    <g:else>
        <p>Could not find the publication ${params?.id}</p>
    </g:else>
</body>
</html>
