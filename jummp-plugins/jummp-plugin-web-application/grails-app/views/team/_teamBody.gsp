<g:form name="newTeamForm" action="save">
	<table class='responsive-table'>
        <thead><h2>General information</h2></thead>
		<tbody>
			<tr>
				<td class="tableLabels"><label class="required" for="name">Name</label></td>
				<td>
                    <g:textField required="true" value="${name}" autofocus="true" placeholder="Choose a team name"

                                 maxlength="255" id="teamName" name="name"/>
                </td>
			</tr>
			<tr>
				<td class="tableLabels"><label for="teamDescription">Description</label></td>
				<td>
                    <g:textField value="${description}" id="teamDescription"
                                 placeholder="Give a bit more details of your team"
                                 name="description" maxlength="255" />
                </td>
			</tr>
		</tbody>
	</table>
	<div id="membersContainer" class='spaced'>
		<h2>Team Members</h2>
		<div class="row">
            <div class="small-12 medium-12 columns">
                <label for="nameSearch" class="required">User</label>
            </div>
            <div class="small-10 medium-10 columns">
                <input placeholder="Name, username or email" id="nameSearch" name="nameSearch" type="text"/>
            </div>
            <div class="small-2 medium-2 columns">
                <g:field type="button" class="button" name="add" value="Add"/>
            </div>
		</div>
		<span class="tip">
			<span class='tipNote'>Tip:</span>
			Choose the collaborator, then press enter to add them to this team.
		</span>
        <h3 id="flashMessage" hidden>Warning Message</h3>
		<div class="spaced" id="members">
			<table id="membersTable">
				<thead id='nameLabel'>
					<tr>
						<th>Name</th>
						<th>&nbsp;</th>
					</tr>
				</thead>
				<tbody id="membersTableBody">
					<!-- ADD MEMBERS HERE. -->
				</tbody>
			</table>
		</div>
	</div>

	<g:field type="button" class='submitButton button' name="${buttonLabel}" value="${buttonLabel}"/>
</g:form>
