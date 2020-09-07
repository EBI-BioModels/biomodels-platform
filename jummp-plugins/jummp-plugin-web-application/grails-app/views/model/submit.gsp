<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 07/09/2020
  Time: 16:30
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="biomodels/main"/>
    <title>Submit a new model | BioModels</title>
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/12.1.2/css/intlTelInput.css"/>
%{--    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.0.0-beta.2/css/bootstrap.css" />--}%
    <link rel="stylesheet" href="${submissionCssHref}" />
</head>

<body>
<!-- Multi step form -->
<section class="multi_step_form">
    <form id="msform">
        <!-- Tittle -->
        <div class="tittle">
            <h1>Submitting a new model</h1>
        </div>
        <!-- progressbar -->
        <ul id="progressbar">
            <li class="active">Upload Model Files</li>
            <li>Add Model Information</li>
            <li>Add Publication Details</li>
            <li>Summary of Your Submission/Update</li>
        </ul>
        <!-- fieldsets -->
        <fieldset>
            <h2>Select and upload your model files</h2>
            <div class="form-row">
                <div class="form-group col-md-6">
                    <input type="tel" id="phone" class="form-control" placeholder="+880">
                </div>
                <div class="form-group col-md-6">
                    <input type="text" class="form-control" placeholder="+8801123456789">
                </div>
            </div>
            <div class="done_text">
                <a href="#" class="don_icon"><i class="ion-android-done"></i></a>
                <h6>A secret code is sent to your phone. <br>Please enter it here.</h6>
            </div>
            <div class="code_group">
                <input type="text" class="form-control" placeholder="0">
                <input type="text" class="form-control" placeholder="0">
                <input type="text" class="form-control" placeholder="0">
                <input type="text" class="form-control" placeholder="0">
            </div>
            <button type="button" class="action-button previous_button">Back</button>
            <button type="button" class="next action-button">Continue</button>
        </fieldset>
        <fieldset>
            <h3>Verify Your Identity</h3>
            <h6>Please upload any of these documents to verify your Identity.</h6>
            <div class="passport">
                <h4>Govt. ID card <br>PassPort <br>Driving License.</h4>
                <a href="#" class="don_icon"><i class="ion-android-done"></i></a>
            </div>
            <div class="input-group">
                <div class="custom-file">
                    <input type="file" class="custom-file-input" id="upload">
                    <label class="custom-file-label" for="upload"><i class="ion-android-cloud-outline"></i>Choose file</label>
                </div>
            </div>
            <ul class="file_added">
                <li>File Added:</li>
                <li><a href="#"><i class="ion-paperclip"></i>national_id_card.png</a></li>
                <li><a href="#"><i class="ion-paperclip"></i>national_id_card_back.png</a></li>
            </ul>
            <button type="button" class="action-button previous previous_button">Back</button>
            <button type="button" class="next action-button">Continue</button>
        </fieldset>
        <fieldset>
            <h3>Create Security Questions</h3>
            <h6>Please update your account with security questions</h6>
            <div class="form-group">
                <select class="product_select">
                    <option data-display="1. Choose A Question">1. Choose A Question</option>
                    <option>2. Choose A Question</option>
                    <option>3. Choose A Question</option>
                </select>
            </div>
            <div class="form-group fg_2">
                <input type="text" class="form-control" placeholder="Anwser here:">
            </div>
            <div class="form-group">
                <select class="product_select">
                    <option data-display="1. Choose A Question">1. Choose A Question</option>
                    <option>2. Choose A Question</option>
                    <option>3. Choose A Question</option>
                </select>
            </div>
            <div class="form-group fg_3">
                <input type="text" class="form-control" placeholder="Anwser here:">
            </div>
            <button type="button" class="action-button previous previous_button">Back</button>
            <a href="#" class="action-button">Finish</a>
        </fieldset>
        <fieldset>
            <h3>Create Security Questions</h3>
            <h6>Please update your account with security questions</h6>
            <div class="form-group">
                <select class="product_select">
                    <option data-display="1. Choose A Question">1. Choose A Question</option>
                    <option>2. Choose A Question</option>
                    <option>3. Choose A Question</option>
                </select>
            </div>
            <div class="form-group fg_2">
                <input type="text" class="form-control" placeholder="Anwser here:">
            </div>
            <div class="form-group">
                <select class="product_select">
                    <option data-display="1. Choose A Question">1. Choose A Question</option>
                    <option>2. Choose A Question</option>
                    <option>3. Choose A Question</option>
                </select>
            </div>
            <div class="form-group fg_3">
                <input type="text" class="form-control" placeholder="Anwser here:">
            </div>
            <button type="button" class="button action-button previous previous_button">Back</button>
            <a href="#" class="action-button button">Finish</a>
        </fieldset>
    </form>
</section>
<!-- End Multi step form -->
%{--<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>--}%
%{--<script src="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.0.0-beta/js/bootstrap.min.js"></script>--}%
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-easing/1.3/jquery.easing.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/12.1.2/js/intlTelInput.js"></script>
%{--<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js"></script>--}%
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-nice-select/1.1.0/js/jquery.nice-select.min.js"></script>
<script type="text/javascript">
    (function ($) {
        "use strict";

        //* Form js
        function verificationForm() {
            //jQuery time
            var current_fs, next_fs, previous_fs; //fieldsets
            var left, opacity, scale; //fieldset properties which we will animate
            var animating; //flag to prevent quick multi-click glitches

            $(".next").click(function () {
                if (animating) return false;
                animating = true;

                current_fs = $(this).parent();
                next_fs = $(this).parent().next();

                //activate next step on progressbar using the index of next_fs
                $("#progressbar li").eq($("fieldset").index(next_fs)).addClass("active");

                //show the next fieldset
                next_fs.show();
                //hide the current fieldset with style
                current_fs.animate(
                    {
                        opacity: 0
                    },
                    {
                        step: function (now, mx) {
                            //as the opacity of current_fs reduces to 0 - stored in "now"
                            //1. scale current_fs down to 80%
                            scale = 1 - (1 - now) * 0.2;
                            //2. bring next_fs from the right(50%)
                            left = now * 50 + "%";
                            //3. increase opacity of next_fs to 1 as it moves in
                            opacity = 1 - now;
                            current_fs.css({
                                transform: "scale(" + scale + ")",
                                position: "absolute"
                            });
                            next_fs.css({
                                left: left,
                                opacity: opacity
                            });
                        },
                        duration: 800,
                        complete: function () {
                            current_fs.hide();
                            animating = false;
                        },
                        //this comes from the custom easing plugin
                        easing: "easeInOutBack"
                    }
                );
            });

            $(".previous").click(function () {
                if (animating) return false;
                animating = true;

                current_fs = $(this).parent();
                previous_fs = $(this).parent().prev();

                //de-activate current step on progressbar
                $("#progressbar li")
                    .eq($("fieldset").index(current_fs))
                    .removeClass("active");

                //show the previous fieldset
                previous_fs.show();
                //hide the current fieldset with style
                current_fs.animate(
                    {
                        opacity: 0
                    },
                    {
                        step: function (now, mx) {
                            //as the opacity of current_fs reduces to 0 - stored in "now"
                            //1. scale previous_fs from 80% to 100%
                            scale = 0.8 + (1 - now) * 0.2;
                            //2. take current_fs to the right(50%) - from 0%
                            left = (1 - now) * 50 + "%";
                            //3. increase opacity of previous_fs to 1 as it moves in
                            opacity = 1 - now;
                            current_fs.css({
                                left: left
                            });
                            previous_fs.css({
                                transform: "scale(" + scale + ")",
                                opacity: opacity
                            });
                        },
                        duration: 800,
                        complete: function () {
                            current_fs.hide();
                            animating = false;
                        },
                        //this comes from the custom easing plugin
                        easing: "easeInOutBack"
                    }
                );
            });

            $(".submit").click(function () {
                return false;
            });
        }

        //* Add Phone no select
        function phoneNoselect() {
            if ($("#msform").length) {
                $("#phone").intlTelInput();
                $("#phone").intlTelInput("setNumber", "+880");
            }
        }
        //* Select js
        function nice_Select() {
            if ($(".product_select").length) {
                $("select").niceSelect();
            }
        }
        /*Function Calls*/

        verificationForm();
        phoneNoselect();
        nice_Select();

    })(jQuery);
    $(document).ready(function () {
       console.log("Show agreement first");
    });
</script>
</body>
</html>
