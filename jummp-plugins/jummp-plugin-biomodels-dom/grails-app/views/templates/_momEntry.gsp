<%
    def previewImage = Base64.encoder.encodeToString(entry.previewImage)
%>
<div class="row">
    <div class="small-2 columns">${entry.authors}</div>
    <div class="small-2 columns">${entry.title}</div>
    <div class="small-4 columns">${entry.shortDescription}</div>
    <div class="small-2 columns">
        <img src="data:image/jpeg;base64,${previewImage}"/></div>
    <div class="small-2 columns">${entry.lastUpdated}</div>
</div>
