<div class="row" style="margin: 5px 0px 5px 0px">
    <div class="columns small-12">
        <div class='flashNotificationDiv'>
            <g:if test="${flashMessage && flashMessage.length() > 0}">
                <% flashMessage = message(code: flashMessage, default: flashMessage) %>
                ${flashMessage}
                <a onclick="hideNow()" class="close">&times;</a>
                <% flashMessage = "" %>
            </g:if>
            <g:else>
                <g:if test="${validationErrorOn}">
                    <g:hasErrors bean="${validationErrorOn}">
                        <g:renderErrors bean="${validationErrorOn}" as="list"/>
                        <a onclick="hideNow()" class="close">&times;</a>
                    </g:hasErrors>
                </g:if>
                <g:else>
                    <g:javascript>
                        hideNow()
                    </g:javascript>
                </g:else>
            </g:else>

        </div>
    </div>
</div>
