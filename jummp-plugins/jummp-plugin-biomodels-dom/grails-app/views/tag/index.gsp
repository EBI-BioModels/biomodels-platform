<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-04-05
  Time: 21:12
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>All Tags | BioModels</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <link rel="stylesheet" type="text/css"
          href="https://cdn.datatables.net/1.10.19/css/jquery.dataTables.min.css">
    <style>
        .even.selected td {
            background-color: rgb(10, 158, 170); !important;
            /* Add !important to make sure override datables base styles */
        }

        .odd.selected td {
            background-color: rgb(252, 224, 151); !important;
            /* Add !important to make sure override datables base styles */
        }
    </style>
    <script type="text/javascript" language="javascript"
            src="https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js"></script>
    <script type="application/javascript">
        $(document).ready(function() {
            var table = $('#allTagsTable').DataTable({
                "lengthMenu": [[5, 10, 15, 20, 25, 50, -1], [5, 10, 15, 20, 25, 50, "All"]]
            });
            $('#allTagsTable tbody').on('click', 'tr', function() {
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
</head>

<body>
<div class="row">
    <div class="small-12 medium-12 large-12 columns">
    <h2>All Tags</h2>
    <a class="button" href="${createLink(controller: "tag", action: "add")}">Add a new tag</a>
    <table id="allTagsTable" class="display">
        <thead class="row" style="font-weight: bold">
        <tr>
            <th>Tag Name</th>
            <th>Created By</th>
            <th>Created On</th>
            <th>Modified On</th>
        </tr></thead>
        <tbody>
        <g:render template="/templates/showTag" collection="${tags}" var="tag" />
        </tbody>
    </table>
    </div>
</div>
</body>
</html>
