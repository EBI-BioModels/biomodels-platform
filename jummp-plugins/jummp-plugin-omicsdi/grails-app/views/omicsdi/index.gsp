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
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title>OmicsDI Settings</title>
    <style>
        .editor {
            margin-left: 10px;
            width: 900px;
        }
        #schemaXmlContent {
            resize: both;
        }
    </style>
</head>

<body>
    <h1>OmicsDI Settings</h1>
    <p>Below is the content of OmicsDI Schema XML file. You can change and update it.</p>
    <div id="OmicsDISchemaXMLeditor" class="editor">
        <g:textArea id="schemaXmlContent" name="omicsdiSchemaXML" cols="100" rows="15" value="${nbmodels}">
        </g:textArea>
        <br/>
        <g:field type="button" name="saveButton" value="Save"></g:field>
        <g:field type="button" name="resetButton" value="Reset"></g:field>
        <g:field type="button" name="generateButton" value="Generate OmicsDI's entries"></g:field>
    </div>
</body>
</html>
