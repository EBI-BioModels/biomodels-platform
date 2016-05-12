<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.core.model.ModelTransportCommand"%>
    <head>
        <meta name="layout" content="main"/>
        <title>
            <g:if test="${isUpdate}">
                <g:message code="submission.publicationInfoPage.update.title" args="${ [params.id] }" />
            </g:if>
            <g:else>
                <g:message code="submission.publicationInfoPage.create.title"/>
            </g:else>
        </title>
        <link rel="stylesheet"
              href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: '/css', file: 'publicationPageStyle.css')}" />
        <g:javascript contextPath="" src="publicationSubmission.js"/>
    </head>
    <body>
        <h2>Update Publication Information</h2>
        <g:form>
            <div class="dialog">
                <table class="formtable">
                    <tbody>
                        <tr class="prop">
                            <td>
                                <label>
                                    <g:message code="submission.publication.title"/>
                                </label>
                            </td>
                            <td>
                                <g:textField class="input50" name="title" size="50"
                                             value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.title}"/>
                            </td>
                            <td>
                                <label for="journal">
                                    <g:message code="submission.publication.journal"/>
                                </label>
                            </td>
                            <td>
                                <g:textField class="input50" name="journal" size="50"
                                             value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.journal}"/>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <label>
                                    <g:message code="submission.publication.authors"/>
                                </label>
                            </td>
                            <td>
                            <select class="input50" id="authorList" name="authorList" size="${workingMemory.get("Authors")?.size() ?: 5}">
                                <g:each in="${workingMemory.get("Authors")}">
                                    <option value="${it.userRealName}<init>${it.orcid ?: "no_orcid"}<init>${it.institution ?: "no_institution_provided"}">${it.userRealName}</option>
                                </g:each>
                            </select>
                            <div>
                                <ul class="subListForm">
                                    <li>
                                        <label style="display:block;margin-left:0px">Name</label>
                                        <span><input class="input40" size="40" type="text" id="newAuthorName"/></span>
                                    </li>
                                    <li>
                                        <label style="display:block;margin-left:0px">ORCID</label>
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
                                        %{--<a href="#" id="updateButton" class="button">Update</a>--}%
                                        <a href="#" id="deleteButton" class="button">Delete</a>
                                    </li>
                                </ul>
                            </div>

                            </td>
                            <td>
                                <label>
                                    <g:message code="submission.publication.synopsis"/>
                                </label>
                            </td>
                            <td>
                                <g:textArea name="synopsis" id="synopsis" rows="13" cols="32" value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.synopsis}"/>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <label>
                                    <g:message code="submission.publication.affiliation"/>
                                </label>
                            </td>
                            <td>
                                <g:textArea name="affiliation" id="affiliation" rows="5" cols="32" value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.affiliation}"/>
                            </td>
                            <td>
                                <label>
                                    <g:message code="submission.publication.pubDetails"/>
                                </label>
                            </td>
                            <td>
                                <div>
                                    <ul class="subListForm">
                                        <li>
                                            <label style="display:block;margin-left:0px"><g:message code="submission.publication.date"/></label>
                                            <span>
                                                <g:select name="month" from="${1..12}" value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.month?:Calendar.instance.get(Calendar.MONTH)}"/>
                                                <g:select name="year" from="${1800..Calendar.instance.get(Calendar.YEAR)}" value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.year?:Calendar.instance.get(Calendar.YEAR)}"/>
                                            </span>
                                        </li>
                                        <li>
                                            <label style="display:block;margin-left:0px"><g:message code="submission.publication.volume"/></label>
                                            <span>
                                                <g:textField class="input20" name="volume" size="20" value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.volume}"/>
                                            </span>
                                        </li>
                                        <li>
                                            <label style="display:block;margin-left:0px"><g:message code="submission.publication.issue"/></label>
                                            <span>
                                                <g:textField class="input20" name="issue" size="20" value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.issue}"/>
                                            </span>
                                        </li>
                                        <li>
                                            <label style="display:block;margin-left:0px"><g:message code="submission.publication.pages"/></label>
                                            <span>
                                                <g:textField class="input20" name="pages" size="20" value="${(workingMemory.get("ModelTC") as ModelTransportCommand).publication.pages}"/>
                                            </span>
                                        </li>
                                    </ul>
                                </div>
                            </td>
                        </tr>
                        </table>
                  <div class="buttons">
                    <g:submitButton name="Cancel" value="${g.message(code: 'submission.common.cancelButton')}" />
                    <g:submitButton name="Back" value="${g.message(code: 'submission.common.backButton')}" />
                    <g:submitButton name="Continue" value="${g.message(code: 'submission.publication.continueButton')}" />
                    <g:hiddenField name="authorFieldTotal" value="" />
                </div>
            </div>
        </g:form>
    </body>
    <g:render template="/templates/decorateSubmission" />
