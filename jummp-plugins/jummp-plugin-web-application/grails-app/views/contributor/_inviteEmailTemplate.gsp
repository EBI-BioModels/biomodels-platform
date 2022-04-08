<%
    def rootHost = serverURL.substring(0, serverURL.lastIndexOf("/biomodels"))
    def acceptURL = rootHost + createLink(action: "handleInviteResponse", params: [ref: refCode, op: 'accept'])
    def rejectURL = rootHost + createLink(action: "handleInviteResponse", params: [ref: refCode, op: 'reject'])
%>
<h1>You are invited!</h1>
<p>${inviterName} invited you to join your submission in BioModels as a ${role}. What would you like to do?</p>
<p><a href="${acceptURL}">
    <input id="btn-accept" value="Accept" class="button" type="button" /></a> &nbsp;
    <a href="${rejectURL}">
        <input id="btn-reject" value="Reject" class="button" type="button" /></a></p>
<p>Notes: Click one of the following links if the buttons do not work.</p>
<p>Accept: <a href="${acceptURL}" target="_blank">${acceptURL}</a></p>
<p>Decline: <a href="${rejectURL}" target="_blank">${rejectURL}</a></p>

