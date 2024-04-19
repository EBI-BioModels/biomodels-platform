<%--
  Created by IntelliJ IDEA.
  User: Tung Nguyen <nvntung@gmail.com>
  Date: 12/03/2022
  Time: 08:32
--%>

<%@ page import="grails.converters.JSON" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>Manage Model Contributors | BioModels</title>
    <script>
        const currentEmail = "";
        let contributorEmails = [];
        $.each(${contributorEmailList as JSON}, (i, v) => {
            contributorEmails.push(v);
        });
    </script>
    <g:render template="/templates/head" plugin="jummp-plugin-web-application" />
    <style>
        .contributor-header {
            font-size: x-large;
            font-weight: bolder;
        }
        #users-list {
            list-style-type: none; /* Remove bullets */
            padding: 0; /* Remove padding */
            margin: 0; /* Remove margins */
        }
    </style>

</head>

<body>
<g:if test="${revision}">
<h2>Manage Contributors of
    <a href="${createLink(controller: "model", action: "show", id: "${modelId}.${revisionNumber}")}">
    ${modelId}.${revisionNumber}</a></h2>

<!-- The current contributors -->
<div id="model-contributor-list">
    <h3 style="color: red">${message}</h3>
    <h3 class="padding-top-medium">The current contributors</h3>
<form name="test_form" method="POST"><div style="background-color: lightskyblue">
    <g:render template="showHeaderTitle" plugin="jummp-plugin-web-application" />
    <jummp:renderContributors contributors="${contributors}" />
</div></form>
</div>

<!-- Add an existing user as a contributor -->
<div class="add-contributor">
    <h3 class="padding-top-xlarge">Add an existing user as a contributor</h3>
    <div class="row">
        <div class="columns large-3 medium-3 small-12">
            <label for="txt-email-or-name" class="text-right middle">Search</label>
        </div>
        <div class="columns large-8 medium-8 small-12">
            <input type="text" id="txt-email-or-name" name="txt-email-or-name"
                   placeholder="Type a valid email address" >
            <div id="suggestion-box"></div>
        </div>
        <div class="columns large-1 medium-1 small-12">
            <input type="submit" class="button" value="Add" id="btn-add-contributor">
        </div>
    </div>
</div>
<!-- Add an external contributor without sending an invitation -->
<div class="invite-contributor">
    <h3 class="padding-top-xlarge">Add an external contributor without sending an invitation</h3>
    <div class="row">
        <div class="columns small-12 medium-3 large-3">
            <label for="txt-display-name">Display Name</label>
            <input type="text" id="txt-display-name" name="txt-display-name"
                   placeholder="Type a full name or scientific name appeared in publications">
        </div>
        <div class="columns small-12 medium-3 large-3">
            <label for="txt-email-address">Email</label>
            <input type="text" id="txt-email-address" name="txt-email-address"
                   placeholder="Type a valid email address">
        </div>
        <div class="columns small-12 medium-3 large-3">
            <label for="txt-orcid">ORCID</label>
            <input type="text" id="txt-orcid" name="txt-orcid"
                   placeholder="Type the orcid id">
        </div>
        <div class="columns small-12 medium-2 large-2">
            <label for="select-defined-roles">Roles</label>
            <select name="defined-role" required id="select-defined-roles" class="form-control">
                <g:each in="${roles}" var="role">
                    <option value="${role}">${role}</option>
                </g:each>
            </select>
        </div>
        <div class="columns small-12 medium-1 large-1">
            <input type="submit" class="button" value="Add" id="btn-add-contributor-wto-invite">
        </div>
    </div>
</div>
<!-- Invite a contributor -->
<div class="invite-contributor">
    <h3 class="padding-top-xlarge">Invite a contributor</h3>
    <div class="row">
        <div class="columns small-12 medium-3 large-3">
            <label for="txt-email-invite" class="text-right middle">Email</label>
        </div>
        <div class="columns small-12 medium-4 large-4">
            <input type="text" id="txt-email-invite" name="txt-email-invite"
                   placeholder="Type a valid email address of the contributor invited">
        </div>
        <div class="columns small-12 medium-4 large-4">
            <select name="defined-role" required id="defined-role" class="form-control">
                <g:each in="${roles}" var="role">
                    <option value="${role}">${role}</option>
                </g:each>
            </select>
        </div>
        <div class="columns small-12 medium-1 large-1">
            <input type="submit" class="button" value="Invite" id="btn-invite">
        </div>
    </div>
</div>
</g:if>
<g:else>
    <h2 style="color: orange">Resource not found</h2>
    <p>Revision <a href="${createLink(controller: "model", action: "show", id: "${modelId}")}">
        ${modelId}</a>.${revisionNumber} does not exist.</p>
</g:else>
<script>
    $(document).ready(function() {
        $('#defined-role option:selected').val("Other");
        // AJAX call for autocomplete
        searchAutocomplete();
    });

    function searchAutocomplete() {
        $("#txt-email-or-name").keyup(function(){
            const postURL = "${createLink(controller: "usermanagement", action: "searchUsersForAddContributors")}";
            $.ajax({
                type: "POST",
                url: postURL,
                data: {
                    searchTerm: $(this).val(),
                    column: 1 // or 2
                },
                beforeSend: function(){
                    $("#txt-email-or-name").css("background", "#FFF url(${serverURL}/images/loading.gif) no-repeat 225px");
                },
                success: function(data){
                    const usersList = data["users"];
                    if (usersList !== undefined && usersList.length > 0) {
                        $("#suggestion-box").show();
                        $("#suggestion-box").html(data["htmlBasedStringOfUsers"]);
                    } else {
                        $("#suggestion-box").hide();
                    }
                    $("#txt-email-or-name").css("background", "#ffffff"); //"#87cefa"
                }
            });
        });
    }

    // To select user found: to display
    function selectFoundUser(val) {
        $("#txt-email-or-name").val(val);
        $("#suggestion-box").hide();
    }

    function doCheckEmail(email) {
        let message = "";
        let returned;
        if (email.match(emailRegExp)) {
            const LOOKUP_EMAIL_RESULT = doLookUpUserEmail(email);
            if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.FETCH_FAILED) {
                message = "There has been an internal error happening. Please try again!";
                returned = false;
            } else if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.NOT_FOUND) {
                message = "The email address " + email + " could not be found, or does not exist in our system.";
                returned = false;
            } else if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.FOUND) {
                returned = true;
            } else {
                message = "An unknown error has happened! Please try again.";
                returned = false;
            }
        } else {
            message = "Your email address provided is invalid.";
            returned = false;
        }
        if (message) {
            clearNotification();
            showNotification(message);
            toastr.warning(message);
        };
        return returned;
    }


    $("#txt-email-or-name").on("blur", function() {
        let email = $(this).val().trim();
        doCheckEmail(email);
    });

    $("#txt-email-invite").on("blur", function() {
        let email = $(this).val().trim();
        doCheckEmail(email);
    });

    $("#btn-add-contributor").on("click", function() {
        let email = $('input[name=txt-email-or-name]').val();
        let valid = preValidate(email);
        if (!valid) {
            return false;
        }
        valid = doCheckEmail(email);
        if (!valid) {
            return false;
        }
        const urlPost = $.jummp.createLink("contributor", "add");
        let data = new FormData();
        data.append("modelId", "${modelId}");
        data.append("revisionNumber", ${revisionNumber});
        data.append("email", email);
        fetch(urlPost, {
            method: "POST",
            body: data
        }).then((result) => {
            if (200 !== result.status) {
                const errMsg = "Bad Server Response";
                showNotification(errMsg);
                toastr.error(errMsg);
                //throw new Error(errMsg);
                return false;
            } else {
                return result.json();
            }
        }).then((response) => {
            const msg = "A confirmation email has been sent to the user associated with the email " + email + ".";
            showNotification(msg);
            toastr.success(msg);
            contributorEmails.push(response["email"]);
            if ($(".row .contributors-body").length) {
                $(".row .contributors-header").after(response["htmlBasedStringForNewContributor"]);
            } else {
                $(".row .contributors-body").after(response["htmlBasedStringForNewContributor"]);
            }
        }).catch((error) => {
            const errMsg = "There has been an internal error. Please try again or later.";
            showNotification(errMsg);
            toastr.error(errMsg);
            console.log(error);
        });
        return true;
    });

    $("#btn-add-contributor-wto-invite").on("click", function() {
        let email = $('input[name=txt-email-address]').val();
        let valid = preValidate(email);
        if (!valid) {
            return false;
        }
        /*valid = doCheckEmail(email);
        if (!valid) {
            return false;
        }*/
        let displayName = $('input[name=txt-display-name]').val();
        let orcid = $('input[name=txt-orcid]').val();
        let role = $("#select-defined-roles option:selected").text();
        const urlPost = $.jummp.createLink("contributor", "addWithoutInvitation");
        let data = new FormData();
        data.append("modelId", "${modelId}");
        data.append("revisionNumber", "${revisionNumber}");
        data.append("displayName", displayName);
        data.append("email", email);
        data.append("orcid", orcid);
        data.append("role", role);
        fetch(urlPost, {
            method: "POST",
            body: data
        }).then((result) => {
            if (200 !== result.status) {
                const errMsg = "Bad Server Response";
                showNotification(errMsg);
                toastr.error(errMsg);
                //throw new Error(errMsg);
                return false;
            } else {
                return result.json();
            }
        }).then((response) => {
            const m = displayName + ", " + email + (orcid !== "" ? ", " + orcid : "");
            const msg = "A new contributor [" + m  + "] has been added.";
            showNotification(msg);
            toastr.success(msg);
            contributorEmails.push(response["email"]);
            if ($(".row .contributors-body").length) {
                $(".row .contributors-header").after(response["htmlBasedStringForNewContributor"]);
            } else {
                $(".row .contributors-body").after(response["htmlBasedStringForNewContributor"]);
            }
        }).catch((error) => {
            const errMsg = "There has been an internal error. Please try again or later. " +
                "If the error persists, please contact our developers team. Thank you for your patience.";
            showNotification(errMsg);
            toastr.error(errMsg);
            console.log(error);
        });
        return true;
    });

    $("#btn-invite").on("click", function() {
        let email = $('input[name=txt-email-invite]').val();
        let valid = preValidate(email);
        if (!valid) {
            return false;
        }
        const LOOKUP_EMAIL_RESULT = doLookUpUserEmail(email);
        if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.FOUND) {
            const msg = "The email " + email + " already exists. Please try with another email or add this user to the contributor list.";
            showNotification(msg);
            toastr.warning(msg);
            return false;
        }

        let role = $("#defined-role option:selected").text();
        const urlPost = $.jummp.createLink("contributor", "invite");
        let data = new FormData();
        data.append("serverURL", "${serverURL}");
        data.append("modelId", "${modelId}");
        data.append("revisionNumber", "${revisionNumber}");
        data.append("inviterUsername", "${currentUsername}");
        data.append("inviterEmail", "${currentUserEmail}");
        data.append("inviterName", "${currentUserRealName}");
        data.append("inviteeEmail", email);
        data.append("role", role);
        fetch(urlPost, {
            method: "POST",
            body: data
        }).then((result) => {
            if (200 !== result.status) {
                const errMsg = "Bad Server Response";
                showNotification(errMsg);
                toastr.error(errMsg);
                //throw new Error(errMsg);
                return false;
            } else {
                return result.json();
            }
        }).then((response) => {
            const msg = "An invitation has been sent to the email address " + email + ".";
            showNotification(msg);
            toastr.success(msg);
            console.log(JSON.stringify(response));
            //contributorEmails.push(response["email"]);
            $("form[name=test_form]").append(response["htmlBasedStringForNewContributor"]);
        }).catch((error) => {
            const errMsg = "There has been an internal error. Please try again or later.";
            showNotification(errMsg);
            toastr.error(errMsg);
            console.log(error);
        });
        return true;
    });

    $("#model-contributor-list").on("change", "#role", function () {
        const currentRole = $(this).val();
        console.log("Current Role: " + currentRole);
        const parentRow = $(this).parent().parent();
        const usernameAndEmailElement = parentRow.find(".username-email");
        const usernameAndEmail = usernameAndEmailElement.text();
        const displayName = parentRow.find(".user-real-name").text().replaceAll("\n", "").trim();
        let message = "";
        if (!usernameAndEmail) {
            message = "Cannot update the contribution role due to an error!";
            showNotification(message);
            toastr.error(message);
            return true;
        }
        toastr.success(usernameAndEmail);
        showNotification(usernameAndEmail);
        const urlPost = $.jummp.createLink("contributor", "updateRole");
        let data = new FormData();
        data.append("displayName", displayName);
        data.append("usernameAndEmail", usernameAndEmail);
        data.append("modelId", "${modelId}");
        data.append("revisionNumber", "${revisionNumber}");
        data.append("newRole", currentRole);
        fetch(urlPost, {
            method: "POST",
            body: data
        }).then((result) => {
            if (200 !== result.status) {
                message = "Bad Server Response";
                showNotification(message);
                toastr.error(message);
                //throw new Error(message);
                return result.text();
            }
            return result.json();
        }).then((response) => {
            message = response["message"];
            showNotification(message);
            toastr.success(message);
        }).catch((error) => {
            console.log(error);
            return false;
        });
        return true;
    });

    $("#model-contributor-list").on("click", ".contributor-remove.unlocked", function () {
        const parentRow = $(this).parent().parent();
        const usernameAndEmailElement = parentRow.find(".username-email");
        const usernameAndEmail = usernameAndEmailElement.text().trim("\n");
        const userRealName = parentRow.find(".user-real-name").text().trim("\n");
        const roleName = $("#defined-role option:selected").text();
        const isExternal = parentRow.hasClass("external-contributor");
        let message = "";
        if (!usernameAndEmail) {
            message = "Cannot remove the contribution role due to an error!";
            showNotification(message);
            toastr.error(message);
            return true;
        }
        const urlPost = $.jummp.createLink("contributor", "remove");
        let data = new FormData();
        data.append("usernameAndEmail", usernameAndEmail);
        data.append("userRealName", userRealName);
        data.append("roleName", roleName);
        data.append("modelId", "${modelId}");
        data.append("revisionNumber", "${revisionNumber}");
        data.append("externalContributor", isExternal);
        //data.append("newRole", currentRole);
        fetch(urlPost, {
            method: "POST",
            body: data
        }).then((result) => {
            if (200 !== result.status) {
                message = "Bad Server Response";
                showNotification(message);
                toastr.error(message);
                //throw new Error(message);
                return result.text();
            }
            return result.json();
        }).then((response) => {
            const email = usernameAndEmail.split(", ")[1];
            contributorEmails = jQuery.grep(contributorEmails, function(e) {
                return e !== email;
            });
            parentRow.remove();
            message = response["message"];
            showNotification(message);
            toastr.success(message);
        }).catch((error) => {
            console.log(error);
            return false;
        });
        return true;
    });

    $("#btn-init-contributors").on("click", function() {
        const urlPost = $.jummp.createLink("contributor", "init");
        let data = new FormData();
        data.append("modelId", "${modelId}");
        data.append("revisionNumber", "${revisionNumber}");
        let message = "";
        fetch(urlPost, {
            method: "POST",
            body: data
        }).then((result) => {
            if (200 !== result.status) {
                message = "Bad Server Response";
                if (403 === result.status) {
                    message = "You are not allowed to perform this operation."
                }
                showNotification(message);
                toastr.error(message);
                //throw new Error(message);
                return result.text();
            }
            return result.json();
        }).then((response) => {
            message = response["message"];
            showNotification(message);
            toastr.success(message);
            console.log("htmlBasedStringOfContributors: " + response["htmlBasedStringOfContributors"]);
            $(".row .contributors-body").remove();
            $(".row .contributors-header").after(response["htmlBasedStringOfContributors"]);
        }).catch((error) => {
            console.log(error);
            return false;
        });
        return true;
    });

    function preValidate(email) {
        let message;
        if (!email) {
            message = "Please try with a valid email address!";
            clearNotification();
            showNotification(message);
            toastr.warning(message);
            return false;
        }

        if (contributorEmails.indexOf(email) >= 0) {
            message = "The user associated with this email " + email + " has already been invited or added to the contributor list.";
            showNotification(message);
            toastr.warning(message);
            return false;
        }
        return true;
    }
</script>
</body>
</html>
