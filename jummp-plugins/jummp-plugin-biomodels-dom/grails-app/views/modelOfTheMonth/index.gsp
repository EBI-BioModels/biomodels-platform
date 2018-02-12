<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 23/06/17
  Time: 15:53
--%>

<%@ page import="net.biomodels.jummp.deployment.biomodels.ModelOfTheMonthTransportCommand" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>Update Preview Image and Short Description</title>
</head>

<body>
    <h2>Model of The Month Management</h2>
    <div>
        <h3>List all of Model of Month entries</h3>

        <div class="row" style="font-weight: bold">
            <div class="small-2 columns">Authors</div>
            <div class="small-2 columns">Title</div>
            <div class="small-4 columns">Short description</div>
            <div class="small-2 columns">Preview image</div>
            <div class="small-2 columns">Last updated</div>
        </div>
        <g:render template="/templates/momEntry" collection="${entries}" var="entry" />
        <ul class="pagination text-center" role="navigation" aria-label="Pagination">
            <li class="pagination-previous disabled">Previous</li>
            <li class="current"><span class="show-for-sr">You're on page</span> 1</li>
            <li><a href="#" aria-label="Page 2">2</a></li>
            <li><a href="#" aria-label="Page 3">3</a></li>
            <li><a href="#" aria-label="Page 4">4</a></li>
            <li class="ellipsis"></li>
            <li><a href="#" aria-label="Page 12">12</a></li>
            <li><a href="#" aria-label="Page 13">13</a></li>
            <li class="pagination-next"><a href="#" aria-label="Next page">Next</a></li>
        </ul>
    </div>
    <div>
        <g:render template="/templates/updatePIandSD" />
    </div>
</body>
</html>
