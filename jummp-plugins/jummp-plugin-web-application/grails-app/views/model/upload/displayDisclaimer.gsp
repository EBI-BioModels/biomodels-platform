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
<%
    String style = session['branding.style']
    String faqURI = g.createLink(uri: '/faq', absolute: true)
    String contactURI = g.createLink(uri: '/contact', absolute: true)
    def createMsgArgs = [contactURI, faqURI]
%>
    <head>
        <meta name="layout" content="${style}/main" />
        <title>
        	<g:if test="${isUpdate}">
        	      <g:message code="submission.disclaimer.update.title" args="${ [params.id] }" />
        	</g:if>
        	<g:else>
        	      <g:message code="submission.disclaimer.create.title"/>
        	</g:else>
        </title>
        <script type="text/javascript">
            $(document).ready(function() {
                $('#creatorExample').click(function () {
                    $("#dialog").dialog({
                        width: 659,
                        height: 755
                    });
                });
            });
        </script>
    </head>
    <body>
        <h2>Submission Guidelines</h2>
        <g:if test="${isUpdate}">
        	<g:message code="submission.disclaimer.updateMessage" args="${ [params.id] }" />
        </g:if>
        <g:else>
            <g:if test="${'default' != style}">
                <g:message code="submission.disclaimer.${style}.createMessage" args="${createMsgArgs}"/>
            </g:if>
            <g:else>
                <g:message code="submission.disclaimer.default.createMessage"/>
            </g:else>
        </g:else>
        <g:form>
            <div class="dialog">
                <div class="buttons">
                    <g:submitButton name="Cancel" class="button" value="${g.message(code: 'submission.common.cancelButton')}" />
                    <g:submitButton name="Continue" class="button" value="${g.message(code: 'submission.disclaimer.continueButton')}" />
                </div>
            </div>
        </g:form>
        <div id="dialog" title="SBML creator sample" hidden>
            <p><g:message code="submission.disclaimer.biomodels.guidelines.sbmlexample"/></p>
        </div>
    </body>
    <g:render template="/templates/decorateSubmission" />
    <g:render template="/templates/subFlowContextHelp" />

