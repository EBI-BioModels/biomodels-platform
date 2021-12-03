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
          href="https://cdn.datatables.net/1.10.19/css/jquery.dataTables.min.css">
    <style>
        .even.selected td {
            background-color: rgb(10, 158, 170); !important; /* Add !important to make sure override datables base styles */
        }

        .odd.selected td {
            background-color: rgb(252, 224, 151); !important; /* Add !important to make sure override datables base styles */
        }
    </style>
    <script type="text/javascript" language="javascript"
            src="https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js"></script>
    <script type="application/javascript">
        $(document).ready(function() {
            var table = $('#momEntries').DataTable({
                "lengthMenu": [[3, 5, 10, 15, 20, 25, 50, -1], [3, 5, 10, 15, 20, 25, 50, "All"]]
            });
            $('#momEntries tbody').on('click', 'tr', function() {
                if ( $(this).hasClass('selected') ) {
                    $(this).removeClass('selected');
                }
                else {
                    table.$('tr.selected').removeClass('selected');
                    $(this).addClass('selected');
                }
            });

            $('#btnDeleteRow').click(function() {
                // delete the current row to the view
                table.row('.selected').remove().draw(false);
                // delete that record from the database
                $.ajax({

                });
            });
        });
    </script>
    <title>Model of The Month Management | BioModels</title>
</head>

<body>
    <h2>Model of The Month Management</h2>
    <h3>List all of Model of Month entries</h3>
    <a class="button" href="${createLink(controller: "ModelOfTheMonth", action: "create")}">Add a new entry</a>
    <table id="momEntries" class="display">
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
</body>
</html>
