<h3 style="color: red">Oh, snap</h3>
<g:if test="${errorTicketId}">
    <p>An error occurred during the submission process. A ticket has been generated
    and the admin has been notified. Your ticket reference is <strong>${errorTicketId}</strong>.
    </p>
</g:if>
<g:else>
    <g:if test="${updateMissingId}">
        <p>A valid model ID was not specified for the update process.</p>
    </g:if>
    <g:else>
        <p>Something bad happened. That is all we know. Sorry about that.</p>
    </g:else>
</g:else>
