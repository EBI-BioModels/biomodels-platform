<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-05-17
  Time: 20:09
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.model.PublicationLinkProvider; grails.converters.JSON;" %>
<%
    def style = grailsApplication.config.jummp.branding.style
    def serverUrl = grailsApplication.config.grails.serverURL
%>
<html>
<head>
    <meta name="layout" content="biomodels/main" />
    <title>${title}</title>
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "/css/${style}", file: 'publicationPageStyle.css')}" />
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "css", file: 'toastr.min.css')}"/>

    <g:javascript>
        let authorMap = {"authors": []};
        let authorList = [];
    </g:javascript>
    <g:javascript contextPath="" src="${style}/publicationSubmission.js"/>
    <g:javascript src="toastr.min.js" contextPath=""/>
    <g:javascript src="helpers.js" contextPath=""/>
    <g:javascript>
        toastr.options = {
            "closeButton": false,
            "debug": false,
            "newestOnTop": false,
            "progressBar": true,
            "positionClass": "toast-top-right",
            "preventDuplicates": false,
            "onclick": null,
            "showDuration": "300",
            "hideDuration": "1000",
            "timeOut": "5000",
            "extendedTimeOut": "1000",
            "showEasing": "swing",
            "hideEasing": "linear",
            "showMethod": "fadeIn",
            "hideMethod": "fadeOut"
        }
    </g:javascript>
</head>

<body>
    <div class="row">
        <h2>Add a new publication</h2>

        <g:render template="/templates/publication/selectPublicationSource"
                  plugin="jummp-plugin-web-application"/>
        <div id="publicationForm">
            <g:render template="/templates/publication/publicationEditableElements"
                      plugin="jummp-plugin-web-application"
                      model="['publication': publication, 'authorListContainerSize': authorListContainerSize]"/>
            <g:render template="/templates/publication/publicationButtonsForm"
                      plugin="jummp-plugin-web-application"/>
        </div>
    </div>
    <g:javascript>
        $('#btnSave').on("click", function(event) {
            "use strict";
            event.preventDefault();
            // validate the form
            if ($('#pubLinkProvider').val() === "NoPub") {
                toastr.clear();
                toastr.error("Cannot save the publication without choosing a type of publication resource");
                return;
            }
            validation = validation && validateDataForm('publicationForm');
            if (!validation) {
                let msg = "The publication source and link do not match. Please verify these values and try again";
                toastr.clear();
                toastr.error(msg);
                showFlashMessages(msg);
                return;
            }
            $.ajax({
                type: "POST",
                url: $.jummp.createLink("publication", "save"),
                cache: true,
                processData: true,
                async: true,
                data: JSON.stringify(buildPublicationTC()),
                contentType: "application/json",
                beforeSend: function() {
                    toastr.info("The publication details are being saved. Please wait...");
                },
                success: function(response) {
                    toastr.clear();
                    if (response['status'] === 200) {
                        toastr.success(response['message']);
                        setTimeout(function() {
        window.location.href = "${createLink(controller: "publication", action: "show")}"+"/" + response['publicationId'];
                        }, 5200); // 5200 is equivalent to toastr.options.timeOut due to toastr.success

                    } else if (response['status'] === 500) {
                        toastr.error(response['message']);
                    }
                    showFlashMessages(response['message']);
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    let errMsg = extractErrorMessage(jqXHR);
                    toastr.clear();
                    toastr.error(errMsg);
                    showFlashMessages(errMsg);
                }
            });
        });
        /* build up a publication transport command object */
        function buildPublicationTC() {
            let linkProvider = {
                "linkType": $('#pubLinkProvider').val(),
                "pattern": ""
            };
            let link = $('#publicationLink').val();
            let title = $('#title').val();
            let journal = $('#journal').val();
            let affiliation = $('#affiliation').val();
            let synopsis = $('#synopsis').val();
            let year = $('#year').val();
            let month = $('#month').val();
            let day = $('#day').val();
            let volume = $('#volume').val();
            let issue = $('#issue').val();
            let pages = $('#pages').val();
            let opts = $('#authorList option');
            let authors  = $.map(opts, function(opt) {
                if ($(opt).attr("data-person-id") === "undefined") {
                    return {
                        "userRealName": $(opt).attr("data-person-realname"),
                        "orcid": $(opt).attr("data-person-orcid"),
                        "institution": $(opt).attr("data-person-institution")
                    };
                } else {
                    return {
                        "id": $(opt).attr("data-person-id"),
                        "userRealName": $(opt).attr("data-person-realname"),
                        "orcid": $(opt).attr("data-person-orcid"),
                        "institution": $(opt).attr("data-person-institution")
                    };
                }
            });

            let pubCmd = {
                'linkProvider': linkProvider,
                'link': link,
                'title': title,
                'journal': journal,
                'affiliation': affiliation,
                'synopsis': synopsis,
                'year': year,
                'month': month,
                'day': day,
                'volume': volume,
                'issue': issue,
                'pages': pages,
                'authors': authors
            };
            return pubCmd;
        }

        $('input[name="Back"]').on("click", function() {
            // The following function is defined in publicationSubmission.js
            backAway();
        });

        function verifyAndFetchPublicationDetails(pubLinkProvider, pubLink) {
            if (!pubLinkProvider || !pubLink) {
                toastr.clear();
                toastr.error("Either of publication provider or link is empty");
                return;
            }
            $.ajax({
                type: "POST",
                url: "${createLink(controller: "publication", action: "fetchPublicationFromPubMedAndRenderPublicationForm")}",
                data: {
                    pubLinkProvider: pubLinkProvider,
                    pubLink: pubLink,
                    operation: "add"
                }
            }).done(function(res) {
                validation = res.status !== "Failed";
                if (!validation) {
                    toastr.error(res.message);
                    showFlashMessages(res.message);
                } else {
                    let msg = "The publication details have been fetched successfully";
                    if (res.status === 302) {
                        msg = "${g.message(code: "publication.editor.duplicateEntry.message")}";
                        toastr.warning(msg);
                    } else {
                        toastr.success(msg);
                    }
                    showFlashMessages(msg);
                    $('.editablePart').html(res);
                }
            }).fail(function(jqXHR) {
                // the method below is defined in helpers.js
                let msg = extractErrorMessage(jqXHR);
                toastr.error(msg);
            });
        };
    </g:javascript>
</body>
</html>
