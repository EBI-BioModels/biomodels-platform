<div class="row">
    <div class="small-12 medium-12 large-12 columns">
        <p style="color: orange; font-size: x-large; font-weight: bold;">${monthString}, ${yearString}</p>
    </div>
</div>

<div class="row">
    <div class="small-12 medium-12 large-12 columns">
        <g:if test="${hrefToEditor}">
        <a href="${hrefToEditor}" target="_blank">
            <h5 class="text-left"><strong>${entryTitle}</strong></h5></a>
        </g:if>
        <g:else>
            <h5 class="text-left"><strong>${entryTitle}</strong></h5>
        </g:else>
        <p class="widget-body-text" style="text-align: left">${shortDescription}</p>
        <%
            List ids = models?.split(";") as List<String>
            int nbModels = ids?.size()
        %>
        <p class="text-left">Model(s) associated with this Model of the Month:
            <g:each in="${ids}" var="id" status="i">
                <g:if test="${i == nbModels-1}">
                    <a href="${createLink(controller: "model", action: "show", id: id)}">${id}</a>.
                </g:if>
                <g:else>
                    <a href="${createLink(controller: "model", action: "show", id: id)}">${id}</a>, &nbsp;
                </g:else>
            </g:each>
        </p>
        <p class="text-left">Last updated by: ${lastUpdatedBy}</p>
        <img class="thumbnail text-center" src="data:image/jpeg;base64,${previewImage}" title="${titlePreviewImage}"/>
    </div>
</div>
<div class="row small-12 medium-12 large-12 columns">
    <p style="text-align: left; font-size: 90%; padding-top: 0.75em"><a href="${momEntryLink}">Access this model of the
    month</a> |
        <a href="${momEntryLinkAll}">View all Model of the Month entries</a>
    <g:if test="${hrefToEditor}">
        | <a href="${hrefToEditor}" target="_blank">Update this entry</a>
    </g:if>
    </p>
</div>
