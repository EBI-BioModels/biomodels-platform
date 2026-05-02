<label class="required" for="username"><g:message code="user.signup.ui.username"/></label>
<g:if test="${user}"> <!-- case: edit user, the username should be readonly -->
    <g:textField name="username" id="username" value="${user.username}"
                 placeholder="Choose an username" readonly="true" autocomplete="username"/>
</g:if>
<g:else>
    <g:textField name="username" id="username" aria-describedby="username-help-text"
                 placeholder="Alphanumeric characters, underscores, dots and hyphens"
                 required="true" autocomplete="username"/>
    <p class="help-text" id="username-help-text"
       style="color: darkred">4–64 characters; letters, numbers, underscores, dots and hyphens only.
    Cannot start or end with a dot, and consecutive dots are not allowed. For more information,
    refer to our <a href="${manualURL}/getting-started-with-biomodels.html#general-rules-for-username-and-passwords"
                    target="_blank">general rules for usernames and passwords</a>.</p>
</g:else>


<label class="required" for="email"><g:message code="user.signup.ui.email"/></label>
<g:field type="email" name="email" id="email" value="${user?.email}"
         placeholder="Enter your email address" required="true" autocomplete="email"/>

<label class="required" for="userRealName"><g:message code="user.signup.ui.realname"/></label>
<g:textField name="userRealName" id="userRealName" value="${user?.person?.userRealName}"
             placeholder="Enter your real name" required="true" autocomplete="name"/>

<label for="institution"><g:message code="user.signup.ui.institution"/></label>
<g:textField name="institution" id="institution" value="${user?.person?.institution}"
             placeholder="Enter an institution name where you are working now" autocomplete="organization"/>

<label for="orcid"><g:message code="user.signup.ui.orcid"/></label>
<g:textField name="orcid" id="orcid" value="${user?.person?.orcid}"
             placeholder="For example, 0000-0002-2876-6046" autocomplete="off"/>

<g:javascript>
    /* global toastr */
    function validateAllowedCharacters(val) {
        if (!/^[a-zA-Z0-9_.-]{4,64}$/.test(val)) {
            return "Username must be 4–64 characters and contain only letters, numbers, underscores, dots and hyphens.";
        }
        if (/^\./.test(val) || /\.$/.test(val)) {
            return "Username cannot start or end with a dot.";
        }
        if (/\.\./.test(val)) {
            return "Username cannot contain consecutive dots.";
        }
        return "";
    }

    $("#username").on("blur", function() {
        const val = $(this).val();
        const message = validateAllowedCharacters(val);
        if (message.length > 0) {
            toastr.error(message);
        }
    });
</g:javascript>
