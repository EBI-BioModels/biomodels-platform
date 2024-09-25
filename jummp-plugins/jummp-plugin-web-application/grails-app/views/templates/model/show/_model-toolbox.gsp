<sec:ifLoggedIn>
<!-- TODO: create dialog boxes to confirm -->
<!-- Load an icon library -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
<div id="model-toolbox" class="sidenav">
    <a href="javascript:void(0)" class="closebtn" onclick="closeNav()">&times;</a>
    <a href="javascript:void(0)" onclick="linkServeOmex()" title="Download the model in OMEX format">
        <i class="fa fa-download" aria-hidden="true"></i> Download</a>
    <g:if test="${canUpdate}">
        <a href="javascript:void(0)" onclick="update()" title="Update the model">
            <i class="fa fa-pencil-square-o" aria-hidden="true"></i> Update</a>
    </g:if>
    <g:if test="${canDelete}">
        <a href="javascript:void(0)" data-open="popup-confirm-archive-model" title="Archive the model">
            <i class="fa fa-archive" aria-hidden="true"></i> Delete</a>
    </g:if>

    <g:if test="${canSubmitForPublication}">
        <a class='toolbutton' id="peer-review"
           title="Submit for publication"
           data-open="popup-confirm-submit-publication">
            <i class="fa fa-unlock" aria-hidden="true"></i> Publish</a>
    </g:if>

    <g:if test="${showPublishOption}">
        <a class='toolbutton' id="publish" title="Publish the model"
           data-open="popup-confirm-publish"><i class="fa fa-unlock" aria-hidden="true"></i> Publish</a>
    </g:if>
    <g:if test="${showUnpublishOption}">
        <a id="unpublish" title="Unpublish the model"
           data-open="popup-confirm-unpublish">
            <i class="fa fa-lock" aria-hidden="true"></i> Unpublish</a>
    </g:if>
    <g:if test="${canShare}">
        <a class='toolbutton' id="share" onclick="share()" title="Share your model with contributors">
            <i class="fa fa-share-square-o" aria-hidden="true"></i> Share</a>
    </g:if>
    <g:if test="${canCertify}">
        <a class='toolbutton' id="certify" title="Certify model" onclick="certify()">
            <i class="fa fa-certificate" aria-hidden="true"></i> Certify</a>
    </g:if>
    <g:if test="${canCheckConsistency}">
        <a id="checkConsistency"
           title="Check consistency"
           data-open="popup-confirm-check-consistency">
            <i class="fa fa-check-circle-o" aria-hidden="true"></i> Check</a>
    </g:if>
    <g:if test="${hasCuratorRole && supportedForConversion}">
        <a id="convert"
           title="Convert This Model To The Other Formats"
           onclick="convert()">
            <i class="fa fa-exchange" aria-hidden="true"></i> Convert</a>
    </g:if>
    <g:if test="${canAskReviewerAccount}">
        <a href="javascript:void(0)" onclick="openReviewerAccount()"
           title="Open a reviewer account for this model">
            <i class="fa fa-tasks" aria-hidden="true"></i> Reviewer</a>
    </g:if>
    <g:if test="${canAskReviewerAccount}">
        <!-- canAddContributor is the same canAskReviewerAccount -->
        <a href="javascript:void(0)" onclick="manageContributors()"
           title="Manage the list of contributors of your model">
            <i class="fa fa-users" aria-hidden="true"></i> Members</a>
    </g:if>
    <g:if test="${hasCuratorRole}"><!-- canIndex is the same hasCuratorRole -->
        <a href="javascript:void(0)" onclick="indexModelRevision()"
           title="Reindex your model">
            <i class="fa fa-database" aria-hidden="true"></i> Index</a>
    </g:if>
</div>
<span style="font-size:20px;cursor:pointer" onclick="openNav()">&#9776; Model ToolBox</span>

<!-- Define the modal dialogs to confirm the operations -->
<div>
    <!-- Confirm to delete the model -->
    <div class="reveal" id="popup-confirm-archive-model" data-reveal>
        <h1>Confirm!</h1>
        <p class="lead">Are you sure you want to delete (e.g., archive) the model?</p>
        <button class="close-button" data-close aria-label="Close reveal" type="button">
            <span aria-hidden="true">&times;</span>
        </button>
        <div class="button-group primary">
            <button class="button secondary" id="btn-no-to-archive-model">No</button>
            <button class="button" id="btn-yes-to-archive-model">Yes</button>
        </div>
    </div>

    <!-- Confirm to submit for model publication -->
    <div class="reveal" id="popup-confirm-submit-publication" data-reveal>
    <g:if test="${revision.model.publication}">
        <h1><jummp:renderSubmitForPublicationConfirmDialogTitle/></h1>
        <p class="lead"><jummp:renderSubmitForPublicationConfirmDialogMessage/></p>
    </g:if>
    <g:else>
        <h1><jummp:renderSubmitForPublicationWarningDialogTitle/></h1>
        <p class="lead"><jummp:renderSubmitForPublicationWarningDialogMessage/></p>
    </g:else>
        <button class="close-button" data-close aria-label="Close reveal" type="button">
            <span aria-hidden="true">&times;</span>
        </button>
        <div class="button-group primary">
            <button class="button secondary" id="btn-no-to-submit-publication">No</button>
            <button class="button" id="btn-yes-to-submit-publication">Yes</button>
        </div>
    </div>

    <!-- Confirm to check consistency -->
    <div class="reveal" id="popup-confirm-check-consistency" data-reveal>
        <h1>Confirm!</h1>
        <p class="lead">Checking model consistency uses an online validator.
        This might take time for uploading and validating the model. Do you want to proceed the validation?</p>
        <button class="close-button" data-close aria-label="Close reveal" type="button">
            <span aria-hidden="true">&times;</span>
        </button>
        <div class="button-group primary">
            <button class="button secondary" id="btn-no-to-check-consistency">No</button>
            <button class="button" id="btn-yes-to-check-consistency">Yes</button>
        </div>
    </div>

    <!-- Confirm to publish -->
    <div class="reveal" id="popup-confirm-publish" data-reveal>
        <h1>Confirm!</h1>
        <p class="lead">Make this version of the model visible to anyone without logging in?</p>
        <button class="close-button" data-close aria-label="Close reveal" type="button">
            <span aria-hidden="true">&times;</span>
        </button>
        <div class="button-group primary">
            <button class="button secondary" id="btn-no-to-publish">No</button>
            <button class="button" id="btn-yes-to-publish">Yes</button>
        </div>
    </div>

    <!-- Confirm to unpublish  -->
    <div class="reveal" id="popup-confirm-unpublish" data-reveal>
        <h1>Confirm!</h1>
        <p class="lead">You're about to unpublish the model. Are you sure?</p>
        <button class="close-button" data-close aria-label="Close reveal" type="button">
            <span aria-hidden="true">&times;</span>
        </button>
        <div class="button-group primary">
            <button class="button secondary" id="btn-no-to-unpublish">No</button>
            <button class="button" id="btn-yes-to-unpublish">Yes</button>
        </div>
    </div>
</div>

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
    $("#model-toolbox").on("mouseleave", function () {
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

    $('#btn-yes-to-archive-model').on("click", function() {
        archive();
    });
    $('#btn-no-to-archive-model').on("click", function() {
        closePopup("popup-confirm-archive-model");
    });

    $('#btn-yes-to-submit-publication').on("click", function() {
        submitForPublication();
    });
    $('#btn-no-to-submit-publication').on("click", function() {
        closePopup("popup-confirm-submit-publication");
    });

    $('#btn-yes-to-check-consistency').on("click", function() {
        checkConsistency();
    });
    $('#btn-no-to-check-consistency').on("click", function() {
        closePopup("popup-confirm-check-consistency");
    });

    $('#btn-yes-to-publish').on("click", function() {
        publish();
    });
    $('#btn-no-to-publish').on("click", function() {
        closePopup("popup-confirm-publish");
    });

    $('#btn-yes-to-unpublish').on("click", function() {
        unpublish();
    });
    $('#btn-no-to-unpublish').on("click", function() {
        closePopup("popup-confirm-unpublish");
    });

    function closePopup(modalDialogId) {
        $('#'+modalDialogId).foundation('close');
    }

    function certify() {
        $.jummp.openPage('${g.createLink(controller: 'qcInfo', action: 'edit', id: revision.modelIdentifier())}');
    }

    function update() {
        $.jummp.openPage("${g.createLink(controller: 'model', action: 'update', id: revision.modelIdentifier())}")
    }

    function share() {
        $.jummp.openPage('${g.createLink(controller: 'model', action: 'share', id: revision.identifier())}')
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

    function publish() {
        $.jummp.openPage("${g.createLink(controller: 'model', action: 'publish', id: revision.identifier() )}");
    }

    function unpublish() {
        $.jummp.openPage("${g.createLink(controller: 'model', action: 'unpublish', id: revision.identifier() )}");
    }

    function checkConsistency() {
        const url = "${g.createLink(controller: 'sbml', action: 'checkConsistency', id: revision.identifier())}";
        $.jummp.openPage(url);
    }

    function convert() {
        let url = "${g.createLink(controller: 'conversion', action: 'convert')}";
        url += "?id=${revision.model.submissionId}&revisionId=${revision.revisionNumber}"
        $.jummp.openPage(url);
    }

    function submitForPublication() {
        $.jummp.openPage("${g.createLink(controller: 'model', action: 'submitForPublication', id: revision.identifier())}");
    }
</script>
</sec:ifLoggedIn>
<script>
    /**
     * This function computes a proper link for the download button depending on the size/total size of the model
     * files in the submission in request.
     * @returns {string|void}
     */
    function linkServeOmex() {
        const link = "${createLink(controller: 'model', action: 'download', id: revision.identifier())}";
        $.jummp.openPage(link);
    }

</script>
