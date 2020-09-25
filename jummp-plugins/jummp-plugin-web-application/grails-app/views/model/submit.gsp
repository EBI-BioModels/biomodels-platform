<%--
  Created by IntelliJ IDEA.
  User: tnguyen@ebi.ac.uk
  Date: 07/09/2020
  Time: 16:30
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="biomodels/main"/>
    <title>Submit a new model | BioModels</title>
    <link rel="stylesheet" href="${submissionCssHref}"/>
    <link rel="stylesheet" href="${publicationCssHref}"/>
    <link rel="stylesheet"
          href="${resource(contextPath: serverURL, dir: 'css/biomodels/uploader-1.0.2', file:
              'jquery.dm-uploader.min.css')}">
    <link rel="stylesheet"
          href="${resource(contextPath: serverURL, dir: 'css/font-awesome-4.7.0/css', file: 'font-awesome.css')}"/>
    <g:javascript src="toastr.min.js" contextPath=""/>
    <link rel="stylesheet"
          href="${resource(contextPath: serverURL, dir: 'css', file: 'toastr.min.css')}"/>
    <script type="text/javascript">
        var currentValidation = false;
        var errorMessages = new Array();
        var modelInfo = {};
        var modelFile;
        var additionalFiles;
        var authorMap = { authors: [] };
        var authorList = authorMap.authors;
        var publication;
    </script>
</head>

<body>
<form id="msform" useToken="true" class="${submissionSessionId}">
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
        <div class="row">
            <div class="columns small-12 medium-7 large-7">
                <h2 class="fs-title">Finish:</h2>
            </div>

            <div class="columns small-12 medium-5 large-5">
                <h2 class="steps">Step 5 - 5</h2>
            </div>
        </div>

        <h2 class="purple-text text-center"><strong>SUCCESS !</strong></h2>

        <div class="row align-center">
            <div class="columns small-12 medium-12 large-12" style="text-align: center">
                <img src="https://i.imgur.com/GwStPmg.png" class="fit-image" style="width: 25%"></div>
        </div>

        <div class="row align-center">
            <div class="columns small-12 medium-12 large-12">
                <h5 class="purple-text text-center">You Have Successfully Signed Up</h5>
            </div>
        </div>
    </fieldset>
</form>
<script
    src="${resource(contextPath: serverURL, dir: 'js/biomodels', file: 'submission.js')}"></script>
<script
    src="${resource(contextPath: serverURL, dir: 'js/biomodels/uploader-1.0.2',
        file: 'jquery.dm-uploader.min.js')}"></script>

</body>
</html>
