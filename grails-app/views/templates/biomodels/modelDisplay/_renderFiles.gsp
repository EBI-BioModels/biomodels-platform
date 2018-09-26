<%@ page import="net.biomodels.jummp.utils.DisplayFormat" %>
<% int index = 1 %>
<g:each in="${repoFiles}" var="file">
    <%
        String fileSize = DisplayFormat.format((double)file.size, 2)
        String downloadLink = createLink(controller: 'model',
            action: 'download', id: revision.identifier())
        downloadLink += "?filename=${file.filename}"
    %>
    <tr>
        <td>${file.filename}</td>
        <td>${file.description}</td>
        <td>${fileSize}</td>
        <td><a id="previewButton${index++}" data-file-mime-type="${file.mimeType}"
               data-file-name="${file.filename}"
               data-download-link="${downloadLink}"
               data-preview="${file.showPreview}"
               data-open="filePreviewBox">Preview</a> |
            <a href="${downloadLink}" style="text-decoration: none">Download</a></td>
    </tr>
</g:each>
