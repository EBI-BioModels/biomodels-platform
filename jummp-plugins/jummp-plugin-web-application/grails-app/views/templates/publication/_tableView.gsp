<table>
    <tr>
        <th>External ID or<br/>Link</th>
        <th>Title</th>
        <th>Journal</th>
        <th>Affiliation</th>
        <th>Abstract</th>
        <th>Year</th>
        <th>Month</th>
        <th>Date</th>
        <th>Volume</th>
        <th>Issue</th>
        <th>Pages</th>
    </tr>
    <tr>
        <sec:ifLoggedIn>
            <td><a href="${g.createLink(controller: "publication", action: "edit",
                params: [id: publication.id])}">${publication.link}</a></td>
            <td><a href="${g.createLink(controller: "publication", action: "edit",
                params: [id: publication.id])}">${publication.title}</a></td>
        </sec:ifLoggedIn>
        <sec:ifNotLoggedIn>
            <td>${publication.link}</td>
            <td>${publication.title}</td>
        </sec:ifNotLoggedIn>

        <td>${publication.journal}</td>
        <td>${publication.affiliation}</td>
        <td>${publication.synopsis}</td>
        <td>${publication.year}</td>
        <td>${publication.month}</td>
        <td>${publication.day}</td>
        <td>${publication.volume}</td>
        <td>${publication.issue}</td>
        <td>${publication.pages}</td>
    </tr>
</table>
