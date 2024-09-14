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

<%
    def contextHelpLocation=g.pageProperty(name:'page.contexthelp')
    if (contextHelpLocation) {
        contextHelpLocation=contextHelpLocation.trim()
    }
    else {
        contextHelpLocation="manual"
    }
    int helpWidth = 800;

    def styleName = grailsApplication.config.jummp.branding.style

    response.setHeader("Cache-Control","no-cache");
    response.setHeader("Cache-Control","no-store");
    response.setDateHeader("Expires", 0);
    response.setHeader("Pragma","no-cache");
%>

<!doctype html>
<g:render template="/templates/${styleName}/precursor" />
<html lang="en">
<head>
    <g:render template="/templates/${styleName}/head" />
    <g:render template="/templates/${styleName}/setup-contextual-help" />
    <g:javascript src="jummp.js"/>
    <g:javascript src="notification.js"/>

    <link rel="stylesheet" href="<g:resource dir="css" file="notification.css"/>" />
    <link rel="stylesheet" href="<g:resource dir="css/biomodels" file="layout.css"/>" />
    <link rel="stylesheet" href="<g:resource dir="css/biomodels" file="biomodels.css"/>" />
    <link rel="stylesheet" href="<g:resource dir="css/jqueryui/smoothness" file="jquery-ui-1.10.3.custom.min.css"/>" />
    <title>BioModels</title>
    <g:layoutHead/>
</head>
<!-- open body tag -->
<%
    def sidebarContent = g.pageProperty(name:'page.sidebar')
    if (sidebarContent) {
        sidebarContent = sidebarContent.trim()
    }
%>
<body class="level2 full-width">
<div id="mainframe">
    <g:render template="/templates/${styleName}/header"/>
    <g:render template="/templates/${styleName}/mainbody"/>
    <g:render template="/templates/${styleName}/footer"/>

    <g:if test="${contextHelpLocation}">
        <div id="helpbutton">
            <a id="toggleHelp" title="Access help for this page" href="#">More about this page</a>
        </div>
        <div id="helpPanel">
            <div id="toolbar" class="ui-widget-header ui-corner-all">
                <button id="expand">Increase help size</button>
                <button id="contract">Decrease help size</button>
                <button id="snap">Reset help</button>
                <button id="outlink">Open in a new tab</button>
                <button id="close">Close</button>
            </div>
            <ContextHelp:getLink location="${contextHelpLocation}" width="${helpWidth}"/>
        </div>
    </g:if>
</div>
<g:render template="/templates/${styleName}/loadjs"/>
</body>
</html>
