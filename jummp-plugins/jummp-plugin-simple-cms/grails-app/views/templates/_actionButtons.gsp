<div class="row">
<div class="columns large-12 medium-12 small-12">
<hr style="border: 0"/>
<%
    def dashboardLink = createLink(uri: "/cms/content")
    def editLink = createLink(uri: "/cms/editor/edit/${id}")
%>
<p>
    <input type="button" class="button"
           title="Click this button if you want to go back to the dashboard (i.e., Post Browser)!"
           value="Dashboard" onclick='$.jummp.openPage("${dashboardLink}")'/>
    <input type="button" class="button" title="Click this button if you want to update this post!"
           value="Click here to edit" onclick='$.jummp.openPage("${editLink}")'/></p>
</div>
</div>
