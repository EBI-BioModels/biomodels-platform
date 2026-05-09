<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>Contributor Invitation | BioModels</title>
</head>
<body>
    <div class="content">
        <div class="row">
            <div class="small-12 columns">
                <h2>BioModels Contributor Invitation</h2>
                <g:if test="${confirmed}">
                    <p>${msgUser}</p>
                </g:if>
                <g:else>
                    <p><strong>${inviterName}</strong> has invited you to contribute to a BioModels submission.</p>
                    <p>Would you like to accept or decline this invitation?</p>
                    <g:form controller="contributor" action="handleInviteResponse" method="POST">
                        <input type="hidden" name="ref" value="${reference}"/>
                        <input type="hidden" name="op" value="accept"/>
                        <input type="submit" class="button" value="Accept"/>
                    </g:form>
                    <g:form controller="contributor" action="handleInviteResponse" method="POST">
                        <input type="hidden" name="ref" value="${reference}"/>
                        <input type="hidden" name="op" value="reject"/>
                        <input type="submit" class="button secondary" value="Decline"/>
                    </g:form>
                </g:else>
            </div>
        </div>
    </div>
</body>
</html>
