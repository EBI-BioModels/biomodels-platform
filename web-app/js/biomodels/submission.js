$(document).ready(function () {
    let current_fs, next_fs, previous_fs; //fieldsets
    let opacity;
    let current = 1;
    let steps = $("fieldset").length;
    steps = 5;
    setProgressBar(current);
    $(".next").click(function () {
        validateData(current);
        let step;
        current_fs = $(this).parent();
        next_fs = current_fs.next();
        if (currentValidation) {
            // Add Class Active
            console.log("before: " + current);
            $("#progressbar li").eq(current++).addClass("active");
            console.log("after: " + current);

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
            setProgressBar(current);
            step = current - 1;
            clearErrorMessages();
            updateSubFormAtStep(current);
        } else {
            showErrorMessages();
            step = current;
        }
        setCheckList(step, currentValidation);
    });

    $(".previous").click(function () {
        current--;
        current_fs = $(this).parent();
        previous_fs = $(this).parent().prev();

        // Remove class active
        $("#progressbar li").eq($("fieldset").index(current_fs)).removeClass("active");
        $("#progressbar li").eq(current).removeClass("active");

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
        setProgressBar(current);
    });

    function setProgressBar(curStep) {
        let percent = (100 / steps) * curStep;
        percent = percent.toFixed();
        $(".progress-meter").css("width", percent + "%");
    }

    $(".submit").click(function () {
        return false;
    })

    function setCheckList(curStep, isValid) {
        console.log("C.S. " + curStep + " --- isValid: " + isValid);
        if (isValid) {
            $('#step' + curStep + ' .fa-times-circle').hide();
            $('#step' + curStep + ' .fa-check-circle-o').show();
        } else {
            $('#step' + curStep + ' .fa-check-circle-o').hide();
            $('#step' + curStep + ' .fa-times-circle').show();
        }
    }

    function hideAllInvalidIcons() {
        $('.fa-times-circle').hide();
        $('.fa-check-circle-o').hide();
    }

    hideAllInvalidIcons();

    function showErrorMessages() {
        if (errorMessages.length) {
            let messages = "<ul>";
            for (i = 0; i < errorMessages.length; i++) {
                console.log(errorMessages[i]);
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

    function updateSubFormAtStep(step) {
        switch (step) {
            case 1:
                break;
            case 2:
                // defined in the step 2
                updateModelInfoForm(modelFile);
                break;
            case 3:
                break;
            default:
                break;
        }
    }

    function validateData(step) {
        switch (step) {
            case 1:
                validateFileUpload();
                break;
            case 2:
                validateModelInfo();
                break;
            case 3:
                validatePublicationInfo();
                break;
            default:
                break;
        }
    }
});
