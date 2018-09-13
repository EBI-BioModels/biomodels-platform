<%
    String modelIds = ""
    if (entry?.models) {
        Set models = entry.models
        List ids = models.collect {
            it.publicationId ?: it.submissionId
        }
        modelIds = String.join(", ", ids)
    }
%>
<tr class="row">
    <td class="small-1 columns">${entry.authors}</td>
    <td class="small-2 columns">
        <a href="${g.createLink(controller: "modelOfTheMonth", action: "show",
        params: [id: entry.id])}">${entry.title}</a></td>
    <td class="small-4 columns">${entry.shortDescription}</td>
    <td class="small-1 columns">${entry.publicationDate}</td>
    <td class="small-1 columns">${entry.lastUpdated}</td>
    <td class="small-1 columns">${modelIds}</td>
</tr>
