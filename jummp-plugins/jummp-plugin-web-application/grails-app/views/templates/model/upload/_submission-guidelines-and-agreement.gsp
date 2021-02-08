<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 07/09/2020
  Time: 21:25
--%>

<h2>Submission Guidelines</h2>
<g:if test="${isUpdate}">
    <g:message code="submission.disclaimer.updateMessage" args="${ [params.id] }" />
</g:if>
<g:else>
    <g:if test="${'default' != style}">
        <g:message code="submission.disclaimer.${style}.createMessage" args="${createMsgArgs}"/>
    </g:if>
    <g:else>
        <g:message code="submission.disclaimer.default.createMessage"/>
    </g:else>
</g:else>
<g:form>
    <div class="dialog">
        <div class="buttons">
            <input type="button" name="Cancel" id="Cancel" class="button"
                   value="${g.message(code: 'submission.common.cancelButton')}" />
            <input type="button" name="Continue" id="Continue" class="button"
                   value="${g.message(code: 'submission.disclaimer.continueButton')}" />
        </div>
    </div>
</g:form>
<div id="dialog" title="SBML creator sample" hidden>
    <p><g:message code="submission.disclaimer.biomodels.guidelines.sbmlexample"/></p>
</div>
