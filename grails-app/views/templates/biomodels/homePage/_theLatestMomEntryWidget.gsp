<div class="row">
    <div class="small-12 medium-12 large-12 columns">
        <p style="color: orange; font-size: x-large; font-weight: bold;">${monthString},
        &nbsp;${yearString}</p>
    </div>
</div>

<div class="row">
    <div class="small-12 medium-12 large-12 columns">
        <h5 class="text-left">${entryTitle}</h5>
        <p class="widget-body-text" style="text-align: left">${shortDescription}</p>
        <p class="text-left">Model(s) associated with this Model of the Month:
            <g:each in="${models?.split(";")}" var="id">
                <a href="${createLink(controller: "model", action: "show", id: id)}">${id}</a>&nbsp;
            </g:each>
        </p>
        <p class="text-left">Last updated by: ${lastUpdatedBy}</p>
        <img class="thumbnail text-center" src="data:image/jpeg;base64,${previewImage}" title="${titlePreviewImage}"/>
    </div>
</div>
<div class="row small-12 medium-12 large-12 columns">
    <p style="text-align: left; font-size: 90%; padding-top: 0.75em"><a href="${momEntryLink}">Access this model of the
    month</a> |
        <a href="${momEntryLinkAll}">View all Model of the Month entries</a></p>
</div>
