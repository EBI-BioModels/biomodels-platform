/**
 * Created by Tung Nguyen <tnguyen@ebi.ac.uk> on 14/09/17.
 * Updated: 13/12/2017
 */

/**
 * Manipulates the stars-based rating system including a modal form and a strip of five stars
 * which are placed in the footer.
 * Notes: Redesigning the rating system will be affected to the following code. Please pay more
 * attention once you want to customise it.
 */
/* global currentUsername, actionName, currentEmail, currentOrcid, controllerName */
const submitButtonRate = $('#submitButtonRate');
submitButtonRate.prop('disabled', true);
const ALL_STARS = ["star1", "star2", "star3", "star4", "star5"];
let stackOfStars = [];
let currentStar;
$('span[id^=star]').on('click', function() {
    currentStar = this.id;
    Array.prototype.diff = function(a) {
        return this.filter(function(i) {return a.indexOf(i) < 0;});
    };
    if (currentStar !== undefined) {
        const currentStarId = currentStar.substring(4);
        stackOfStars = [];
        const currentStartEle = $('#' + currentStar);
        const currentStarClass = currentStartEle.attr('class');
        for (let i = 1; i <= currentStarId; i++) {
            const idx = i;
            stackOfStars.push("star" + idx);
            $('#star'+ idx).attr('class', 'star-icon full');
        }
        if (currentStarClass === 'star-icon full') {
            currentStartEle.attr('class', 'star-icon');
            stackOfStars.pop();
        }
        const remainingStars = ALL_STARS.diff(stackOfStars);
        $.each(remainingStars, function (index, value) {
            $('#'+value).attr('class', 'star-icon');
        });

        $('#rateStar').val(stackOfStars.length);
        if (stackOfStars.length === 0) {
            submitButtonRate.prop('disabled', true);
        } else {
            submitButtonRate.prop('disabled', false);
        }
        console.log(stackOfStars);
    }
});
submitButtonRate.on("click", function(event) {
    "use strict";
    event.preventDefault();
    $.ajax({
        dataType: "json",
        type: "GET",
        url: $.jummp.createLink("jummp", "feedback"),
        cache: false,
        data: {
            star: $('#rateStar').val(),
            email: $('#emailAddress').val(),
            comment: $('#additionalComment').val()
        },
        error: function (jqXHR) {
            console.error("epic fail", jqXHR.responseText);
            $('#feedback_panel').html("There is an error when trying to submit your feedback. Please fresh the page and try again!");
        },
        success: function (response) {
            if (response.status === "200") {
                let thankfulMessage = '<div style="text-align:center;">';
                thankfulMessage += '<img style="text-align: center;" src="' +
                    $.serverUrl + '/images/img_done_check_2x_1.png"  alt="thank you"/>';
                thankfulMessage += '</div>';
                thankfulMessage += '<button class="button" ' +
                    'style="background-color: grey;" onclick="closeForm()">Done</button>';
                const msgTitleEle = $('#messageTitle');
                msgTitleEle.html('Thank you for your feedback');
                msgTitleEle.css('color', '#ffffff');
                $('#rate_review_form').css('background-color', '#007c96')
                $('#feedback_panel').html(thankfulMessage);
            } else {
                $("#feedback_panel").addClass("failure");
                $('#feedback_panel').html(response.message);
            }
        }
    });
});

function closeForm() {
    $('#rate_review_form').foundation('close');
}

/**
 * The following code is being used for handling sign up a new account and edit user profile.
 * In the section, some variables are defined in specific views, for instance, user edit view
 */
// reference: https://wiki.eprints.org/w/ORCID
const orcidRegExp = /^\d{4}-\d{4}-\d{4}-\d{3}[\dX]$/gi;
const emailRegExp = /^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
$("#registerForm #resetFormButton").click(function() {
    $('#registerForm')[0].reset();
});
$('#registerForm input').on("change input", function() {
    hideNow();
});
$('input[id=username]').blur(function() {
    let username = $(this).val().trim();
    if (username !== currentUsername) {
        let message = "";
        $.ajax({
            dataType: "json",
            cache: false,
            data: {
                query: username,
                column: 1
            },
            url: $.jummp.createLink("usermanagement", "lookupUser"),
            success: function (response) {
                username = response[0];
                username = username.trim();
                if (username) {
                    message = "Use another username. This one is unavailable."
                } else {
                    message = "This username does not exist. Please check typos and spelling or try again."
                }
                // When an anonymous user is trying to open a new account and username doesn't exist
                // or to log in the system, don't show the warning message
                if (("registration" === actionName && username === "") ||
                    ("create" === actionName && username === "") ||
                    ("forgot" === actionName && username !== "") ||
                    ("auth" === actionName && username !== "")) {
                    hideNow();
                } else {
                    showNotification(message);
                }
            }
        });
    } else {
        hideNow();
    }
});

const LOOKUP_USER_INFO_STATUS_CODE = {
    "FETCH_FAILED": -1,
    "NOT_FOUND": 0,
    "FOUND": 1
};

function doLookUpUserEmail(emailAddress) {
    let lookupStatus;
    let emailFound = "";
    $.ajax({
        async: false,
        dataType: "json",
        cache: false,
        data: {
            query: emailAddress,
            column: 2
        },
        url: $.jummp.createLink("usermanagement", "lookupUser"),
        success: function (response) {
            emailFound = response[0];
            if (emailFound.trim()) {
                lookupStatus = LOOKUP_USER_INFO_STATUS_CODE.FOUND;
            } else {
                lookupStatus = LOOKUP_USER_INFO_STATUS_CODE.NOT_FOUND;
            }
        },
        error: function () {
            lookupStatus = LOOKUP_USER_INFO_STATUS_CODE.FETCH_FAILED;
        },
        complete: function (e) {
            // do nothing
        }
    });
    return lookupStatus;
}

$('input[name=email]').blur(function() {
    let email = $(this).val().trim();
    if (email !== currentEmail) {
        let message = "";
        let returned;
        let LOOKUP_EMAIL_RESULT = LOOKUP_USER_INFO_STATUS_CODE.NOT_FOUND;
        if (email.match(emailRegExp)) {
            if (window.location.pathname === $.jummp.createLink("usermanagement", "edit")) {
                return true;
            }
            LOOKUP_EMAIL_RESULT = doLookUpUserEmail(email);
            if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.FETCH_FAILED) {
                message = "There has been an internal error happening. Please try again!";
                returned = false;
            } else if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.NOT_FOUND) {
                message = "The email address " + email + " could not be found, or does not exist.";
                returned = false;
            } else if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.FOUND) {
                message = "The email " + email + " used by another BioModels user. Choose a different one.";
                returned = true;
            } else {
                message = "An unknown error has happened! Please try again.";
                returned = false;
            }
        } else {
            message = "Your email address is invalid";
            returned = false;
        }
        if (("create" === actionName || "registration" === actionName) &&
            (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.NOT_FOUND)) {
            hideNow();
        } else {
            showNotification(message);
        }
        return returned;
    } else {
        hideNow();
    }
});

$('input[name=orcid]').blur(function() {
    const orcid = $(this).val().trim();
    if (orcid !== currentOrcid) {
        let message = "";
        if (orcid.match(orcidRegExp)) {
            // look it up in the database
            $.ajax({
                dataType: "json",
                cache: false,
                data: {
                    query: orcid,
                    column: 3
                },
                url: $.jummp.createLink("usermanagement", "lookupUser"),
                success: function (response) {
                    message = response[0];
                    if (message.trim()) {
                        message = "Someone with this ORCID is already registered in our database.";
                        showNotification(message);
                    }
                }
            });
        } else {
            message = "The ORCID provided is not valid";
            //"<g:message code="net.biomodels.jummp.webapp.RegistrationCommand.orcid.validator.error"/>";
        }
        if (message.trim() !== "") {
            showNotification(message);
        }
    } else {
        hideNow();
    }
});

function escapeSpecialLuceneCharacters(facet_value) {
    facet_value = facet_value.replace(/\+/g, '\\+');
    facet_value = facet_value.replace(/\?/g, '\\?');
    facet_value = facet_value.replace(/\*/g, '\\*');
    facet_value = facet_value.replace(/\(/g, '\\(');
    facet_value = facet_value.replace(/\)/g, '\\)');
    facet_value = facet_value.replace(/\[/g, '\\[');
    facet_value = facet_value.replace(/\]/g, '\\]');
    facet_value = facet_value.replace(/\{/g, '\\{');
    facet_value = facet_value.replace(/\}/g, '\\}');
    facet_value = facet_value.replace(/\:/g, '\\:');
    facet_value = facet_value.replace(/\//g, '\\/');
    return facet_value;
}

function getTimeStamp() {
    const d = new Date(); // for now
    const hour = d.getHours() < 10 ? "0" + d.getHours().toString() : d.getHours();
    const minute = d.getMinutes() < 10 ? "0" + d.getMinutes().toString() : d.getMinutes();
    const second = d.getSeconds() < 10 ? "0" + d.getSeconds().toString() : d.getSeconds();
    return "T" + hour + ":" + minute + ":"+ second;
}

/**
 * The following functions are used for manipulating data of an array.
 * These functions do basic operations such as get, set, remove or retrieve all values, etc.
 * These operations are being used in handling data entered by the end users at working on
 * Curation Notes and Model Of The Month pages.
 */
function get(array, k) {
    return array[k];
}

function set(array, k, v) {
    array[k] = v;
}

function remove(array, k) {
    delete array[k];
}

function values(array) {
    const values = [];
    for (let k in array) {
        values.push(array[k]);
    }
    return values;
}

/**
 * This function helps display the image that has been uploaded into an image holder. It is being used
 * for Curation Notes and Model of The Month editor form
 * @param input         Input file
 * @param imageHolder   An identifier of the email holder
 * @returns {*}         Binary stream denoting the uploaded image
 */
function previewImage(input, imageHolder) {
    // reused sample codes from https://stackoverflow.com/a/4459419/865603
    let imgData;
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        const image = input.files[0];
        reader.onload = function (event) {
            const imgSrc = event.target.result;
            imgData = event.target.result.replace("data:"+ image.type +";base64,", '');
            $(imageHolder).attr('src', imgSrc);
            $(imageHolder).attr('title', 'This image has been uploaded or replaced');
        };
        reader.readAsDataURL(image);
    }
    return imgData;
}

/**
 * This function converts an image from its URL to the based64 format
 *
 * @param imgURL indicating the image via a URL
 * @returns {String} representing the based64 format of the image denoting from the imgURL
 */
function convertImageURL2Data(imgURL) {
    let imgData = null;
    if (imgURL) {
        const image = new Image();
        image.src = imgURL;
        let canvas = document.createElement("canvas");
        canvas.width = image.width;
        canvas.height = image.height;
        let ctx = canvas.getContext("2d");
        ctx.drawImage(image, 0, 0);
        const dataURL = canvas.toDataURL("image/png");
        imgData = dataURL.replace(/^data:image\/(png|jpg);base64,/, "");
    }
    return imgData;
}
/**
 * Patches the issue of hiding a dropdown menu partially. Foundation dropdown menu script adds opens-inner class
 * improperly causing this problem.
 */
$('#menu-item-myaccount').on('mouseover', function (event) {
    event.preventDefault();
    if ($(this).hasClass("opens-inner")) {
        $(this).removeClass("opens-inner");
        $(this).addClass("opens-left");
    }
});

const $isSubMenuItem = $('.is-submenu-item');
$isSubMenuItem.on("mouseover click", function() {
    $('.main-menu-item').removeClass("active");
    $('.main-menu-item a').removeAttr("style");
    let grand = $(this).parent().parent().find('a');
    // get the first menu item
    $(grand[0]).css("background-color", "white");
});

$isSubMenuItem.on("mouseout", function() {
    $('.main-menu-item a').removeAttr("style");
});

$("#menuItemFeedback").on("click", function() {
    $('.main-menu-item').removeClass("active");
    $(this).addClass("active");
});

/**
 * Hamburger menu toggle replaces Foundation 6's ResponsiveToggle plugin.
 *
 * Foundation's ResponsiveToggle._update() is bound to changed.zf.mediaquery
 * and unconditionally hides #biomodels-menu on every mobile breakpoint event,
 * overwriting any state set by toggleMenu(). This means any layout shift
 * (dropdown opening, sticky recalculation, virtual keyboard) resets the menu,
 * causing the hamburger to appear/disappear on each menu item tap.
 *
 * The toggle bar visibility on medium+ is handled purely by the CSS class
 * hide-for-medium (applied in the template), so it is correct at first paint
 * regardless of JS timing. This IIFE manages #biomodels-menu visibility only.
 */
(function () {
    const $menu = $('#biomodels-menu');
    // Foundation's medium breakpoint default: 640px
    const MEDIUM_PX = 640;
    let menuOpenedByUser = false;

    function isDesktop() {
        return window.innerWidth >= MEDIUM_PX;
    }

    function applyState(initial) {
        if (isDesktop()) {
            $menu.show();
            menuOpenedByUser = false;
        } else if (initial || !menuOpenedByUser) {
            $menu.hide();
        }
    }

    applyState(true);

    $('#biomodels-hamburger').on('click', function () {
        $menu.toggle();
        menuOpenedByUser = $menu.is(':visible');
    });

    // Sync menu visibility on resize (e.g. rotating devices, resizing windows)
    $(window).on('resize', function () {
        applyState(false);
    });

    // Close menu when tapping outside the nav on mobile
    $(document).on('click', function (e) {
        if (!isDesktop() &&
            !$(e.target).closest('#biomodels-menu, #biomodels-toggle-bar').length) {
            $menu.hide();
            menuOpenedByUser = false;
        }
    });
}());

/**
 * Check to see if the given HTML element has the length in the range
 * @param element The given HTML element such as inputs
 * @param minLength The minimum length
 * @param maxLength The maximum length
 * @param messageHolder The element where the message will be displayed
 */
function validateInputLength(element, minLength, maxLength, messageHolder) {
    $(element).on('keydown keyup change', function(){
        const char = $(this).val();
        const charLength = $(this).val().length;
        if (charLength < minLength) {
            $(messageHolder).text('Length is short, minimum '+minLength+' characters required.');
            setTimeout(function() { $(this).focus(); }, 0);
        } else if (charLength > maxLength) {
            $(messageHolder).text('Length is not valid, maximum '+maxLength+' characters allowed.');
            $(this).val(char.substring(0, maxLength));
        } else {
            $(messageHolder).text('');
        }
        $(messageHolder).show();
    });
}
