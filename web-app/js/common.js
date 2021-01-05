/**
 * Created by Tung Nguyen <tnguyen@ebi.ac.uk> on 14/09/17.
 * Updated: 13/12/2017
 */

/**
 * Manipulates the stars-based rating system including a modal form and a strip of five stars
 * which are placed in the footer.
 * Notes: Redesigning the rating system will be effected to the following code. Please pay more
 * attention once you want to customise it.
 */
$('#submitButtonRate').prop('disabled', true);
var allStars = ["star1", "star2", "star3", "star4", "star5"];
var stackOfStars = [];
var currentStar;
$('span[id^=star]').on('click', function() {
    currentStar = this.id;
    Array.prototype.diff = function(a) {
        return this.filter(function(i) {return a.indexOf(i) < 0;});
    };
    if (currentStar !== undefined) {
        var currentStarId = currentStar.substring(4);
        stackOfStars = [];
        var currentStarClass = $('#'+currentStar).attr('class');
        for (i = 1; i <= currentStarId; i++) {
            var idx = i;
            stackOfStars.push("star" + idx);
            $('#star'+ idx).attr('class', 'star-icon full');
        }
        if (currentStarClass == 'star-icon full') {
            $('#'+currentStar).attr('class', 'star-icon');
            stackOfStars.pop();
        }
        var remainingStars = allStars.diff(stackOfStars);
        $.each(remainingStars, function (index, value) {
            $('#'+value).attr('class', 'star-icon');
        });

        $('#rateStar').val(stackOfStars.length);
        if (stackOfStars.length == 0) {
            $('#submitButtonRate').prop('disabled', true);
        } else {
            $('#submitButtonRate').prop('disabled', false);
        }
        console.log(stackOfStars);
    }
});
$('#submitButtonRate').on("click", function(event) {
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
            if (response.status == "200") {
                var thankyouMessage = '<div style="text-align:center;">';
                thankyouMessage += '<img style="text-align: center;" src="' +
                    $.serverUrl + '/images/img_done_check_2x_1.png" />';
                thankyouMessage += '</div>';
                thankyouMessage += '<button class="button" ' +
                    'style="background-color: grey;" onclick="closeForm()">Done</button>';
                $('#messageTitle').html('Thank you for your feedback');
                $('#messageTitle').css('color', '#ffffff');
                $('#rate_review_form').css('background-color', '#007c96')
                $('#feedback_panel').html(thankyouMessage);
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
var orcidRegExp = /^\d{4}-\d{4}-\d{4}-\d{3}(?:\d|X)$/gi;
var emailRegExp = /^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
$("#registerForm #resetFormButton").click(function() {
    $('#registerForm')[0].reset();
});
$('#registerForm input').on("change input", function() {
    hideNow();
});
$('input[id=username]').blur(function() {
    var username = $(this).val().trim();
    if (username !== currentUsername) {
        var message = "";
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
                    message = "A user with this username " + username + " already exists. Please try another one."
                } else {
                    message = "This username does not exist. Please check typos and spelling or try again."
                }
                // When an anonymous user is trying to open a new account and username doesn't exist 
                // or to login the system, don't show the warning message
                if (("create" === actionName && username === "") || 
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
$('input[name=email]').blur(function() {
    var email = $(this).val().trim();
    if (email !== currentEmail) {
        var message = "";
        if (email.match(emailRegExp)) {
            $.ajax({
                dataType: "json",
                cache: false,
                data: {
                    query: email,
                    column: 2
                },
                url: $.jummp.createLink("usermanagement", "lookupUser"),
                success: function (response) {
                    message = response[0];
                    if (message.trim()) {
                        message = "A user with this email address " + message.trim() + " already exists in our database. Please use a different one."
                        showNotification(message);
                    }
                }
            });
        } else {
            message = "Your email address is invalid";
            showNotification(message);
        }
    } else {
        hideNow();
    }
});
$('input[name=orcid]').blur(function() {
    var orcid = $(this).val().trim();
    if (orcid !== currentOrcid) {
        var message = "";
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
    var d = new Date(); // for now
    var hour = d.getHours() < 10 ? "0" + d.getHours().toString() : d.getHours();
    var minute = d.getMinutes() < 10 ? "0" + d.getMinutes().toString() : d.getMinutes();
    var second = d.getSeconds() < 10 ? "0" + d.getSeconds().toString() : d.getSeconds();
    return "T" + hour + ":" + minute + ":"+ second;
}

/**
 * The following functions are used for manipulating data of an array.
 * These functions do basic operations such get, set, remove or retrieve all values, etc.
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
    var values = [];
    for (var k in array) {
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
    var imgData;
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        var image = input.files[0];
        reader.onload = function (event) {
            var imgSrc = event.target.result;
            imgData = event.target.result.replace("data:"+ image.type +";base64,", '');
            $(imageHolder).attr('src', imgSrc);
            $(imageHolder).attr('title', 'This image has been uploaded or replaced');
        };
        reader.readAsDataURL(image);
    }
    return imgData;
}

/**
 * Patches the issue of hiding dropdown menu partially. Foundation dropdown menu script adds opens-inner class
 * improperly causing this problem.
 */
$('#menu-item-myaccount').on('mouseover', function (event) {
    event.preventDefault();
    if ($(this).hasClass("opens-inner")) {
        $(this).removeClass("opens-inner");
        $(this).addClass("opens-left");
    }
});

function validateInputLength(element, minLength, maxLength, messageHolder) {
    $(element).on('keydown keyup change', function(){
        var char = $(this).val();
        var charLength = $(this).val().length;
        if (charLength < minLength){
            $(messageHolder).text('Length is short, minimum '+minLength+' characters required.');
            setTimeout(function() { $(this).focus(); }, 0);
        } else if (charLength > maxLength){
            $(messageHolder).text('Length is not valid, maximum '+maxLength+' characters allowed.');
            $(this).val(char.substring(0, maxLength));
        } else {
            $(messageHolder).text('');
        }
    });
}
