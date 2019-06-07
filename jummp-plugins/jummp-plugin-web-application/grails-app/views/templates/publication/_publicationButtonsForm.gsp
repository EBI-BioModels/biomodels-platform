<div class="small-12 medium-12 columns">
    <div class="buttons">
        <g:submitButton name="Cancel" class="button"
                        value="${g.message(code: 'submission.common.cancelButton')}" />
        <g:submitButton name="Back" class="button"
                        value="${g.message(code: 'submission.common.backButton')}" />
        <g:if test="${controllerName == "publication" && (actionName == "show" || actionName == "add")}">
            <g:submitButton id="btnSave" name="Save" class="button"
                            value="${g.message(code: 'submission.publication.saveButton')}" />
        </g:if>
        <g:elseif test="${controllerName == "model"}">
            <g:submitButton id="continueButton" name="Continue" class="button"
                            value="${g.message(code: 'submission.publication.continueButton')}" />
        </g:elseif>
    </div>
</div>
<div name="authorListTemp" id="authorListTemp"
     style="height: 50px; margin: auto; border: 3px solid #73AD21; display: none">
</div>
