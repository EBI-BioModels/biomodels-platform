<div id="model-toolbox" class="sidenav">
    <a href="javascript:void(0)" class="closebtn" onclick="closeNav()">&times;</a>
    <a href="javascript:void(0)" onclick="linkServeOmex()">Download</a>
    <g:if test="${canUpdate}">
        <a href="javascript:void(0)" onclick="update()">Update</a>
    </g:if>
    <g:if test="${canDelete}">
        <a href="javascript:void(0)" onclick="archive()">Delete</a>
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
        <a class='toolbutton' id="peer-review"
           title="Submit for publication"
           onclick='return $("#${dialog_id}").dialog("open");'>
            Publish</a>
    </g:if>
    <g:if test="${showPublishOption}">
        <div id="confirm-model-publish" title="You are about to publish this model version"
             style="display:none;">
            <p>Make this version of the model visible to anyone without logging in?</p>
        </div>

        <a class='toolbutton' id="publish"
           onclick="return $('#confirm-model-publish').dialog('open');">Publish</a>

    </g:if>
    <g:if test="${showUnpublishOption}">
        <div id="confirm-model-unpublish" title="Confirm!!!" style="display:none">
            <p>You are about to unpublish this model version. Are you sure?</p>
        </div>
        <a id="unpublish"
           onclick="return $('#confirm-model-unpublish').dialog('open');">Unpublish</a>
    </g:if>
    <g:if test="${canShare}">
        <a class='toolbutton' id="share"
           onclick="return $.jummp.openPage('${g.createLink(controller: 'model',
                            action: 'share', id: revision.identifier())}')">Share</a>
    </g:if>
    <g:if test="${canCertify}">
        <a class='toolbutton' id="certify"
           onclick="return $.jummp.openPage('${g.createLink(controller: 'qcInfo', action: 'edit',
                        id: revision.modelIdentifier())}')">Certify</a>
    </g:if>
    <g:if test="${canCheckConsistency}">
        <div id="confirm-model-consistency-check" title="Model consistency check" style="display:none;">
            <p>Checking model consistency uses an online validator. This might take time for uploading and validating the model. Do you want to proceed the validation?</p>
        </div>
        <a id="checkConsistency"
                class="toolbutton"
                title="Check consistency"
                onclick="return $('#confirm-model-consistency-check').dialog('open');">
            Check
        </a>
    </g:if>
    <g:if test="${hasCuratorRole && supportedForConversion}">
%{--        <div id="confirm-model-conversion" title="Model Conversion" style="display:none;">--}%
%{--            <p>Exporting this model to other formats uses an online service. This might take time for--}%
%{--            uploading and exporting the model. Do you want to proceed the model conversion?</p>--}%
%{--        </div>--}%
        <a id="convert"
                class="toolbutton"
                title="Convert This Model To The Other Formats"
                onclick="return $('#confirm-model-conversion').dialog('open');">
            Convert
        </a>
    </g:if>
    <g:if test="${canAskReviewerAccount}">
        <a href="javascript:void(0)" onclick="openReviewerAccount()"
           title="Click on this button to open a reviewer account for this model">Reviewer</a>
    </g:if>
    <g:if test="${canAskReviewerAccount}">
        <!-- canAddContributor is the same canAskReviewerAccount -->
        <a href="javascript:void(0)" onclick="manageContributors()"
           title="Click on this button to manage the list of contributors of your model">Members</a>
    </g:if>
    <g:if test="${hasCuratorRole}"><!-- canIndex is the same hasCuratorRole -->
        <a href="javascript:void(0)" onclick="indexModelRevision()"
           title="Click on this button to reindex your model">Index</a>
    </g:if>
</div>
<span style="font-size:20px;cursor:pointer" onclick="openNav()">&#9776; Model ToolBox</span>
<style>

.sidenav {
    height: 50%;
    width: 0;
    position: fixed;
    z-index: 1;
    top: 0;
    left: 0;
    background-color: rgb(0, 124, 130);
    overflow-x: hidden;
    transition: 0.5s;
    padding-top: 60px;
}

.sidenav a {
    padding: 8px 8px 8px 32px;
    text-decoration: none;
    font-size: 25px;
    color: #818181;
    display: block;
    transition: 0.3s;
}

.sidenav a:hover {
    color: #f1f1f1;
}

.sidenav .closebtn {
    position: absolute;
    top: 0;
    right: 25px;
    font-size: 36px;
    margin-left: 50px;
}

@media screen and (max-height: 450px) {
    .sidenav {
        padding-top: 15px;
    }

    .sidenav a {
        font-size: 18px;
    }
}
</style>
<script>
    $("#model-toolbox").on("mouseleave", function() {
        /*$("#model-toolbox").animate({
            display: "none"
        });*/
        closeNav();
    });

    function openNav() {
        document.getElementById("model-toolbox").style.width = "250px";
    }

    function closeNav() {
        document.getElementById("model-toolbox").style.width = "0";
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

    function update() {
        $.jummp.openPage("${g.createLink(controller: 'model', action: 'update', id: revision.modelIdentifier())}")
    }

    function archive() {
        $.jummp.openPage('${g.createLink(controller: 'model', action: 'delete', id: revision.modelIdentifier())}');
    }

    function manageContributors() {
        $.jummp.openPage('${g.createLink(controller: 'contributor', action: 'manage', id: revision.identifier())}');
    }

    function indexModelRevision() {
        $.jummp.openPage('${g.createLink(controller: 'search', action: 'reindex', params: ["models": [revision.identifier()]])}');
    }

    function openReviewerAccount() {
        $.jummp.openPage('${g.createLink(controller: 'jummp', action: 'createReviewerAccount', id: revision.modelIdentifier())}');
    }
</script>
