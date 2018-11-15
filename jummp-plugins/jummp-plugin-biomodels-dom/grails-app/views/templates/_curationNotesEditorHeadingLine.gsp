<h3><g:if test="${curationNotesTC.updated}">Update</g:if><g:else>Add</g:else> curation notes of the model
    <a href="${createLink(controller: "model", action: "show", id: id)}"
       title="Back to the model display page">[${model}]</a></h3>
