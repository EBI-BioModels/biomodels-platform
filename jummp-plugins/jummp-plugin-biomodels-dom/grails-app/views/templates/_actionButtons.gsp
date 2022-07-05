<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 27/06/2022
  Time: 16:05
--%>
<sec:ifLoggedIn>
<g:if test="${canEdit}">
    <a id="btnEdit" class="button btn-primary" href="<g:createLink uri="/cms/editor/edit/$id"/>">Edit</a>
</g:if>
</sec:ifLoggedIn>
