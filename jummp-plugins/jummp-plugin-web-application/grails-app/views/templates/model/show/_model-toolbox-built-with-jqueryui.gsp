<script>
    $(function() {
        // Handler for .ready() called.
        $('#confirm-model-consistency-check').dialog({
            resizable: false,
            autoOpen: false,
            height: 250,
            width: 500,
            modal: true,
            buttons: {
                Confirm: function() {
                    const url = "${g.createLink(controller: 'sbml',
                                        action: 'checkConsistency',
                                        id: revision.identifier())}";
                    $.jummp.openPage(url);
                    $(this).dialog("close");
                },
                Cancel: function() {
                    $(this).dialog("close");
                }
            }
        });

        $('#confirm-model-conversion').dialog({
            resizable: false,
            autoOpen: false,
            height: 250,
            width: 500,
            modal: true,
            buttons: {
                Confirm: function() {
                    let url = "${g.createLink(controller: 'conversion', action: 'convert')}";
                    url += "?id=${revision.model.submissionId}&revisionId=${revision.revisionNumber}"
                    $.jummp.openPage(url);
                    $(this).dialog("close");
                },
                Cancel: function() {
                    $(this).dialog("close");
                }
            }
        });

        $( "#confirm-model-deletion" ).dialog({
            resizable: false,
            autoOpen: false,
            height: 200,
            width: 440,
            modal: true,
            buttons: {
                "Confirm Delete": function() {
                    $.jummp.openPage('${g.createLink(controller: 'model', action: 'delete',
                            id: revision.modelIdentifier())}');
                    $( this ).dialog( "close" );
                },
                Cancel: function() {
                    $( this ).dialog( "close" );
                }
            }
        });

        $('#confirm-model-publish').dialog({
            resizable: false,
            autoOpen: false,
            height: 250,
            width: 500,
            modal: true,
            buttons: {
                Confirm: function() {
                    $.jummp.openPage("${g.createLink(controller: 'model',
                        action: 'publish', id: revision.identifier() )}");
                    $( this ).dialog( "close" );
                },
                Cancel: function() {
                    $( this ).dialog( "close" );
                }
            }
        });

        $('#confirm-model-unpublish').dialog({
            resizable: false,
            autoOpen: false,
            height: 250,
            width: 500,
            modal: true,
            buttons: {
                Confirm: function() {
                    $.jummp.openPage("${g.createLink(controller: 'model',
                        action: 'unpublish', id: revision.identifier() )}");
                    $( this ).dialog( "close" );
                },
                Cancel: function() {
                    $( this ).dialog( "close" );
                }
            }
        });

        $('#confirm-model-notify').dialog({
            resizable: false,
            autoOpen: false,
            height: 250,
            width: 525,
            modal: true,
            buttons: {
                Confirm: function() {
                    $.jummp.openPage("${g.createLink(controller: 'model',
                        action: 'submitForPublication', id: revision.identifier() )}");
                    $( this ).dialog( "close" );
                },
                Cancel: function() {
                    $( this ).dialog( "close" );
                }
            }
        });

        $('#warn-publication-details').dialog({
            resizable: false,
            autoOpen: false,
            height: 540,
            width: 895,
            modal: true,
            buttons: [
                {
                    id: "btnWarningDialogAction",
                    text: "Close",
                    click: function() {
                        doProceedOrLeave($(this));
                    }
                }
            ]
        });

        $("body").append("<div id='modelToolbar' class='collapsibleContainer' title='Model Toolbar'>" +
            "<button title='Expand Toolbar' data-showing='0' id='panelToggle'>Expand</button></div>	");
        $("#buttonContainer").prependTo("#modelToolbar");
        const panelToggle = $("#panelToggle");
        panelToggle.on("click", function (evt){
            displayToolbar(panelToggle.data("showing") === '0', true);
        });
        $( "#download" ).button({
            text:false,
            icons: {
                primary:"ui-icon-arrowthickstop-1-s"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $("#ask-reviewer-account").button({
            text:false,
            icons: {
                primary:"ui-icon-circle-check"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $("#manage-contributors").button({
            text:false,
            icons: {
                primary:"ui-icon-contact"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $("#index-model-revision").button({
            text:false,
            icons: {
                primary:"ui-icon-refresh"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $( "#peer-review" ).button({
            text:false,
            icons: {
                primary:"ui-icon-signal-diag"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $( "#update" ).button({
            text:false,
            icons: {
                primary:"ui-icon-refresh"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $( "#delete" ).button({
            text:false,
            icons: {
                primary:"ui-icon-trash"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $( "#publish" ).button({
            text:false,
            icons: {
                primary:"ui-icon-unlocked"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $( "#unpublish" ).button({
            text:false,
            icons: {
                primary:"ui-icon-locked"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $( "#share" ).button({
            text:false,
            icons: {
                primary:"ui-icon-person"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        /*$("#annotate").button({
            text: false,
            icons: {
                primary: "ui-icon-tag"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });*/
        $("#checkConsistency").button({
            text: false,
            icons: {
                primary: "ui-icon-check"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $("#certify").button({
            text: false,
            icons: {
                primary: "ui-icon-star"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        $("#convert").button({
            text: false,
            icons: {
                primary: "ui-icon-transferthick-e-w"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });

        panelToggle.button({
            text:false,
            icons: {
                primary: "ui-icon-circle-arrow-e"
            }
        }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px', 'float':'right'  });
    });

    function displayToolbar(show, firstTime) {
        if (show) {
            $("#panelToggle").data("showing", '1');
            $(".buttonLabel").show();
            $("#modelToolbar").width("110px");
            $("#panelToggle").button("option", {
                icons: { primary: "ui-icon-circle-arrow-w" }
            });
            $(".toolbutton").button("option", "text", true);
            $(".toolbutton").css({ width: '110px', 'padding-top': '10px', 'padding-bottom': '10px' });
        } else {
            $("#panelToggle").data("showing", '0');
            $(".buttonLabel").hide();
            $("#modelToolbar").width("45px");
            $(".toolbutton").button("option", "text", false);
            if (firstTime) {
                $("#panelToggle").button("option", {
                    icons: { primary: "ui-icon-circle-arrow-e" }
                });
            }
            $(".toolbutton").css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
        }
    }

    /**
     * This function computes a proper link for the download button depending on the size/total size of the model
     * files in the submission in request.
     * @returns {string|void}
     */
    function linkServeOmex() {
        const link = "${createLink(controller: 'model', action: 'download', id: revision.identifier())}";
        $.jummp.openPage(link);
    }

    $('#chkPublishWithoutPublication').on("change", function() {
        let whichButton = '';
        if (this.checked) {
            whichButton = '<span class="ui-button-text">Proceed</span>';
        } else {
            whichButton = '<span class="ui-button-text">Close</span>';
        }
        $('#btnWarningDialogAction').html(whichButton);
    });

    function doProceedOrLeave(pointer) {
        let actionButton = $('#btnWarningDialogAction').text();
        if (actionButton === "Proceed") {
            $.jummp.openPage("${g.createLink(controller: 'model',
                        action: 'submitForPublication', id: revision.identifier())}");
        }
        pointer.dialog("close");
    }

    function manageContributors() {
        $.jummp.openPage('${g.createLink(controller: 'contributor', action: 'manage', id: revision.identifier())}');
    }

    function indexModelRevision() {
        $.jummp.openPage('${g.createLink(controller: 'search', action: 'reindex', params: ["models": [revision.identifier()]])}');
    }
</script>
<ul id='toolbarList'>
    <li>
        <button class='toolbutton' id="download"
                onclick="return linkServeOmex();">Download</button></li>
    <g:if test="${canUpdate}">
        <li>
            <button class='toolbutton' id="update"
                    onclick="return $.jummp.openPage('${g.createLink(controller: 'model',
                            action: 'update',
                            id: revision.modelIdentifier())}')">Update</button>
        </li>
    </g:if>
    <g:if test="${canDelete}">
        <div id="confirm-model-deletion" title="Confirm Delete Model" style="display:none;">
            <p>Are you sure you want to delete (e.g., archive) the model?</p>
        </div>
        <li>
            <button class='toolbutton' id="delete"
                    onclick='return $( "#confirm-model-deletion" ).dialog( "open" );'>Delete</button>
        </li>
    </g:if>
    <g:if test="${canSubmitForPublication}">
        <% def dialog_id %>
        <g:if test="${revision.model.publication}">
            <div id="confirm-model-notify"
                 title="<jummp:renderSubmitForPublicationConfirmDialogTitle/>"
                 style="display:none;">
                <p><jummp:renderSubmitForPublicationConfirmDialogMessage/></p>
            </div>
            <% dialog_id = "confirm-model-notify" %>
        </g:if>
        <g:else>
            <div id="warn-publication-details"
                 title="<jummp:renderSubmitForPublicationWarningDialogTitle/>" style="display: none">
                <p><jummp:renderSubmitForPublicationWarningDialogMessage/></p>
            </div>
            <% dialog_id = "warn-publication-details" %>
        </g:else>
        <li>
            <button class='toolbutton' id="peer-review"
                    title="Submit for publication"
                    onclick='return $("#${dialog_id}").dialog("open");'>
                Publish</button></li>
    </g:if>
    <g:if test="${showPublishOption}">
        <div id="confirm-model-publish" title="You are about to publish this model version"
             style="display:none;">
            <p>Make this version of the model visible to anyone without logging in?</p>
        </div>
        <li>
            <button class='toolbutton' id="publish"
                    onclick="return $( '#confirm-model-publish' ).dialog( 'open' );">Publish</button>
        </li>
    </g:if>
    <g:if test="${showUnpublishOption}">
        <div id="confirm-model-unpublish" title="Confirm!!!" style="display:block">
            <p>You are about to unpublish this model version. Are you sure?</p>
        </div>
        <li>
            <button class='toolbutton' id="unpublish"
                    onclick="return $( '#confirm-model-unpublish' ).dialog( 'open' );">Unpublish</button>
        </li>
    </g:if>
    <g:if test="${canShare}">
        <li>
            <button class='toolbutton' id="share"
                    onclick="return $.jummp.openPage('${g.createLink(controller: 'model',
                            action: 'share', id: revision.identifier())}')">Share</button>
        </li>
    </g:if>
%{--<g:if test="${canUpdate}">
    <li>
        <button class='toolbutton' id='annotate'
                onclick="return $.jummp.openPage('${g.createLink(controller: 'annotation',
                action: 'edit',
                id: revision.modelIdentifier())}')">Annotate</button>
    </li>
</g:if>--}%
    <g:if test="${canCertify}">
        <li>
            <button class='toolbutton' id="certify"
                    onclick="return $.jummp.openPage('${g.createLink(controller: 'qcInfo',
                                action: 'edit',
                        id: revision.modelIdentifier())}')">Certify</button>
        </li>
    </g:if>
    <g:if test="${canCheckConsistency}">
        <div id="confirm-model-consistency-check" title="Model consistency check" style="display:none;">
            <p>Checking model consistency uses an online validator. This might take time for uploading and validating the model. Do you want to proceed the validation?</p>
        </div>
        <li>
            <button id="checkConsistency"
                    class="toolbutton"
                    title="Check consistency"
                    onclick="return $('#confirm-model-consistency-check').dialog('open');">
                Check
            </button>
        </li>
    </g:if>
    <g:if test="${hasCuratorRole && supportedForConversion}">
        <div id="confirm-model-conversion" title="Model Conversion" style="display:none;">
            <p>Exporting this model to other formats uses an online service. This might take time for
            uploading and exporting the model. Do you want to proceed the model conversion?</p>
        </div>
        <li>
            <button id="convert"
                    class="toolbutton"
                    title="Convert This Model To The Other Formats"
                    onclick="return $('#confirm-model-conversion').dialog('open');">
                Convert
            </button>
        </li>
    </g:if>
    <g:if test="${canAskReviewerAccount}">
        <li>
            <button class='toolbutton' id="ask-reviewer-account"
                    title="Click on this button to open a reviewer account for this model"
                    onclick="return $.jummp.openPage('${g.createLink(controller: 'jummp',
                            action: 'createReviewerAccount',
                            id: revision.modelIdentifier())}')">Reviewer</button>
        </li>
    </g:if>
    <g:if test="${canAskReviewerAccount}">
        <!-- canAddContributor is the same canAskReviewerAccount -->
        <li>
            <button class='toolbutton' id="manage-contributors"
                    title="Click on this button to manage the list of contributors of your model"
                    onclick="return manageContributors()">Members</button>
        </li>
    </g:if>
    <g:if test="${hasCuratorRole}"> <!-- canIndex is the same hasCuratorRole -->
        <li>
            <button class='toolbutton' id="index-model-revision"
                    title="Click on this button to reindex your model"
                    onclick="return indexModelRevision()">Index</button>
        </li>
    </g:if>
</ul>
