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
                <g:if test="${canUpdate || hasCuratorRole || hasAdminRole}">
                    <a class="versionToggleMinor" data-revision-id="${rv.identifier()}"
                       data-revision-number="${rv.revisionNumber}" data-minor="${rv.minorRevision}"
                       title="${rv.minorRevision ? 'unmark' : 'mark'} version ${rv.revisionNumber} as a minor revision"
                       href="${g.createLink(controller: 'model', action: 'toggleMinorRevision', id: rv.identifier())}">
                        <i class="icon icon-common ${rv.minorRevision ? 'icon-times' : 'icon-flag'}"
                           title="${rv.minorRevision ?
                               'This is flagged as a minor revision - click to unflag it' :
                               'Flag this as a minor revision, e.g. a small non-scientific correction'}"></i>
                    </a>
                </g:if>
                <g:if test="${hasCuratorRole || hasAdminRole}">
                    <span class="versionDeleteWrapper" data-revision-id="${rv.identifier()}"
                          ${rv.minorRevision ? '' : 'hidden="hidden"'}>
                        <a class="versionDelete" title="delete minor revision ${rv.revisionNumber}"
                           href="${g.createLink(controller: 'model', action: 'deleteRevision', id: rv.identifier())}"
                           onclick="return confirm('Are you sure you want to permanently delete minor revision ${rv.revisionNumber}?')">
                            <i class="icon icon-common icon-trash" title="Delete this minor revision"></i>
                        </a>
                    </span>
                </g:if>
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

<script type="text/javascript">
    /* global $, toastr, fetch */
    // Flips Revision.minorRevision in place via fetch() instead of a full page reload, so the
    // History tab stays put. See ModelController.toggleMinorRevision, which always answers JSON.
    (function () {
        "use strict";
        $(document).on("click", ".versionToggleMinor", function (e) {
            e.preventDefault();
            var $link = $(this);
            if ($link.data("busy")) {
                return;
            }
            var url = $link.attr("href");
            var $icon = $link.find("i");
            var revisionId = $link.data("revision-id");
            var $deleteWrapper = $(".versionDeleteWrapper[data-revision-id='" + revisionId + "']");
            $link.data("busy", true);
            fetch(url, {
                method: "POST",
                headers: {"X-Requested-With": "XMLHttpRequest"}
            }).then(function (result) {
                return result.json().then(function (data) {
                    return {status: result.status, data: data};
                });
            }).then(function (res) {
                var data = res.data;
                if (res.status !== 200 || !data.success) {
                    toastr.error(data.message || "Could not update this revision.");
                    return;
                }
                var isMinor = data.minorRevision;
                $link.attr("data-minor", isMinor);
                $link.attr("title", (isMinor ? "unmark" : "mark") + " version " +
                    data.revisionNumber + " as a minor revision");
                $icon.removeClass("icon-flag icon-times").addClass(isMinor ? "icon-times" : "icon-flag");
                $icon.attr("title", isMinor ?
                    "This is flagged as a minor revision - click to unflag it" :
                    "Flag this as a minor revision, e.g. a small non-scientific correction");
                if ($deleteWrapper.length) {
                    $deleteWrapper.prop("hidden", !isMinor);
                }
                toastr.success(data.message);
            }).catch(function () {
                toastr.error("Bad Server Response");
            }).finally(function () {
                $link.data("busy", false);
            });
        });
    }());
</script>
