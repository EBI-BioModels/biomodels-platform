<div class="row contributors-body ${cont.external ? 'external-contributor' : ''}">
    <div class="columns large-3 medium-3 small-12 text-center middle">
        <div class="thumbnail" style="border: none !important">
            <img src="${serverURL}/images/default-user-avatar.png" style="width: 30%">
        </div>
    </div>
    <div class="columns large-5 medium-5 small-12">
        <h4 class="user-real-name">
            <g:if test="${cont}">
            <a href="${createLink(controller: "usermanagement", action: "profile", params: [username: cont.user.username])}"
               target="_blank">${cont.person.userRealName}</a>
            </g:if>
            <g:else>
            <span style="color: black">${displayName}</span>
            </g:else>
        </h4>
        <p class="username-email">
        <g:if test="${cont}">
            ${cont.user.username.startsWith("ext_") ?
                (cont.person.orcid ? (cont.person.orcid + ", ") : "") :
                (cont.user.username + ", ")}${cont.user.email}
        </g:if>
        <g:else>
            ${orcid ? orcid + ", " + email : email}
        </g:else></p>
    </div>
    <div class="columns large-2 medium-2 small-12">
        <select name="role" required id="role" class="form-control">
            <g:each in="${roles}" var="role">
                <option value="${role}"
                    <g:if test="${role == cont.role.name}">selected="selected"</g:if>
                >${role}</option>
            </g:each>
        </select>
    </div>
    <div class="columns large-2 medium-2 small-12">
        <g:if test="${cont}">
        <p id="contributor-remove" class="contributor-remove button <g:if test="${cont.locked}">locked secondary</g:if><g:else>unlocked</g:else>">
            <g:if test="${cont.locked}"><del>Remove</del></g:if><g:else>Remove</g:else></p>
        </g:if>
        <g:else>
            <p id="contributor-remove" class="contributor-remove button">Remove</p>
        </g:else>
    </div>

</div>
