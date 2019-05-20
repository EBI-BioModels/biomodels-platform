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
  Time: 11:34
--%>

<%@ page contentType="text/html; charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>List of all publications | BioModels</title>
    <style type="text/css">
    .underline {
        border-bottom: 1px solid grey;
    }
    </style>
    <link rel="stylesheet" type="text/css"
          href="https://cdn.datatables.net/1.10.19/css/jquery.dataTables.min.css">
    <script type="text/javascript" language="javascript"
            src="https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js"></script>
    <script type="application/javascript">
        $(document).ready(function() {
            let table = $('#allPublicationsTable').DataTable({
                "lengthMenu": [[5, 10, 15, 20, 25, 50, -1], [5, 10, 15, 20, 25, 50, "All"]]
            });
            $('#allPublicationsTabe tbody').on('click', 'tr', function() {
                if ( $(this).hasClass('selected') ) {
                    $(this).removeClass('selected');
                }
                else {
                    table.$('tr.selected').removeClass('selected');
                    $(this).addClass('selected');
                }
            });
        });
    </script>
</head>
<body>
<div class="row">
    <div class="small-12 medium-12 large-12 columns">
        <h2>List of publications</h2>
        <a class="button" href="${createLink(action: "add")}">Add a new publication</a>
        <table id="allPublicationsTable" class="display">
            <thead class="row" style="font-weight: bold">
            <tr>
                <th>Link</th>
                <th>Title</th>
                <th>Journal</th>
                <th>Affiliation</th>
                <th>Abstract</th>
                <th>Year</th>
                <th>Month</th>
                <th>Day</th>
                <th>Volume</th>
                <th>Issue</th>
                <th>Pages</th>
                <th>Authors</th>
            </tr></thead>
            <tbody>
            <g:render template="/templates/publication/publication" collection="${publications}" var="publication" />
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
