<tr>
    <td><a href="${g.createLink(controller: "tag", action: "show",
        params: [id: tag.id])}">${tag.name}</a></td>
    <td>${tag.description}</td>
    <td>${tag.userCreated}</td>
    <td>${tag.dateCreated}</td>
    <td>${tag.dateModified}</td>
</tr>
