<%@ page import="net.biomodels.jummp.webapp.RequestType" %>
<g:javascript>
    let definedModellingApproachNames = [];
    <g:each in="${definedModellingApproachNames}" var="name">
        definedModellingApproachNames.push("${name}");
    </g:each>
    let definedModelFormatNames = [];
    <g:each in="${modelFormatsSortedByName}" var="fmt">
        definedModelFormatNames.push("${fmt?.name + ' ' + fmt?.formatVersion}");
    </g:each>
</g:javascript>
<style>
    .disable{
        pointer-events: none;
        background: #bfbfbf;
    }
</style>
<div class="row">
    <div class="columns small-12 medium-10 large-10">
        <h2 class="fs-title"><g:message code="submission.biomodels.model.information.heading"
                                 locale="${Locale.getDefault()}"/></h2>
        <p><g:message code="submission.biomodels.model.information.explanation" locale="${Locale.getDefault()}"/></p>
    </div>
    <div class="columns small-12 medium-2 large-2">
        <h2 class="steps">Step 2 - 5</h2>
    </div>
</div>

<div class="row">
    <div class="small-12 medium-6 large-6 columns">
        <label for="name">
            <span class="required">Name</span>&nbsp;
            <span class="assistive-example">[e.g. Launna2020 - T-Cell signalling model]</span></label>
        <g:if test="${"new_name"}">
            <input type="text" id="name" name="name" required value=""
                   placeholder="Enter a simple sentence summarising title for your model or leave the title of the publication."/>
        </g:if>
        <g:else>
            <input type="text" id="name" name="name" required value=""
                   placeholder="Enter a simple sentence summarising title for your model or leave the title of the publication."/>
            <p class="help-text" id="nameHelp" style="color: red">&nbsp;</p>
        </g:else>

        <jummp:displayModelDescriptionLabel>
            <label for="description">${description}</label>
        </jummp:displayModelDescriptionLabel>
        <g:textArea id="description" cols="70" rows="10" name="description"
            value=""
            placeholder="Enter a brief description for your model revision, for example: what are the  differences to the previous ones"/>
    </div>
    <div class="small-12 medium-6 large-6 columns">
        <label for="model_format">
            <span class="required">Model Format</span>&nbsp;
            <span class="assistive-example">[e.g. SBML L3V2, Python 2.7, C/C++]</span></label>
        <g:select name="model_format" id="model_format" required=""
                  from="${modelFormatsSortedByName}"
                  value="${selectedModelFormat}"
                  optionKey="id"
                  optionValue="${{it?.name + ' ' + it?.formatVersion}}"/>
        <div id="readme_submission_div" style="display: none">
            <label for="readme_submission" class="required">
                Describe more exactly your model format (e.g. SBML L3V2, Python 2.7, C/C++)</label>
            <g:textField name="readme_submission" id="readme_submission"
                         value="${readmeSubmission}"
                         placeholder="Please describe here more accurately what is your model format" />
            <p class="help-text" id="readmeSubmissionHelp" style="color: red">&nbsp;</p>
        </div>
        <label for="modelling_approach">
            <span class="required">Modelling Approach</span>&nbsp;
            <span class="assistive-example">[e.g. Constraint-based modelling, Logical model, Markov model,...]</span></label>
        <input type="text" name="modelling_approach" id="modelling_approach" value="${modellingApproach}" required
               placeholder="Enter your modelling approach" aria-describedby="modellingApproachHelp"/>
        <p class="help-text" id="modellingApproachHelp">Find the appropriate one by typing a few more
        first characters of your words. The system will suggest you our defined modelling approaches. If you
        are not sure your modelling approach, please type Other for now.</p>

        <div id="model_other_info_div" style="display: none">
            <label for="other_info" class="required">Describe more exactly your modelling approach</label>
            <g:textField name="other_info" id="other_info"
                         value="${otherInfo}"
                         placeholder="Please enter here what is your modelling approach"/></div>
        <p class="help-text" id="otherInfoHelp" style="color: red !important;">&nbsp;</p>
    </div>
</div>
<input type="hidden" value="false" name="changed" id="changeStatus"/>

<!-- warning model popup -->
<div class="reveal" id="modellingApproachWarningPopup" data-reveal>
<h3 id="messageTitle"
    style="border-bottom: 1px solid grey">Attention!</h3>
<button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
</button>
<div class="contact-panel" id="feedback_panel" data-toggler=".is-active">
    <p class="lead">You have entered a value as your modelling approach which does not exist in our system.
    Please check your spelling or try again. Otherwise, type 'Other' and enter what is your modelling
    approach in the box below.</p>
</div>
</div>

<input type="button" name="next" class="next action-button" value="Next" />
<input type="button" name="previous" class="previous action-button-previous" value="Previous"/>
<script>
    /* This function will do front-end and back-end validation */
    function validateModelInfo() {
        const data = buildModelInfoData();

        // First, perform a client side validation. If the validation goes through, we will perform deeper checks
        // at the back-end side. Both validations need to be returned an AJAX callback.
        const formValidation = validateModelInfoForm(data);
        if (!formValidation["isValid"]) {
            let msg = "";
            currentValidation = false;
            errorMessages = [];
            return $.ajax({
                type: "POST",
                url: "${createLink(controller: "submission", action: "renderFileUploadFailures")}",
                success: function() {
                    msg = "There are some errors. Please check and correct them.";
                    showNotification(msg);
                    console.log(msg);
                },
                error: function() {
                    msg = "Errors when trying to validate the model info form.";
                    console.log(msg);
                    showNotification(msg);
                    toastr.error(msg);
                },
                complete: () => {
                    errorMessages.push(msg);
                    errorMessages.push(...formValidation["messages"]);
                    console.log(errorMessages);
                    showFlashMessages(errorMessages);
                }
            });
        } else {
            // then perform a server side validation
            return $.ajax({
                url: "${createLink(controller: "submission", action: "validateModelInfo")}",
                type: "POST",
                data: data,
                success: function (response) {
                    console.log(JSON.stringify(response));
                    changesMade = response["changesMade"];
                    errorMessages = [];
                    let isNameValid = true;
                    let modelName = $('input[id="name"]').val();
                    modelName = modelName.trim(); // to prevent from the hacking: a string of spaces
                    if (modelName.length === 0) {
                        errorMessages.push("The model name text box is empty or cannot be a string of spaces. Please enter a meaningful name.")
                        isNameValid = false;
                    } else if (modelName.length < 5 || modelName.length > 255) {
                        errorMessages.push("Length of the model name is greater 4 and less 256 characters.")
                        isNameValid = false;
                    }

                    let modelFormat = $("#model_format option:selected").text();
                    let isMFDetected = definedModelFormatNames.filter(e => e === modelFormat).length === 1;
                    let isFMMatched = true;
                    if (modelFormat === "Original code *") {
                        isFMMatched = $('#readme_submission').val().length > 0
                    }
                    if (!isFMMatched) {
                        errorMessages.push("Please explain what is your model format in the corresponding box.");
                    }

                    let modellingApproach = $('#modelling_approach').val();
                    let isMARecognisable = definedModellingApproachNames.filter(ma => ma === modellingApproach).length === 1;
                    if (!isMARecognisable) {
                        errorMessages.push("Please type to choose a modelling approach from the pre-defined values.")
                    }
                    let isMAMatched = true;
                    if (modellingApproach === "Other") {
                        isMAMatched = $('#other_info').val().length > 0;
                    }
                    if (!isMAMatched) {
                        errorMessages.push("Please explain what is your modelling approach in the corresponding box.");
                    }
                    currentValidation = isNameValid && isMFDetected && isFMMatched && isMARecognisable && isMAMatched;
                    if (currentValidation) {
                        updateModelInfoObject();
                    }
                }
            });
        }
    }

    function buildModelInfoData() {
        return {
            submissionFolder: submissionFolder,
            modelId: modelId,
            isUpdate: isUpdate,
            isAmend: isAmend,
            latestModelName: latestModelName,
            latestModelDescription: latestModelDescription,
            latestModelFormat: latestModelFormat,
            latestModelFormatNameAndVersion: latestModelFormatNameAndVersion,
            latestReadmeSubmission: latestReadmeSubmission,
            latestModellingApproach: latestModellingApproach,
            latestOtherInfo: latestOtherInfo,

            editedModelName: $('input[id="name"]').val(),
            editedModelDescription: $('textarea[id="description"]').val(),
            editedModelFormat: $('#model_format').val(),
            editedModelFormatNameAndVersion: $('#model_format option:selected').text(),
            editedReadmeSubmission: $('#readme_submission').val(),
            editedModellingApproach: $('#modelling_approach').val(),
            editedOtherInfo: $('#other_info').val(),

            changesMade: [...changesMade]
        }
    }

    /* This is front-end validation */
    function validateModelInfoForm(data) {
        let messages = [];
        const editedModelFormatNameAndVersion = data["editedModelFormatNameAndVersion"];
        const editedModelDescription =  data["editedModelDescription"];
        let msg = "";
        let isValid = false;
        if (editedModelDescription.length === 0 && editedModelFormatNameAndVersion === "BioModels Metadata Submission *") {
            msg = "You are submitting only metadata to BioModels. Please describe your model briefly in the description box.";
            messages.push(msg);
            // check the upload files if at least CSV file is uploaded
            const hasCSV = existsFileExtension("CSV")
            if (!hasCSV)  {
                msg = "Metadata submission needs a CSV file containing all annotations. Please go back to the previous step to double check it.";
                messages.push(msg);
            }
        } else if (editedModelFormatNameAndVersion.indexOf("SBML") >= 0 && !existsFileExtension("XML")) {
            msg = "You have chosen SBML as the model format but no SBML file (XML extension) has been uploaded.";
            messages.push(msg);
        } else {
            isValid = true;
        }
        return {isValid: isValid, messages: messages};
    }

    function getFileExtension(filename){
        // get file extension
        return filename.split('.').pop();
    }

    function existsFileExtension(extension) {
        const isCSV = getFileExtension(modelFile.filename).toUpperCase() === extension
        const csvFiles = additionalFiles.filter(f => {
            return getFileExtension(f.filename).toUpperCase() === extension
        });
        return isCSV || csvFiles.length > 0
    }

    function associateEventHandlers(id) {
        let descBox = document.getElementById(id);

        if ("onpropertychange" in descBox) {
            descBox.attachEvent("onpropertychange", $.proxy(function () {
                if (event.propertyName === "value")
                    $("#changeStatus").val(true);
            }, descBox));
        } else {
            descBox.addEventListener("input", function () {
                $("#changeStatus").val(true);
            });
        }
    }
    $(document).ready(function () {
        associateEventHandlers("description");
        associateEventHandlers("name");
        // handleModelFormatBoxState();
        handleShowOrHideModelFormatExtraInfo($('#model_format'));
        handleShowOrHideModellingApproachExtraInfo($('#modelling_approach'), false);
        // The validateInputLength function is defined in common.js loading with the footer construction
        validateInputLength('#name', 5, 255, '#nameHelp');
        validateInputLength('#readme_submission', 5, 2048, '#readmeSubmissionHelp');
        validateInputLength('#other_info', 5, 255, '#otherInfoHelp');
    });

    function handleModelFormatBoxState() {
        let mf = $('#model_format');
        if (%{--${format.name != unknownFormat?.name}--}%true) {
            mf.addClass("disable");
        } else {
            mf.removeClass("disable");
        }
    }

    $('#modelling_approach').on('keydown', function () {
        $(this).autocomplete({
            source: function (request, response) {
                $.ajax({
                    url: $.jummp.createLink('model', 'searchModellingApproach'),
                    type: 'POST',
                    dataType: 'json',
                    data: {
                        search: request.term,
                        request: ${RequestType.SEARCH_TERMS.value}
                    },
                    success: function (data) {
                        response(data);
                    }
                });
            },
            select: function (event, ui) {
                let label = ui.item.label;
                $(this).val(label);   // display the selected text
                let id = ui.item.id; // selected value
                $.ajax({
                    url: $.jummp.createLink('model', 'searchModellingApproach'),
                    type: 'POST',
                    data: {
                        id: id,
                        name: label,
                        request: ${RequestType.SELECT_VALUE.value}
                    },
                    dataType: 'json',
                    success: function (response) {
                        let len = response.length;
                        if (len > 0) {
                            let id = response[0]['id'];
                            let accession = response[0]['accession'];
                            let name = response[0]['name'];
                            let resource = response[0]['resource'];
                        }
                    }
                });
                return false;
            }
        });
    });

    $('#modelling_approach').bind('change blur', function () {
        handleShowOrHideModellingApproachExtraInfo(this, true);
    });

    $('#model_format').bind("change blur", function () {
        handleShowOrHideModelFormatExtraInfo(this);
    });

    function handleShowOrHideModellingApproachExtraInfo(selector, flag) {
        let element = $('#model_other_info_div');
        let inputVal = $(selector).val();
        let comparableVal = 'Other';
        if (flag) {
            showWarningMessageIfNecessary(inputVal, definedModellingApproachNames);
        }
        showOrHideBox(element, inputVal, comparableVal, definedModellingApproachNames);
    }

    function handleShowOrHideModelFormatExtraInfo(selector) {
        let $opt = $(selector).find('option:selected');
        let element = $('#readme_submission_div');
        let selectedFormat = $opt.val();
        let selectedText = $opt.text();
        let comparableText = "${unknownFormat.name} *";
        showOrHideBox(element, selectedText, comparableText, definedModelFormatNames);
    }

    function showWarningMessageIfNecessary(inputVal, definedModellingApproachNames) {
        inputVal = $.trim(inputVal);
        let existed = $.inArray(inputVal, definedModellingApproachNames) >= 0;
        if (!existed) {
            let popup = $('#modellingApproachWarningPopup');
            popup.html($(this).html()).foundation('open');
        }
    }

    function showOrHideBox(element, selectedText, comparableText, listOfDefinedValues) {
        selectedText = $.trim(selectedText);
        let existed = $.inArray(selectedText, listOfDefinedValues) >= 0;
        if (selectedText.toLowerCase() === comparableText.toLowerCase() || !existed) {
            $(element).removeAttr("style").show();
        } else {
            $(element).hide();
        }
    }

    function updateModelInfoForm() {
        // the modelFile is the global variable that is updated in step 1: uploading and processing files
        let name = modelFile.detectedModelInfo.name;
        if (!name) {
            name = modelInfo.detectedName;
            if (!name && ${isUpdate}) {
                name = `${RevisionTC?.name}`;
            }
        }
        $('input[id="name"]').val(name);

        let description = modelFile.detectedModelInfo.description;
        if (!description) {
            description = modelInfo.detectedDescription;
            if (!description && ${isUpdate}) {
                // For example: SBML models often have a description in HTML format. To prevent unexpected errors
                // happening in Javascript,  use backticks to assign a block of HTML text to a variable.
                // Read the explanation here [1].
                // [1] https://stackoverflow.com/a/44234016/865603
                description = `${RevisionTC?.description}`;
            }
        }
        $('textarea[id="description"]').val(description);

        // the detected model format could not be identical to the latestModelFormat because the detected one
        // is inferred from the real main file uploaded in the file uploading step. Therefore, the text displayed
        // in the format dropdown box might be different from what we can see from the latest revision format.
        if (modelFile.detectedModelFormat.id !== ${selectedModelFormat} &&
            modelFile.detectedModelFormat.name !== "Other") {
            console.log("Detected Model Format: " + modelFile.detectedModelFormat.name);
            $("#model_format").val(modelFile.detectedModelFormat.id).change();
        }
        // Below are two pieces of information associated with the revision
        let readmeSubmission = modelFile.detectedModelFormat.readme;
        if (!readmeSubmission && isUpdate)  {
            if (typeof modelInfo.detectedModelFormat !== "undefined") {
                readmeSubmission = modelInfo.detectedModelFormat.readme;
            }
            if (!readmeSubmission) {
                readmeSubmission = "${readmeSubmission}";
            }
        }
        $('#readme_submission').val(readmeSubmission);

        // Below are two extra info associated with the model
        let modellingApproach = modelFile.detectedModelInfo.modellingApproach;
        if (!modellingApproach && isUpdate) {
            if (typeof modelInfo.detectedModelling !== "undefined") {
                modellingApproach = modelInfo.detectedModelling.approach;
            }
            if (!modellingApproach) {
                modellingApproach = "${modellingApproach}";
            }
        }
        $('#modelling_approach').val(modellingApproach);

        if (typeof modelInfo.detectedModelling !== "undefined") {
            let otherInfo = modelInfo.detectedModelling.otherInfo;
            if (!otherInfo) {
                otherInfo = "${otherInfo}";
            }
            $('#other_info').val(otherInfo);
        }

        // below are two functions defined in addingModelInfo template
        handleShowOrHideModelFormatExtraInfo($('#model_format'));
        handleShowOrHideModellingApproachExtraInfo($('#modelling_approach'), false);
    }

    function updateModelInfoObject() {
        modelInfo.detectedName = $('input[id="name"]').val();
        modelInfo.detectedDescription = $('textarea[id="description"]').val();

        modelInfo.detectedModelFormat = {};
        modelInfo.detectedModelFormat.id = $('#model_format').val();
        modelInfo.detectedModelFormat.name = $('#model_format option:selected').text();
        modelInfo.detectedModelFormat.readme = $('#readme_submission').val();

        modelInfo.detectedModelling = {};
        modelInfo.detectedModelling.approach = $('#modelling_approach').val();
        modelInfo.detectedModelling.otherInfo = $('#other_info').val();
    }
</script>
