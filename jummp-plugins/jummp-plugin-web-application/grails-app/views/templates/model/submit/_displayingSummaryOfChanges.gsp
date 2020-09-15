<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.core.model.RevisionTransportCommand" %>
<%@ page import="net.biomodels.jummp.core.model.ModelTransportCommand" %>
<%
    /*ModelTransportCommand model = workingMemory.get("ModelTC")
    RevisionTransportCommand revision = workingMemory.get("RevisionTC")*/
%>
<div class="row">
    <div class="columns small-12 medium-7 large-7">
        <h2 class="fs-title"><g:message code="submission.summary.header"/></h2>
    </div>

    <div class="columns small-12 medium-5 large-5">
        <h2 class="steps">Step 4 - 5</h2>
    </div>
</div>
<div class="row">
    <div class="columns small-12 medium-12 large-12">

        %{--<table class="formtable responsive-table">
            <tbody>
            <tr class="prop">
                <td class="name" style="vertical-align:top;">
                    <label for="${g.message(code: 'submission.summary.nameLabel')}">
                        <g:message code="submission.summary.nameLabel"/>
                    </label>
                </td>
                <td class="value" style="vertical-align:top;">
                    <g:if test="${workingMemory["new_name"]}">
                        ${workingMemory["new_name"]}
                    </g:if>
                    <g:else>
                        ${revision.name}
                    </g:else>
                </td>
            </tr>
            <tr class="prop">
                <td class="name" style="vertical-align:top;">
                    <jummp:displayModelDescriptionLabel>
                        <label for="${description}">
                            ${description}
                        </label>
                    </jummp:displayModelDescriptionLabel>
                </td>
                <td class="value" style="vertical-align:top;">
                    <div class="displayDescription">
                        <g:if test="${workingMemory["new_description"]}">
                            ${workingMemory["new_description"]}
                        </g:if>
                        <g:else>
                            ${revision.description}
                        </g:else>
                    </div>
                </td>
            </tr>
            <g:if test="${revision.model.publication?.validate()}">
                <tr class="prop">
                    <td class="name" style="vertical-align:top;">
                        <label for="${g.message(code: 'submission.summary.publication')}">
                            <g:message code="submission.summary.publication"/>
                        </label>
                    </td>
                    <td class="value" style="vertical-align:top;">
                        <div class="displayDescription">
                            <g:render  model="[model:model]" template="/templates/showPublication" />
                        </div>
                    </td>
                </tr>
            </g:if>
            <g:else>
                <tr class="prop">
                    <td class="name" style="vertical-align:top;">
                        <label for="${g.message(code: 'submission.summary.publication')}">
                            <g:message code="submission.summary.publication"/>
                        </label>
                    </td>
                    <td class="value" style="vertical-align:top;">
                        <div class="displayDescription">
                            No publication provided
                        </div>
                    </td>
                </tr>
            </g:else>
            <g:if test="${workingMemory.get("isUpdateOnExistingModel") as Boolean}">
                <tr class="prop">
                    <td class="name">
                        <label for="RevisionComments">
                            <g:message code="submission.summary.revisionLabel"/>
                        </label>
                    </td>
                    <td class="value">
                        <g:textArea name="RevisionComments" rows="5" cols="70"
                                    placeholder="Explain what you have updated"/>
                    </td>
                </tr>
            </g:if>
            </tbody>
        </table>--}%
    </div>
</div>
<input type="button" name="next" class="next action-button" value="Submit"/>
<input type="button" name="previous" class="previous action-button-previous" value="Previous"/>
