<%--
  Created by IntelliJ IDEA.
  User: sarala
  Date: 08/07/2016
  Time: 09:44
--%>

<g:applyLayout name="main">
<head>
    <title>Certification</title>
</head>

<body>
    <h1>Certify Model ${revision.name} (${revision.model.publicationId ?: revision.model.submissionId})</h1>

    <div><h2 id="message"></h2></div>

    <div>
        <table>
            <tbody>
            <jummp:renderCertificationForm />
            <tr>
                <td class='tableLabels'><label><g:message code="certify.comment"/>:</label></td>
                <td><g:textArea id="comment" name="comment"/></td>
            </tr>
            </tbody>
        </table>
        <div class="buttons">
            <button id="certifyButton" title="Certifing model" class="action">Certify</button>
            <button id="cancelButton" title="Cancel" class="action"
                    onclick="return $.jummp.openPage('${g.createLink(controller: 'model', action: 'show', id: modelId)}')">Cancel</button>
        </div>
    </div>

    <g:javascript>
        $('#certifyButton').button({
            icons: {
                primary: "ui-icon-star"
            }
        }).on("click", function(event) {
        "use strict";
        event.preventDefault();
        $.ajax({
            dataType: "json",
            type: "GET",
            url: $.jummp.createLink("qcInfo", "certify"),
            cache: false,
            data: {
                flag: $('#certifyLevel').val(),
                comment: $('#comment').val(),
                modelId: "${modelId}",
                revision: "${revision.id}"
            },
            error: function(jqXHR) {
                console.error("epic fail", jqXHR.responseText);
                $("#message").removeClass("success");
                $("#message").removeClass("jummpWarning");
                $("#message").addClass("failure");
                $('#message').html("There was an internal error while certifing the information provided.");
            },
            success: function(response) {
                if (response.status == "200") {
                    $.jummp.openPage('${g.createLink(controller: 'model', action: 'showWithMessage',
                            id: modelId, revisionId: revision, params: [flashMessage: "Model has been certified."])}')
                } else {
                    $("#message").addClass("failure");
                    $('#message').html(response.message);
                }

            }
        });
    });
        $('#cancelButton').button({
        icons: {
            primary: "ui-icon-close"
        }
    });
    </g:javascript>
</body>
</g:applyLayout>
