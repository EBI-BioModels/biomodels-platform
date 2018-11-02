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
    <ul class="tabs" data-tabs id="momManagementTabs">
        <li class="tabs-title is-active" role="presentation"><a href="#listAll" aria-selected="true">All MoM entries</a></li>
        <li class="tabs-title" role="presentation"><a href="#importFrom">Import from...</a></li>
    </ul>
    <div class="tabs-content" data-tabs-content="momManagementTabs">
        <div class="tabs-panel is-active" id="listAll">
            <h3>List all of Model of Month entries</h3>
            <a class="button" href="${createLink(controller: "ModelOfTheMonth", action: "create")}">Add a new entry</a>
            <table id="momEntries">
                <thead class="row" style="font-weight: bold">
                <tr>
                    <th>Authors</th>
                    <th>Title</th>
                    <th>Short description</th>
                    <th>Publication Date</th>
                    <th>Last updated</th>
                    <th>Models</th>
                </tr></thead>
                <tbody>
                <g:render template="/templates/momEntry" collection="${entries}" var="entry" />
                </tbody>
            </table>
            <script type="application/javascript">
                $(document).ready(function() {
                    $('#momEntries').DataTable({
                        "lengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]]
                    });
                });
            </script>
        </div>
        <div class="tabs-panel" id="importFrom">
            <g:render template="/templates/updatePIandSD" />
        </div>
    </div>
</body>
</html>
