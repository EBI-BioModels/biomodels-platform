<%
    def url = g.createLink(controller: "conversion", action: "download", id: fileTC.revision.model.submissionId)
    url += "?revisionId=${fileTC.revision.revisionNumber}&fileName=${fileTC.path}&mimeType=${fileTC.mimeType}"
%>
<li rel="file">
    <a class="pointerhere" href="${url}">Download the model as ${fileTC.mimeType} format</a> (auto-generated)
    <br/> ${fileTC.description}</li>