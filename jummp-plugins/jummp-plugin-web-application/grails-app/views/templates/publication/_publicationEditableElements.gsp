<%@ page import=" net.biomodels.jummp.model.PublicationLinkProvider" %>
<%
    linkSourceTypes = PublicationLinkProvider.LinkType.values().collect { it.label }
%>
<div class="editablePart row">
<div class="small-6 medium-6 columns">
    <g:if test="${controllerName == "publication" && actionName in ["add", "edit"]}">
    <div class="row">
        <div class="small-12 medium-4 large-4 columns">
            <label for="PubLinkProvider" class="required">Source</label>
            <g:select name="PubLinkProvider" id="linkProvider"
                      from="${linkSourceTypes}"
                      value="${publication?.linkProvider?.linkType}"
                      noSelection="['':'- No publication available -']"/>
        </div>
        <div class="small-12 medium-8 large-8 columns">
            <label for="link" class="required">Link</label>
            <g:textField class="input25" name="link" value="${publication?.link}"/></div></div>
    </g:if>
    <label class="required" for="title">
        <g:message code="submission.publication.title"/>
    </label>
    <g:textField class="input50" name="title" id="title" value="${publication?.title}"/>
    <p class="help-text" id="titleHelp" style="color: red">&nbsp;</p>
    <label for="journal" class="required">
        <g:message code="submission.publication.journal"/>
    </label>
    <g:textField class="input50" name="journal" id="journal" value="${publication?.journal}"/>
    <label class="required" for="authorList">
        <g:message code="submission.publication.authors"/></label>
    <div class="row">
        <div class="small-10 medium-9 columns">
            <select id="authorList" name="authorList"
                    size="${authorListContainerSize}" style="height: inherit">
                <g:each in="${publication?.authors}">
                    <option value="${it.id}|${it.userRealName}|${it.orcid ?: ""}|${it.institution ?: ""}"
                            data-person-id="${it.id}"
                            data-person-realname="${it.userRealName}"
                            data-person-orcid="${it.orcid ?: ""}"
                            data-person-institution="${it.institution ?: ""}">${it.userRealName}</option>
                </g:each>
            </select>
        </div>
        <div class="small-2 medium-3 columns text-left">
            <i class="icon icon-common icon-arrow-up button btn-up re-ordering-author"
               data-name="Up"></i>
            <br/>
            <i class="icon icon-common icon-arrow-down button btn-down re-ordering-author"
               data-name="Down"></i>
        </div>
    </div>
    Enter new author name and ORCID, then click <strong>Add</strong> button to add a new author into the list.
Select any author in the list so as to <strong>Update</strong> or <strong>Delete</strong>. <strong>Notes: </strong>It
must have at least one author. Enter your ORCID ID. For example, an ORCID profile link is often formed as
https://orcid.org/0000-0001-8479-0262, therefore this profile's ORCID ID is 0000-0001-8479-0262.
    <div>
        <ul class="subListForm">
            <div class="row">
                <div class="small-6 medium-6 columns">
                    <li>
                        <label class="required" for="newAuthorName">Name</label>
                        <span><input class="input40" size="40" type="text" id="newAuthorName"/></span>
                    </li>
                </div>
                <div class="small-6 medium-6 columns">
                    <li>
                        <label style="display:block; margin-left:0px">ORCID</label>
                        <span>
                            <input class="input40" size="40" type="text" id="newAuthorOrcid"
                                   title="Enter your ORCID ID. For example, an ORCID profile link is often formed as https://orcid.org/0000-0001-8479-0262, therefore this profile's ORCID ID is 0000-0001-8479-0262"
                                   placeholder="Ex: 0000-0001-8479-0262"/>
                        </span>
                    </li>
                </div>
            </div>
            <li>
                <label for="newAuthorInstitution"
                       style="display:block;margin-left:0px">Institution</label>
                <span><input class="input40" size="40" type="text" id="newAuthorInstitution"/></span>
                <p class="help-text" id="institutionHelp" style="color: red">&nbsp;</p>
            </li>
            <li>
                <a href="#" id="addButton" class="button">Add</a>
                <a href="#" id="updateButton" class="button">Update</a>
                <a href="#" id="deleteButton" class="button">Delete</a>
            </li>
        </ul>
    </div>
    <label for="affiliation" class="required">
        <g:message code="submission.publication.affiliation"/>
    </label>
    <g:textArea name="affiliation" id="affiliation" rows="5" cols="32"
                value="${publication?.affiliation}"/>
    <p class="help-text" id="affiliationHelp" style="color: red">&nbsp;</p>
</div>
<div class="small-6 medium-6 columns">
    <label class="required" for="synopsis">
        <g:message code="submission.publication.synopsis"/>
    </label>
    <g:textArea name="synopsis" id="synopsis" rows="13" cols="32"
                value="${publication?.synopsis}"/>
    <p class="help-text" id="synopsisHelp" style="color: red">&nbsp;</p>
    <label>
        <g:message code="submission.publication.pubDetails"/>
    </label>
    <div>
        <ul class="subListForm">
            <li>
                <label style="display: block; margin-left:0px">
                    <g:message code="submission.publication.date"/></label>
                <span>
                    <g:select name="month" from="${1..12}"
                              value="${publication?.month?:Calendar.instance.get(Calendar.MONTH)}"/>
                    <g:select name="year" from="${1800..Calendar.instance.get(Calendar.YEAR)}"
                              value="${publication?.year?:Calendar.instance.get(Calendar.YEAR)}"/>
                </span>
            </li>
            <li>
                <label style="display:block;margin-left:0px">
                    <g:message code="submission.publication.volume"/></label>
                <span>
                    <g:textField class="input20" name="volume" size="20"
                                 value="${publication?.volume}"/>
                </span>
                <label style="display:block;margin-left:0px">
                    <g:message code="submission.publication.issue"/></label>
                <span>
                    <g:textField class="input20" name="issue" size="20"
                                 value="${publication?.issue}"/>
                </span>
            </li>
            <li>
                <label style="display:block;margin-left:0px">
                    <g:message code="submission.publication.pages"/></label>
                <span>
                    <g:textField class="input20" name="pages" size="20"
                                 value="${publication?.pages}"/>
                </span>
            </li>
        </ul>
    </div>
</div>
</div>
