const $loading = $('#loading');
$(document)
    .ajaxStart(function () {
        $loading.show();
    })
    .ajaxStop(function () {
        $loading.hide();
    });
$(document).ready(function () {
    // this is the same as the procedure above
    /*$('#loading').bind("ajaxStart", function() {
        $(this).show();
    }).bind("ajaxStop", function() {
        $(this).hide();
    });*/
    let current_fs, next_fs, previous_fs; //fieldsets
    let opacity;
    let current = 1;
    let steps = $("fieldset").length;
    steps = 5;
    setProgressBar(current, steps);
    $(".next").click(function () {
        const navSys = $(this);
        validateData(current).done(function (r) {
            let step;
            current_fs = navSys.parent();
            next_fs = current_fs.next();
            if (currentValidation) {
                // Add Class Active
                $("#progressbar li").eq(current++).addClass("active");

                // show the next fieldset
                next_fs.show();
                // hide the current fieldset with style
                current_fs.animate({opacity: 0}, {
                    step: function (now) {
                        // for making fieldset appear animation
                        opacity = 1 - now;

                        current_fs.css({
                            'display': 'none',
                            'position': 'relative'
                        });
                        next_fs.css({'opacity': opacity});
                    },
                    duration: 500
                });
                setProgressBar(current, steps);
                step = current - 1;
                clearErrorMessages();
                updateSubFormAtStep(current);
            } else {
                showErrorMessages();
                step = current;
            }
            // tick or cross the previous or current step if the validation is valid or invalid respectively
            setCheckList(step, currentValidation);
            if (step === 3) {
                // set the check icon for the displaying summary step
                validateData(4).done(function(response) {
                    setCheckList(4, currentValidation);
                });
            }
        }).then(function (r) {
            console.log("Validated and displayed completely.");
        });
    });

    $(".previous").click(function () {
        current--;
        current_fs = $(this).parent();
        previous_fs = $(this).parent().prev();

        // Remove class active
        $("#progressbar li").eq($("fieldset").index(current_fs)).removeClass("active");
        $("#progressbar li").eq(current).removeClass("active");
        // window.history.replaceState(null, null, '?step=' + current);
        // show the previous fieldset
        previous_fs.show();
        // hide the current fieldset with style
        current_fs.animate({opacity: 0}, {
            step: function (now) {
            // for making fieldset appear animation
                opacity = 1 - now;

                current_fs.css({
                    'display': 'none',
                    'position': 'relative'
                });
                previous_fs.css({'opacity': opacity});
            },
            duration: 500
        });
        setProgressBar(current, steps);
    });

    $(".submit").click(function () {
        return false;
    });

    function hideAllInvalidIcons() {
        $('.fa-times-circle').hide();
        $('.fa-check-circle-o').hide();
    }

    hideAllInvalidIcons();

    function showErrorMessages() {
        if (errorMessages.length) {
            let messages = "<ul>";
            for (i = 0; i < errorMessages.length; i++) {
                messages += "<li>" + errorMessages[i] + "</li>";
            };
            messages += "</ul>";
            $('.flashNotificationDiv').html(messages).show();
        }
    }

    function clearErrorMessages() {
        errorMessages = [];
        $('.flashNotificationDiv').html("").hide();
    }

    /**
     * This function will be called before approaching to the step
     * @param step
     */
    function updateSubFormAtStep(step) {
        switch (step) {
            case 1:
                break;
            case 2:
                // defined in the step 2
                updateModelInfoForm();
                break;
            case 3:
                // defined in the step 3
                guessPublicationAndFillForm();
                break;
            case 4:
                // defined in the step 4
                populateSummaryData();
                break;
            case 5:
                // defined in the step 4
                completeSubmission();
                break;
            default:
                break;
        }
    }

    /**
     * This function will be called once being on this step and moving to the next step. That's why its name is
     * validateData after updating data on the form.
     * @param step
     * @returns {*}
     */
    function validateData(step) {
        let func;
        switch (step) {
            case 1:
                func = validateFileUpload();
                break;
            case 2:
                func = validateModelInfo();
                break;
            case 3:
                func = validatePublicationInfo();
                break;
            case 4:
                func = submitData();
                break;
            default:
                break;
        }
        return func;
    }
});

function setProgressBar(curStep, totalSteps) {
    let percent = (100 / totalSteps) * curStep;
    percent = percent.toFixed();
    $(".progress-meter").css("width", percent + "%");
}

function setCheckList(curStep, isValid) {
    if (isValid) {
        $('#step' + curStep + ' .fa-times-circle').hide();
        $('#step' + curStep + ' .fa-check-circle-o').show();
    } else {
        $('#step' + curStep + ' .fa-check-circle-o').hide();
        $('#step' + curStep + ' .fa-times-circle').show();
    }
}
