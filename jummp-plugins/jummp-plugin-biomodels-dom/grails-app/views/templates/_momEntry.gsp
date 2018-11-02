<tr>
    <td>${entry.authors}</td>
    <td><a href="${g.createLink(controller: "modelOfTheMonth", action: "show",
        params: [id: entry.id])}">${entry.title}</a></td>
    <td>${entry.shortDescription}</td>
    <td>${entry.publicationDate}</td>
    <td>${entry.lastUpdated}</td>
    <td>${entry.models}</td>
</tr>
