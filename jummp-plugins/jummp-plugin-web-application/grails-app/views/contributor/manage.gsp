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
        var contributorEmails = [];
        $.each(${contributorEmailList as grails.converters.JSON}, (i, v) => {
            contributorEmails.push(v);
        });
    </script>
    <g:javascript contextPath="" src="toastr.min.js" />
    <link rel="stylesheet"
          href="${resource(dir: 'css', file: 'toastr.min.css', contextPath: "${serverURL}")}" />

</head>

<body>
<h2>Manage Contributors</h2>
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
            <input class="input-group-field" type="text" id="txt-email-or-name" name="email"
                   placeholder="Type a valid email address of the contributor" >
            <div class="input-group-button">
                <input type="submit" class="button" value="Send" id="btn-add-contributor-send">
            </div>
        </div></div>
    </div>
</div>
<script>

    $("#btn-add-contributor-send").on("click", function() {
        let email = $('input[name=email]').val();
        if (!email) {
            showNotification("Please try with a valid email address!");
            return false;
        }
        if (contributorEmails.indexOf(email) >= 0) {
            showNotification("An invitation has been sent to this email! Please add other contributors!");
            return false;
        }
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
                throw new Error("Bad Server Response");
                return result.text();
            }
            return result.json();
        }).then((response) => {
            contributorEmails.push(response["email"]);
            $("form[name=test_form]").append(response["htmlBasedStringForNewContributor"]);
        }).catch((error) => {
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
            return false;
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
        });
        return true;
    });
</script>
</body>
</html>
