<%--
  Created by IntelliJ IDEA.
  User: sarala
  Date: 08/07/2016
  Time: 09:44
--%>

<g:applyLayout name="main">
<head>
    <title>Certification</title>
    <style type="text/css">
        textarea {
            width: 500px;
            height: 200px;
        }
        .rating {
            unicode-bidi: bidi-override;
            direction: rtl;
            font-size: large;
        }
        .rating > span {
            display: inline-block;
            position: relative;
            width: 1.1em;
        }
        .rating > span:hover:before,
        .rating > span:hover ~ span:before {
            text-shadow: 0 0 2px rgba(0,0,0,0.7);
            color: #FDE16D;
            content: '\2605'; /* Full star in UTF-8 */
            position: absolute;
            left: 0;
        }
        .star-icon {
            color: #dddddd;
            font-size: 2em;
            position: relative;
        }
        .star-icon.full:before {
            text-shadow: 0 0 2px rgba(0,0,0,0.7);
            color: #FDE16D;
            content: '\2605'; /* Full star in UTF-8 */
            position: absolute;
            left: 0;
        }
        .star-icon.half:before {
            text-shadow: 0 0 2px rgba(0,0,0,0.7);
            color: #FDE16D;
            content: '\2605'; /* Full star in UTF-8 */
            position: absolute;
            left: 0;
            width: 50%;
            overflow: hidden;
        }
        @-moz-document url-prefix() { /* Firefox Hack :( */
            .star-icon {
                font-size: 50px;
                line-height: 34px;
                cursor: default;
            }
        }
    </style>
</head>

<body>
    <h1>Certify Model ${revision.name} (${revision.model.publicationId ?: revision.model.submissionId})</h1>

    <div><h2 id="message"></h2></div>

    <div>
        <table>
            <tbody>
                <jummp:renderCertificationForm />
            <tr>
                <td style="width: 25%; text-align: right; vertical-align: top">
                    <label><g:message code="jummp.certification.comment.label"/>:</label></td>
                <td><g:textArea id="comment" name="comment"/></td>
            </tr>
            <tr>
                <td>&nbsp;</td>
                <td style="text-align: center; vertical-align: middle">
                    <div class="buttons">
                        <button id="certifyButton" title="Certifying model" class="action">Certify</button>
                        <button id="cancelButton" title="Cancel" class="action"
                                onclick="return $.jummp.openPage('${g.createLink(controller: 'model', action: 'show', id: modelId)}')">Cancel</button>
                    </div>
                </td>
            </tr>
            </tbody>
        </table>
    </div>

    <g:javascript>
        $('textarea').resizable();
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
        $('span[id^=star]').on('click', function() {
            console.log(this.id);
            if (this.id == 'star3') {
                var currentClass = $('#star3').attr('class');
                if (currentClass == 'star-icon') {
                    $('#star3').attr('class', 'star-icon full');
                    $('#certifyLevel').val(1);
                }
                else {
                    $('#star3').attr('class', 'star-icon');
                    $('#certifyLevel').val(0);
                }
                $('#star2').attr('class', 'star-icon');
                $('#star1').attr('class', 'star-icon');
            } else if (this.id == 'star1') {
                $('#star3').attr('class', 'star-icon full');
                $('#star2').attr('class', 'star-icon full');
                $('#star1').attr('class', 'star-icon full');
                $('#certifyLevel').val(3);
            } else if (this.id == 'star2') {
                $('#star3').attr('class', 'star-icon full');
                $('#star2').attr('class', 'star-icon full');
                $('#star1').attr('class', 'star-icon');
                $('#certifyLevel').val(2);
            }
        });
    </g:javascript>
</body>
</g:applyLayout>
