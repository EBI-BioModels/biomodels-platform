<div class="row contributors-body">
    <div class="columns large-1 medium-1 small-12">
        &nbsp;
    </div>
    <div class="columns large-2 medium-2 small-12 text-center middle">
        <div class="thumbnail">
            <img src="${serverURL}/images/default-user-avatar.png" style="width: 30%">
        </div>
    </div>
    <div class="columns large-5 medium-5 small-12">
        <h4 class="user-real-name">
            <a href="${createLink(controller: "usermanagement", action: "profile", params: [username: cont.user.username])}"
               target="_blank">${cont.person.userRealName}</a></h4>
        <p class="username-email">${cont.user.username}, ${cont.user.email}</p>
    </div>
    <div class="columns large-2 medium-2 small-12">
        <select name="role" required id="role" class="form-control">
            <g:each in="${roles}" var="role">
                <option value="${role}"
                        <g:if test="${role.equals(cont.role.name)}">selected="selected" test="${cont.role.name}"</g:if>
                >${role}</option>
            </g:each>
        </select>
    </div>
    <div class="columns large-1 medium-1 small-12">
        <p id="contributor-remove" class="contributor-remove button <g:if test="${cont.locked}">locked secondary</g:if><g:else>unlocked</g:else>">
            <g:if test="${cont.locked}"><del>Remove</del></g:if><g:else>Remove</g:else></p>
    </div>
    <div class="columns large-1 medium-1 small-12">
        &nbsp;
    </div>
</div>
