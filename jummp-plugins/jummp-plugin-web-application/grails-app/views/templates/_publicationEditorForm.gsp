<g:form>
    <div class="dialog">
        <div class="small-6 medium-6 columns">
            <label>
                <g:message code="submission.publication.title"/>
            </label>
            <g:textField class="input50" name="title" value="${publication.title}"/>
            <label for="journal">
                <g:message code="submission.publication.journal"/>
            </label>
            <g:textField class="input50" name="journal" value="${publication.journal}"/>
            <label>
                <g:message code="submission.publication.authors"/>
                <select class="input50" id="authorList" name="authorList"
                        size="${authorListContainerSize}" style="height: inherit">
                    <g:each in="${publication.authors}">
                        <option value="${it.userRealName}|${it.orcid ?: ""}|${it.institution ?: ""}">${it.userRealName}</option>
                    </g:each>
                </select>
                Click to select an author to update or delete. Click Add button to add a new author into the list.
            </label>
            <div>
                <ul class="subListForm">
                    <li>
                        <label class="required">Name</label>
                        <span><input class="input40" size="40" type="text" id="newAuthorName"/></span>
                    </li>
                    <li>
                        <label style="display:block; margin-left:0px">ORCID</label>
                        <span>
                            <input class="input40" size="40" type="text" id="newAuthorOrcid"
                                   title="Enter your ORCID ID. For example, an ORCID profile link is often formed as http://orcid.org/0000-0002-2876-6046, therefore this profile's ORCID ID is 0000-0002-2876-6046"/>
                        </span>
                    </li>
                    <li hidden>
                        <label style="display:block;margin-left:0px">Institution</label>
                        <span><input class="input40" size="40" type="text" id="newAuthorInstitution"/></span>
                    </li>
                    <li>
                        <a href="#" id="addButton" class="button">Add</a>
                        <a href="#" id="updateButton" class="button">Update</a>
                        <a href="#" id="deleteButton" class="button">Delete</a>
                    </li>
                </ul>
            </div>
            <label>
                <g:message code="submission.publication.affiliation"/>
            </label>
            <g:textArea name="affiliation" id="affiliation" rows="5" cols="32"
                        value="${publication.affiliation}"/>
        </div>
        <div class="small-6 medium-6 columns">
            <label>
                <g:message code="submission.publication.synopsis"/>
            </label>
            <g:textArea name="synopsis" id="synopsis" rows="13" cols="32"
                        value="${publication.synopsis}"/>
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
                                      value="${publication.month?:Calendar.instance.get(Calendar.MONTH)}"/>
                            <g:select name="year" from="${1800..Calendar.instance.get(Calendar.YEAR)}"
                                      value="${publication.year?:Calendar.instance.get(Calendar.YEAR)}"/>
                        </span>
                    </li>
                    <li>
                        <label style="display:block;margin-left:0px">
                            <g:message code="submission.publication.volume"/></label>
                        <span>
                            <g:textField class="input20" name="volume" size="20"
                                         value="${publication.volume}"/>
                        </span>
                        <label style="display:block;margin-left:0px">
                            <g:message code="submission.publication.issue"/></label>
                        <span>
                            <g:textField class="input20" name="issue" size="20"
                                         value="${publication.issue}"/>
                        </span>
                    </li>
                    <li>
                        <label style="display:block;margin-left:0px">
                            <g:message code="submission.publication.pages"/></label>
                        <span>
                            <g:textField class="input20" name="pages" size="20"
                                         value="${publication.pages}"/>
                        </span>
                    </li>
                </ul>
            </div>
        </div>
    </div>
    <div class="small-12 medium-12 columns">
        <div class="buttons">
            <g:submitButton name="Cancel" class="button"
                            value="${g.message(code: 'submission.common.cancelButton')}" />
            <g:submitButton name="Back" class="button"
                            value="${g.message(code: 'submission.common.backButton')}" />
            <g:if test="${controllerName == "publication" && actionName == "show"}">
                <g:submitButton id="btnSave" name="Save" class="button"
                                value="${g.message(code: 'submission.publication.saveButton')}" />
            </g:if>
            <g:elseif test="${controllerName == "model"}">
                <g:submitButton id="continueButton" name="Continue" class="button"
                                value="${g.message(code: 'submission.publication.continueButton')}" />
            </g:elseif>
            <div name="authorListTemp" id="authorListTemp"
                 style="height: 50px; margin: auto; border: 3px solid #73AD21; display: none">
            </div>
        </div>
    </div>
</g:form>
