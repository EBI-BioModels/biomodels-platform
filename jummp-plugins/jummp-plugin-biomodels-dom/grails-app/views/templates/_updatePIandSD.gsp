<div>
    <h3>Update the preview image and short description</h3>
    <p style="color: #c12e2a;">This procedure will try to locate the preview images and short descriptions of the entries from the old system, then update these entries in the new one. It will overwrite what have been recently updated on the existing entries. Please think carefully before pressing on the button below.</p>
    <button class="button"
            onclick="<g:remoteFunction controller="ModelOfTheMonth"
                                       action="updatePreviewImageAndShortDescription"
                                       name="updatePIandSD" update="updateReport"
                                       asynchronous="false"/>" >
                Click here to launch the update procedure</button>
    <div id="updateReport">

    </div>
</div>
