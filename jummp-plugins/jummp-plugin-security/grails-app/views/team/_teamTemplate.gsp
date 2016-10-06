<g:javascript src="underscore-min.js" plugin="jummp-plugin-web-application"/>
<g:javascript src="handlebars.min.js" plugin="jummp-plugin-web-application"/>
<g:javascript src="backbone-min.js" plugin="jummp-plugin-web-application"/>
<script src="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: "/js", file: 'share.js')}"></script>
<script id="team-member-template" type="text/x-handlebars-template">
       <td>{{this.name}}</td>
       <td><button type='button' id='remove-{{this.userId}}' class='remove'>Remove</button></td>
</script>
<script id="team-members-template" type="text/x-handlebars-template">
		{{#if isEmpty}}
			<p>Use the search box to add collaborators to this team.</p>
		{{else}}
			<table id="membersTable">
				<thead>
					<tr>
						<th>Name</th>
						<th>&nbsp;</th>
					</tr>
				</thead>
				<tbody>
					{{#each}}
						<tr>
							<td>{{this.name}}</td>
							<td><button id='remove-{{this.id}}' data-id= "{{this.id}}" class='.remove'></button></td>
						</tr>
					{{/each}}
				</tbody>
			</table>
		{{/if}
</script>
<g:javascript src="teams.js" plugin="jummp-plugin-security"/>