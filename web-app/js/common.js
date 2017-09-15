/**
 * Created by Tung Nguyen <tnguyen@ebi.ac.uk> on 14/09/17.
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
            $("#message").removeClass("success");
            $("#message").removeClass("jummpWarning");
            $("#message").addClass("failure");
            $('#message').html("There was an internal error while certifing the information provided.");
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
                $("#message").addClass("failure");
                $('#message').html(response.message);
            }
        }
    });
});

function closeForm() {
    $('#rate_review_form').foundation('close');
}
