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
$('#menuItemFeedback').on('click', function() {
    $(this).addClass("active");
});
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
$('input[name=username]').blur(function() {
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
                if (username.trim()) {
                    message = "A user with this username " + username.trim() + " already exists. Please try another one."
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
