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









<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="${session['branding.style']}/main" />
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/pretty-checkbox@3.0/dist/pretty-checkbox.min.css"/>

        <title>Configuration - ${title}</title>
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'toastr.min.css', contextPath: "${grailsApplication.config.grails.serverURL}")}"/>
        <style>
        .alert {
            padding: 15px;
            margin-bottom: 20px;
            border: 1px solid transparent;
            border-radius: 4px;
        }
        .alert h4 {
            margin-top: 0;
            color: inherit;
        }
        .alert .alert-link {
            font-weight: bold;
        }
        .alert > p,
        .alert > ul {
            margin-bottom: 0;
        }
        .alert > p + p {
            margin-top: 5px;
        }
        .alert-success {
            background-color: #dff0d8;
            border-color: #d6e9c6;
            color: #468847;
        }
        .alert-success hr {
            border-top-color: #c9e2b3;
        }
        .alert-success .alert-link {
            color: #356635;
        }
        .alert-danger {
            background-color: #f2dede;
            border-color: #eed3d7;
            color: #b94a48;
        }
        .alert-danger hr {
            border-top-color: #e6c1c7;
        }
        .alert-danger .alert-link {
            color: #953b39;
        }
        </style>
    </head>
    <body>
        <g:javascript contextPath="" src="toastr.min.js"/>
        <g:hasErrors>
            <div class="alert alert-danger">
                <g:renderErrors/>
            </div>
        </g:hasErrors>
        <g:if test="${flash.message}">
            <div class="alert alert-success">
                <strong>${flash.message}</strong>
            </div>
        </g:if>
        <div id="remote" class="body">
            <h2>Configuration - ${title}</h2>
            <g:form name="configurationForm" action="${action}">
                <g:render template="/templates/configuration/${template}"/>
                <div class="buttons">
                    <button type="reset" id="cancelButton" class="button">Cancel</button>
                    <button type="submit" id="submitButton" class="button">Save</button>
                    <g:if test="${controllerName == 'classifierConfigure' && actionName == 'classifier'}">
                        <button type="button" id="createButton" class="button">New train</button>
                    </g:if>
                </div>
            </g:form>
        </div>
    </body>
    <g:render template="/templates/configuration/configurationSidebar"/>
</html>
