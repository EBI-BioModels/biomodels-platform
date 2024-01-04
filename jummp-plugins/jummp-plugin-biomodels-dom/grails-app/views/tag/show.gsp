<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-04-05
  Time: 21:28
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Tag show: ${tag.name} | BioModels</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <g:render template="/templates/head" plugin="jummp-plugin-web-application" />
</head>

<body>
<div class="row">
    <div class="small-12 medium-6 medium-centered large-6 large-centered columns">
        <h2>Update a tag</h2>
    </div>
</div>
<g:render template="/templates/tag/tagForm" plugin="jummp-plugin-biomodels-dom" />
<g:javascript>
    $('#dateCreated').datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(datetext) {
            datetext = datetext + getTimeStamp();
            $('#datepicker').val(datetext);
            $(this).val(datetext);
        }
    });
    $('#dateModified').datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(datetext) {
            datetext = datetext + getTimeStamp();
            $('#datepicker').val(datetext);
            $(this).val(datetext);
        }
    });
    /*$('#btnSave').click(function(event) {
        event.preventDefault();
        console.log("Saved!");
        $.ajax({
            type: "POST",
            cache: true,
            url: $.jummp.createLink("tag", "update"),
            beforeSend: function() {
                toastr.info("The tag is being saved into our database. Please wait...");
            },
            success: function(data, txtStatus, jqXHR) {
                var msg = data.message;
                toastr.clear();
                toastr.success(msg);
            },
            error: function(data, jqXHR, exception, errorThrown) {
                var msg = data.responseJSON.message;
                var statusCode = data.status;
            }
        });
    });*/
</g:javascript>
</body>
</html>
