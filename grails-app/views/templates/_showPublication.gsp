<%@ page import="net.biomodels.jummp.model.PublicationLinkProvider" %>
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

<%
    def publication = model.publication
    String publicationLinkLabel = ""
    String publicationLink = ""
    if (publication.link) {
        publicationLinkLabel = ", ${publication.linkProvider.linkType}: ${publication.link}"
        String identifiersPrefix = publication.linkProvider.identifiersPrefix
        publicationLink = identifiersPrefix ? "$identifiersPrefix$publication.link" : publication.link
    } else {
        publicationLinkLabel = "Preprint"
        publicationLink = createLink(controller: "publication", action: "show", id: publication.id)
    }
%>
<div>
    <style>
        .hiddenContent {display:none;}
    </style>
    <ul style="list-style-type: none;margin: 0px;padding:0px;">
    <li><a title="View publication - this will open an external site"
           class="publicationLink" href="${publicationLink}" target="_blank">
        <strong>${publication.title}</strong></a>
        <a class="expander" title="Click to see more" href="#">
            <span>
                <img style="width:12px;margin:2px;float:none"
                     src="${grailsApplication.config.grails.serverURL}/images/expand.png"/>
            </span>
        </a>
        <g:if test="${canUpdate}">
        &nbsp;
        <a href="${createLink(controller: "publication", action: "edit", id: publication.id)}"
           class="button" style="color: white">Edit</a></g:if>
    </li>
    <li>${publication.authors.collect{"${it.userRealName}"}.join(", ")}</li>
    <li><i>${publication.journal}</i>
        ${publication.month?", ${publication.month}/":""}
        ${publication.year?"${publication.year}":""}
        ${publication.volume?", Volume ${publication.volume}":""}
        ${publication.issue?", Issue ${publication.issue}":""}
        ${publication.pages?", pages: ${publication.pages}":""}
        ${publicationLinkLabel}
    </li>
    <div class="hiddenContent">
        <g:if test="${publication.affiliation}">
            <li><label>Affiliation: </label> ${publication.affiliation}</li>
        </g:if>
        <g:if test="${publication.synopsis}">
            <li><label>Abstract: </label>${publication.synopsis}</li>
        </g:if>
    </div>
    </ul>
    <g:javascript contextPath="" src="simple-expand.js"/>
    <script>
        $('.expander').simpleexpand({'defaultTarget':'div.hiddenContent'});
    </script>
</div>
