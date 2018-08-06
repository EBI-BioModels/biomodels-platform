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
<html>
    <head>
        <title>Gene Ontology Tree</title>
        <meta name="layout" content="${session['branding.style']}/main" />
        <link rel="stylesheet" href="${resource(dir:'css/dynatree', file:'ui.dynatree.css')}" type="text/css"/>
        <g:javascript src="js/showModels.js"/>
        <g:javascript src="js/gotree.js"/>
    </head>
    <body activetab="search">
        <div class="ui-widget">
            <table>
            <tr>
                <td><label for="gotree-filter">Filter Go Tree:</label></td>
                <td><input id="gotree-filter"/></td>
            </tr>
            </table>
        </div>
        <div id="gotree"></div>
        <g:javascript>
$(function() {
    $.jummp.gotree.load();
});
        </g:javascript>
    </body>
    <content tag="sidebar">
        <div class="element">
            <h1>Gene Ontology Relationships</h1>
            <h2></h2>
            <table>
                <tbody>
                    <tr>
                        <td><r:img dir="css/dynatree" file="go_isa.gif"/></td>
                        <td>is a</td>
                    </tr>
                    <tr>
                        <td><r:img dir="css/dynatree" file="go_partof.gif"/></td>
                        <td>part of</td>
                    </tr>
                    <tr>
                        <td><r:img dir="css/dynatree" file="go_devfrom.gif"/></td>
                        <td>develops from</td>
                    </tr>
                    <tr>
                        <td><r:img dir="css/dynatree" file="go_other.gif"/></td>
                        <td>other</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </content>
</html>
