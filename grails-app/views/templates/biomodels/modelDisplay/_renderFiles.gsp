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
        if (revision.state == ModelState.PUBLISHED && deployTarget != "local") {
            // get download link from EBI BioModels public FTP
            String EBI_BM_FTP = "${BioModels.EBI_BM_PUBLIC_FTP}/repository"
            String filePath = "${revision.model.submissionId}/${revision.revisionNumber}/${file.filename}"
            downloadLink = "${EBI_BM_FTP}/${modelParentFolder}/$filePath"
        }

    %>
    <tr>
        <td>${file.filename}</td>
        <td>${file.description}</td>
        <td>${fileSize}</td>
        <td><a id="previewButton${index++}" data-file-mime-type="${file.mimeType}"
               data-file-name="${file.filename}"
               data-download-link="${previewLink}"
               data-preview="${file.showPreview}"
               data-open="filePreviewBox">Preview</a> |
            <a href="${downloadLink}" style="text-decoration: none">Download</a></td>
    </tr>
</g:each>
