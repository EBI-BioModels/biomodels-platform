<div class="row">
<div class="columns large-12 medium-12 small-12">
<hr style="border: 0"/>
<%
    def link = createLink(uri: "/cms/editor/edit/${id}")
%>
<input type="button" class="button" title="Click this button if you want to update this post!"
       value="Click here to edit" onclick='$.jummp.openPage("${link}")'/></p>
</div>
</div>
