<%@ page import="grails.util.Environment" %>
<%--
 Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
    <g:if test="${Environment.current == Environment.DEVELOPMENT}">
        %{-- preempt DNS lookup so that it doesn't block the loading of EBI Visual Framework assets on local dev --}%
        <link rel="dns-prefetch" href="https://www.ebi.ac.uk/" />
    </g:if>
    <link rel="dns-prefetch" href="https://ebi.emblstatic.net" />
    <title>${command.isDefaultQuery() ? '' : "${command.query} | "}BioModels Parameters | BioModels</title>
    <link rel="stylesheet"
          href="${resource(dir: 'css/', file: 'jquery.dataTables.min.css',
              contextPath: "${grailsApplication.config.grails.serverURL}")}"
          type="text/css">
    <link rel="stylesheet"
          href="${resource(dir: 'css/datatable', file: 'buttons.dataTables.min.css',
              contextPath: "${grailsApplication.config.grails.serverURL}")}"
          type="text/css"/>

    <link rel="stylesheet"
          href="${resource(dir: 'css', file: 'biomodels-parameters.css')}"
          type="text/css">
    <g:javascript src="datatable/jquery.dataTables.min.js" contextPath=""/>
    <g:javascript src="datatable/dataTables.buttons.min.js" contextPath=""/>
</head>

<body>
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

    <!-- How to cite BioModels Parameters -->
    <h2>Reference</h2>
    <p>If you use BioModels Parameters, please cite:</p>
    <p>
        <span style="font-weight: bold">Mihai Glont, Chinmay Arankalle, Krishna Tiwari, Tung V N Nguyen, Henning
        Hermjakob,
        Rahuman S Malik
        Sheriff<br/>
        BioModels Parameters: a treasure trove of parameter values from published systems biology models
        </span><br/>
        <span style="font-style: italic">Bioinformatics, 2020, btaa560,
            <a href="https://doi.org/10.1093/bioinformatics/btaa560">https://doi.org/10.1093/bioinformatics/btaa560</a>
        </span>
    </p>
</div>
</body>
</html>
<content tag="bpsearch">
    selected
</content>
