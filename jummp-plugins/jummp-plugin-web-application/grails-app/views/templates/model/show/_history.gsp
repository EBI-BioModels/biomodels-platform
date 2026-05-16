<%@ page import="grails.converters.JSON; java.text.DateFormat"%>
<%@ page import="net.biomodels.jummp.core.model.ModelState"%>

<div class="row">
<div class="small-12 medium-12 large-12 columns">
    <% DateFormat dateFormat = DateFormat.getDateTimeInstance(); %>
    <ul>
        <li>Model originally submitted by : ${revision.model.submitter}</li>
        <li>Submitted: ${dateFormat.format(allRevs.first().uploadDate)}</li>
        <li>Last Modified: ${dateFormat.format(allRevs.last().uploadDate)}</li>
    </ul>
    <h5>Revisions</h5>
    <ul>
        <g:each status="i" var="rv" in="${allRevs.sort{a,b -> a.revisionNumber > b.revisionNumber ? -1 : 1}}">
            <li style="${revision.id == rv.id ?"background-color:#FFFFCC;":""}margin-top:5px">
                Version: ${rv.revisionNumber}
                <g:if test="${rv.state==ModelState.PUBLISHED}">
                    <i class="icon icon-common icon-unlock" title="This version of the model is public"></i>
                </g:if>
                <g:else>
                    <i class="icon icon-common icon-lock" title="This version of the model is unpublished"></i>
                </g:else>
                <g:if test="${revision.id != rv.id}">
                    <a class="versionDownload" title="go to version ${rv.revisionNumber}" target="_blank"
                       href="${g.createLink(controller: 'model', action: 'show', id: rv.identifier())}">
                        <i class="icon icon-common icon-external-link-alt"></i>
                    </a>
                </g:if>
                <a class="versionDownload" title="download" target="_blank"
                   href="${g.createLink(controller: 'model', action: 'download', id: rv.identifier())}">
                    <i class="icon icon-common icon-download" title="Download this version"></i>
                </a>
                <ul>
                    <li>Submitted on: ${dateFormat.format(rv.uploadDate)}</li>
                    <li>Submitted by: ${rv.owner}</li>
                    <li>With comment: ${rv.comment}</li>
                </ul>
            </li>
        </g:each>
    </ul>
    <g:if test="${allRevs.size() > 1}">
        <p style="font-style: italic; font-size: smaller">(*) You might be seeing discontinuous
        revisions as only public revisions are displayed here. Any private revisions
            <i class="icon icon-common icon-lock" style="font-size: large"></i>
            of this model will only be shown to the submitter and their collaborators.</p>
    </g:if>
</div>
</div>
