<%
    def rootHost = serverURL.substring(0, serverURL.lastIndexOf("/biomodels"))
    def acceptURL = rootHost + createLink(action: "handleInviteResponse", params: [ref: refCode, op: 'accept'])
    def rejectURL = rootHost + createLink(action: "handleInviteResponse", params: [ref: refCode, op: 'reject'])
%>
<div class="row" style="height: 80px; background-color: #007c82">
    <div class="columns small-12 medium-6 large-6">
        <a href="${serverURL}" title="BioModels">
            <img src="${serverURL}/images/biomodels/logo_small.png" alt="BioModels"/></a>
    </div>
    <div class="columns small-12 medium-6 large-6">
        %{--<h2>${inviterName}</h2>--}%
    </div>
</div>

<h1>${emailHeading}</h1>

<p><g:if test="${howtoAction == 'Remind'}">This is a friendly reminder that </g:if>${inviterName} invited you to join your submission in BioModels as a ${role}. What would you like to do?</p>
<p><a href="${acceptURL}">
    <input id="btn-accept" value="Accept" class="button" type="button" /></a> &nbsp;
    <a href="${rejectURL}">
        <input id="btn-reject" value="Reject" class="button" type="button" /></a></p>
<p><strong>Notes:</strong> The button not working? Paste the following link into your browser:</p>
<p>Accept: <a href="${acceptURL}" target="_blank">${acceptURL}</a></p>
<p>Decline: <a href="${rejectURL}" target="_blank">${rejectURL}</a></p>
<p>To join your submission, <a href="${serverURL}/registration" target="_blank">register now</a>
    if you haven't been with BioModels yet.</p>
<hr/>
<p><strong>Stay in touch with BioModels</strong><br/>
<a href="${serverURL}" target="_blank">${serverURL}</a><br/>
<a href="https://twitter.com/biomodels" target="_blank">
    <img src="${serverURL}/images/twitter-logo.png" alt="twitter@biomodels"/></a></p>
