<%--
  Created by IntelliJ IDEA.
  User: tnguyen@ebi.ac.uk
  Date: 23/06/17
  Time: 15:53
--%>

<%@ page import="net.biomodels.jummp.deployment.biomodels.ModelOfTheMonthTransportCommand"
    contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <link rel="stylesheet" type="text/css"
          href="https://cdn.datatables.net/1.10.19/css/dataTables.foundation.min.css">
    <script type="text/javascript" language="javascript"
            src="https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js"></script>
    <script type="text/javascript" language="javascript"
            src="https://cdn.datatables.net/1.10.19/js/dataTables.foundation.min.js"></script>
    <title>Model of The Month Management</title>
</head>

<body>
    <h2>Model of The Month Management</h2>
    <h3>List all of Model of Month entries</h3>
    <a class="button" href="${createLink(controller: "ModelOfTheMonth", action: "show")}">Add a new entry</a>
    <table id="momEntries">
    <thead class="row" style="font-weight: bold">
    <tr>
        <th class="small-1 columns">Authors</th>
        <th class="small-2 columns">Title</th>
        <th class="small-4 columns">Short description</th>
        <th class="small-1 columns">Publication Date</th>
        <th class="small-1 columns">Last updated</th>
        <th class="small-1 columns">Models</th>
    </tr></thead>
    <tbody>
    <g:render template="/templates/momEntry" collection="${entries}" var="entry" />
    </tbody>
    </table>
    <script type="application/javascript">
        $(document).ready(function() {
            $('#momEntries').DataTable({
                "lengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]],
                // "columns": [
                //     null, null, null, null, null, {"visible": false}, null
                // ]
                // "columns": [
                //     { "name": "authors" },
                //     { "name": "title" },
                //     { "name": "shortDescription" },
                //     { "name": "publicationDate" },
                //     { "name": "lastUpdated" },
                //     { "name": "models" }
                // ]
            });
        });
    </script>
    <g:render template="/templates/updatePIandSD" />
</body>
</html>
