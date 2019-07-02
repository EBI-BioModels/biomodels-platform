<%--
 Copyright (C) 2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="${session['branding.style']}/main"/>

    <title>BioModels Parameters | BioModels</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/pretty-checkbox@3.0/dist/pretty-checkbox.min.css"/>
    <link rel="stylesheet"
          href="${resource(dir: 'css', file: 'toastr.min.css',
              contextPath: "${grailsApplication.config.grails.serverURL}")}"/>
    <link rel="stylesheet"
          href="${resource(dir: 'css/', file: 'jquery.dataTables.min.css',
              contextPath: "${grailsApplication.config.grails.serverURL}")}"
          type="text/css">
    <link rel="stylesheet"
          href="${resource(dir: 'css/datatable', file: 'buttons.dataTables.min.css',
              contextPath: "${grailsApplication.config.grails.serverURL}")}"
          type="text/css">
    <link rel="stylesheet"
          href="${resource(dir: 'css/datatable', file: 'select.dataTables.min.css',
              contextPath: "${grailsApplication.config.grails.serverURL}")}"
          type="text/css">
    <link rel="stylesheet"
          href="${resource(dir: 'css', file: 'easy-autocomplete.min.css')}"
          type="text/css">
    <link rel="stylesheet"
          href="${resource(dir: 'css', file: 'common.css')}"
          type="text/css">
    <link rel="stylesheet"
          href="${resource(dir: 'css', file: 'easy-autocomplete.themes.min.css')}"
          type="text/css">
</head>

<body>
<g:javascript contextPath="" src="toastr.min.js"/>
<g:javascript src="datatable/jquery.dataTables.min.js" contextPath=""/>
<g:javascript src="datatable/dataTables.buttons.min.js" contextPath=""/>


<div id="remote" class="body">
    <h2>BioModels Parameters</h2>

    <g:render template="/templates/bpSearchContent"
              plugin="jummp-plugin-biomodels-dom"/>

    <g:hasErrors>
        <div class="warning">
            <g:renderErrors/>
        </div>
    </g:hasErrors>
    <g:if test="${flash.message}">
        <div class="warning">
            ${flash.message}
        </div>
    </g:if>
    <g:render template="/templates/bpSearchDataTable"
              plugin="jummp-plugin-biomodels-dom"/>
</div>
</body>
</html>
