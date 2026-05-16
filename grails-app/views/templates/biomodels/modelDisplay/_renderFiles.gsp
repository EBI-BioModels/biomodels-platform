<%@ page import="net.biomodels.jummp.core.constants.BioModels" %>
<%@ page import="net.biomodels.jummp.core.model.ModelState" %>
<%@ page import="net.biomodels.jummp.utils.DisplayFormat" %>

<% int index = 1 %>
<g:each in="${repoFiles}" var="file">
    <%
        String fileSize = DisplayFormat.format((double)file.size, 2)
        boolean isBigFile = file.size >= BioModels.MAX_FILE_SIZE
        String previewLink = createLink(controller: 'model',
            action: 'download', params: [id: revision.identifier(), filename: file.filename])
        String downloadLink = previewLink
        if (deployTarget != "local") {
            String filePath = "${revision.model.submissionId}/${revision.revisionNumber}/${file.filename}"
            downloadLink = "${grailsApplication.config.jummp.model.download.server}/get-files/${filePath}"
        }
    %>
    <tr>
        <td>${file.filename}</td>
        <td>${file.description}</td>
        <td>${fileSize}</td>
        <td><a id="previewButton${index++}" data-file-mime-type="${file.mimeType}"
               data-file-name="${file.filename}"
               data-preview-link="${previewLink}"
               data-preview="${file.showPreview}"
               data-is-big-file = "${isBigFile}"
               data-download-link = "${downloadLink}"
               data-open="filePreviewBox">Preview</a> |
            <a href="${downloadLink}" style="text-decoration: none">Download</a></td>
    </tr>
</g:each>
