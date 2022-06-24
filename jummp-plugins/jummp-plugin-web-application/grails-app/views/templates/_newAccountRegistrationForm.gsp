<label class="required" for="username"><g:message code="user.signup.ui.username"/></label>
<g:if test="${user}"> <!-- case: edit user, the username should be readonly -->
    <g:textField name="username" value="${user.username}" placeholder="Choose an username" readonly="true"/>
</g:if>
<g:else>
    <g:textField name="username"
                 placeholder="Username containing alphanumeric characters and underscores only"
                 pattern="[a-zA-Z0-9_|]{3,64}" required="true" />
</g:else>


<label class="required" for="email"><g:message code="user.signup.ui.email"/></label>
<g:field type="email" name="email" value="${user?.email}" placeholder="Enter your email address" required="true" />

<label class="required" for="userRealName"><g:message code="user.signup.ui.realname"/></label>
<g:textField name="userRealName" value="${user?.person?.userRealName}" placeholder="Enter your real name" required="true" />

<label for="institution"><g:message code="user.signup.ui.institution"/></label>
<g:textField name="institution" value="${user?.person?.institution}" placeholder="Enter an institution name where you are working now"/>

<label for="orcid"><g:message code="user.signup.ui.orcid"/></label>
<g:textField name="orcid" value="${user?.person?.orcid}" placeholder="For example, 0000-0002-2876-6046"/>
