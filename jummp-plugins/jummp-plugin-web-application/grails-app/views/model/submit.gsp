<%--
  Created by IntelliJ IDEA.
  User: tnguyen@ebi.ac.uk
  Date: 07/09/2020
  Time: 16:30
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="grails.converters.JSON;" %>
<html>
<head>
    <meta name="layout" content="biomodels/main"/>
    <title>${titlePage}</title>
    <link rel="stylesheet" href="${submissionCssHref}"/>
    <link rel="stylesheet" href="${publicationCssHref}"/>
    <link rel="stylesheet"
          href="${resource(contextPath: serverURL, dir: 'css/biomodels/uploader-1.0.2', file:
              'jquery.dm-uploader.min.css')}">
    <link rel="stylesheet"
          href="${resource(contextPath: serverURL, dir: 'css/font-awesome-4.7.0/css', file: 'font-awesome.css')}"/>
    <g:javascript src="helpers.js" contextPath=""/>
    <g:javascript src="toastr.min.js" contextPath=""/>
    <link rel="stylesheet"
          href="${resource(contextPath: serverURL, dir: 'css', file: 'toastr.min.css')}"/>
    <g:javascript>
        var submitterInfo = "${submitterInfo}";
        var submissionFolder = "${submissionFolder}";
        var currentValidation = false;
        var errorMessages = new Array();
        var modelInfo = ${modelInfo};
        var modelFile;
        var additionalFiles;
        var authorMap = { authors: [] };
        var authorList = [];
        if (${publication != null}) {
            authorMap = {
                "authors":
                    ${publication?.authors.collect {
                        String userRealName = it.userRealName ?: ""
                        String institution = it.institution ?: ""
                        String orcid = it.orcid ?: ""
                        def id = it.id ?: "undefined"
                        if (id == "undefined") {
                            [userRealName: userRealName, institution: institution, orcid: orcid]
                        } else {
                            [id: id, userRealName: userRealName, institution: institution, orcid: orcid]
                        }
                    } as JSON }
            };
            authorList = authorMap["authors"];
        }
        var existingFiles = ${existingFiles};
        var publication;
        var isUpdate = ${isUpdate};
        var isAmend = false;
        var revisionComments = "";
        var modelId = "${modelId}";
        var revisionId = "${RevisionID}";
        var revisionNumber = "${RevisionNumber}";
        var changesMade = new Set();

        var latestModelName = "${latestModelName}";

        var latestModelFormat = "${latestModelFormat}";
        var latestModelFormatNameAndVersion = "${latestModelFormatNameAndVersion}";
        var latestReadmeSubmission = "${latestReadmeSubmission}";

        var latestModellingApproach = "${latestModellingApproach}";
        var latestOtherInfo = "${latestOtherInfo}";

        toastr.options = {
            // How long the toast will display without user interaction
            "timeOut": 7000,
            // How long the toast will display after a user hovers over it
            "extendedTimeOut": 10000
        };
    </g:javascript>
</head>

<body>
<div class="row">
    <div class="columns small-12 large-12">
        <div class="text-center">
            <img src="${serverURL}/images/biomodels/loading.gif" id="loading" title="working..." alt="Please wait..."
                 style="display: none" />
        </div>
    </div>
</div>
<form id="msform" useToken="true" class="${submissionFolder}">
    <!-- progressbar -->
    <ul id="progressbar">
        <li class="active" id="upload-file"><strong>Model Files</strong></li>
        <li id="model-info"><strong>Model Information</strong></li>
        <li id="publication-details"><strong>Publication Details</strong></li>
        <li id="summary-submission"><strong>Summary of submission</strong></li>
        <li id="confirm-submission"><strong>Finish</strong></li>
    </ul>
    <ul id="form-validation-bar">
        <li id="step1">
            <i class="fa fa-check-circle-o" aria-hidden="true" style="color: #01a252"></i>
            <i class="fa fa-times-circle" aria-hidden="true" style="color: red"></i>
        </li>
        <li id="step2">
            <i class="fa fa-check-circle-o" aria-hidden="true" style="color: #01a252"></i>
            <i class="fa fa-times-circle" aria-hidden="true" style="color: red"></i></li>
        <li id="step3">
            <i class="fa fa-check-circle-o" aria-hidden="true" style="color: #01a252"></i>
            <i class="fa fa-times-circle" aria-hidden="true" style="color: red"></i></li>
        <li id="step4">
            <i class="fa fa-check-circle-o" aria-hidden="true" style="color: #01a252"></i>
            <i class="fa fa-times-circle" aria-hidden="true" style="color: red"></i></li>
        <li id="step5">
            <i class="fa fa-check-circle-o" aria-hidden="true" style="color: #01a252"></i>
            <i class="fa fa-times-circle" aria-hidden="true" style="color: red"></i></li>
    </ul>
    <div class="progress" role="progressbar" tabindex="0" aria-valuenow="50" aria-valuemin="0"
         aria-valuetext="50 percent" aria-valuemax="100">
        <div class="progress-meter" style="width: 0%"></div>
    </div>

    <!-- field sets -->
    <fieldset>
        <g:render template="/templates/model/submit/uploadingFiles"
                  plugin="jummp-plugin-web-application"/>
    </fieldset>

    <fieldset>
        <g:render template="/templates/model/submit/addingModelInfo"
                  plugin="jummp-plugin-web-application"/>
    </fieldset>

    <fieldset>
        <g:render template="/templates/model/submit/addingPublicationInfo"
                  plugin="jummp-plugin-web-application"/>
    </fieldset>

    <fieldset>
        <g:render template="/templates/model/submit/displayingSummaryOfChanges"
                  plugin="jummp-plugin-web-application"/>
    </fieldset>

    <fieldset>
        <g:render template="/templates/model/submit/completingSubmission"
                  plugin="jummp-plugin-web-application"/>
    </fieldset>
</form>
<g:javascript src="biomodels/submission.js" contextPath="" />
<g:javascript src="biomodels/uploader-1.0.2/jquery.dm-uploader.min.js" contextPath="" />
<script type="text/javascript">
    toastr.options = {
        "closeButton": false,
        "debug": false,
        "newestOnTop": false,
        "progressBar": false,
        "positionClass": "toast-top-right",
        "preventDuplicates": false,
        "onclick": null,
        "showDuration": "600",
        "hideDuration": "1000",
        "timeOut": "10000",
        "extendedTimeOut": "1000",
        "showEasing": "swing",
        "hideEasing": "linear",
        "showMethod": "fadeIn",
        "hideMethod": "fadeOut"
    }
</script>

</body>
<content tag="contexthelp">
    <g:if test="${isUpdate}">
        update
    </g:if>
    <g:else>
        submission
    </g:else>
</content>
</html>
<content tag="submit">
    selected
</content>
