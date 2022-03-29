<%--
  Created by IntelliJ IDEA.
  User: Tung Nguyen <nvntung@gmail.com>
  Date: 12/03/2022
  Time: 08:32
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>Manage Model Contributors | BioModels</title>
    <script>
        const currentEmail = "";
        let contributorEmails = [];
        $.each(${contributorEmailList as grails.converters.JSON}, (i, v) => {
            contributorEmails.push(v);
        });
    </script>
    <g:javascript contextPath="" src="toastr.min.js" />
    <link rel="stylesheet"
          href="${resource(dir: 'css', file: 'toastr.min.css', contextPath: "${serverURL}")}" />

</head>

<body>
<h2>Manage Contributors of
    <a href="${createLink(controller: "model", action: "show", id: "${modelId}.${revisionNumber}")}">
    ${modelId}.${revisionNumber}</a></h2>
<div id="model-contributor-list">
    <h3 style="color: red">${message}</h3>
    <h3>The current contributors</h3>
<form name="test_form" method="POST">
    <g:each in="${contributors}" var="cont">
        <g:render template="showContributor" plugin="jummp-plugin-web-application" model="[cont: cont.value]" />
    </g:each>
</form>
</div>
<div class="add-contributor">
    <h3>Invite a contributor</h3>
    <div class="row">
        <div class="columns large-6 medium-6 small-12">
        <div class="input-group">
            <span class="input-group-label">Email</span>
            <input class="input-group-field" type="text" id="txt-email-or-name" name="txt-email-or-name"
                   placeholder="Type a valid email address of the contributor" >
            <div class="input-group-button">
                <input type="submit" class="button" value="Send" id="btn-add-contributor-send">
            </div>
        </div></div>
    </div>
</div>
<script>
    function doCheckEmail(email) {
        let message = "";
        let returned;
        if (email.match(emailRegExp)) {
            const LOOKUP_EMAIL_RESULT = doLookUpUserEmail(email);
            if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.FETCH_FAILED) {
                message = "There has been an internal error happening. Please try again!";
                returned = false;
            } else if (LOOKUP_EMAIL_RESULT === LOOKUP_USER_INFO_STATUS_CODE.NOT_FOUND) {
                message = "The email address " + email + " could not be found, or does not exist.";
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

    $("#txt-email-or-name").blur(function() {
        let email = $(this).val().trim();
        doCheckEmail(email);
    });

    $("#btn-add-contributor-send").on("click", function() {
        let email = $('input[name=txt-email-or-name]').val();
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

        const validEmail = doCheckEmail(email);
        if (!validEmail) { return false; }

        const urlPost = $.jummp.createLink("contributor", "sendContributionInvite");
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
                throw new Error(errMsg);
                return false;
            } else {
                return result.json();
            }
        }).then((response) => {
            const msg = "An invitation has been sent to the user associated with the email " + email + ".";
            showNotification(msg);
            toastr.success(msg);
            contributorEmails.push(response["email"]);
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
        data.append("usernameAndEmail", usernameAndEmail);
        data.append("modelId", "${modelId}");
        data.append("revisionNumber", ${revisionNumber});
        data.append("newRole", currentRole);
        fetch(urlPost, {
            method: "POST",
            body: data
        }).then((result) => {
            if (200 !== result.status) {
                message = "Bad Server Response";
                showNotification(message);
                toastr.error(message);
                throw new Error(message);
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

    $("#model-contributor-list").on("click", "#contributor-remove", function () {
        const parentRow = $(this).parent().parent();
        const usernameAndEmailElement = parentRow.find(".username-email");
        const usernameAndEmail = usernameAndEmailElement.text();
        const userRealName = parentRow.find(".username-email").text();
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
        data.append("modelId", "${modelId}");
        data.append("revisionNumber", ${revisionNumber});
        //data.append("newRole", currentRole);
        fetch(urlPost, {
            method: "POST",
            body: data
        }).then((result) => {
            if (200 !== result.status) {
                message = "Bad Server Response";
                showNotification(message);
                toastr.error(message);
                throw new Error(message);
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
</script>
</body>
</html>
