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

<nav>
<ul id="local-nav" class="main-menu dropdown menu"
    data-description="navigational" data-dropdown-menu role="menubar"%>
    <li <g:if test="${actionName == null}"> class="first active" </g:if> role="menuitem">
        <a href="${createLink(uri: '/', absolute: true)}" title="Back to BioModels homepage">
            <i class="icon icon-generic" data-icon="H"></i> Home</a>
    </li>
    <%
        boolean selectedSupportItems = g.pageProperty(name:'page.faq')?.length() || g.pageProperty(name:'page.courses')?.length()
        boolean selectedBrowseItems = g.pageProperty(name: 'page.search')?.length() || g.pageProperty(name: 'page.goChart')?.length()
        boolean selectedAboutusItems = g.pageProperty(name:'page.termsOfUse')?.length() ||
            g.pageProperty(name:'page.citation')?.length() ||
            g.pageProperty(name:'page.news')?.length() ||
            g.pageProperty(name:'page.acknowledgements')?.length() ||
            g.pageProperty(name:'page.jobs')?.length()
    %>
    <li <g:if test="${selectedBrowseItems}"> class="active" </g:if> role="menuitem">
        <a><i class="icon icon-common" data-icon="b"></i> Browse</a>
        <ul class="menu">
            <li><a href="${g.createLink(controller: 'search', action: 'search', params: [query: '*:*'])}">All
            models</a></li>
            <li><a
                href="${g.createLink(controller: 'goChart', action: 'index', plugin: 'jummp-plugin-biomodels-dom')}">GO categories</a>
            </li>
            <li><a
                href="${g.createLink(controller: 'parameterSearch', action: 'index', plugin: 'jummp-plugin-biomodels-dom')}">Parameter Search</a>
            </li>
            <li><g:link mapping="agedbrain">Neurodegeneration models</g:link></li>
            <li><g:link mapping="path2models">Path2Models models</g:link></li>
            <li>
                <a href="${g.createLink(controller: 'pdgsmm', action: 'index')}">
                    PDGSM models
                </a>
            </li>
        </ul>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.submit')?.length()}"> class="active" </g:if> role="menuitem">
        <a href="${g.createLink(controller: 'model', action: 'create')}"><i class="icon icon-common icon-submit"></i> Submit</a>
    </li>
    <li <g:if test="${selectedSupportItems}"> class="active" </g:if> role="menuitem">
        <a><i class="icon icon-common icon-support"></i> <g:message code="jummp.support.biomodels.title"/></a>
        <ul class="menu">
            <li><a href="${g.createLink(controller: 'jummp', action: 'faq')}">FAQ</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'courses')}">Courses</a></li>
            <li><a href="//www.ebi.ac.uk/biomodels/tools/converters/" target="_blank">Online converters</a></li>
            <li><a href="//www.ebi.ac.uk/rdf/services/biomodels/sparql" target="_blank">SPARQL Endpoint</a></li>
            <li><a href="${createLink(controller: 'jummp', action: 'developerZone')}">Developer's Zone</a></li>
        </ul>
    </li>
    <li <g:if test="${selectedAboutusItems}"> class="active" </g:if> role="menuitem">
        <a><i class="icon icon-common icon-info"></i> <g:message code="jummp.aboutus.biomodels.title"/></a>
        <ul class="menu">
            <li><a href="${g.createLink(controller: 'jummp', action: 'termsOfUse')}">Terms of Use</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'howToCiteBioModelsDatabase')}">Citation</a></li>
            <li><a href="${grailsApplication.config.grails.serverURL}/content/news">News</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'acknowledgements')}">Acknowledgements</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'jobs')}">Jobs</a></li>
        </ul>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.contactus')?.length()}"> class="active" </g:if> role="menuitem">
        <a href="${g.createLink(controller: 'jummp', action: 'contactus')}">
            <i class="icon icon-common icon-contact"></i> <g:message code="jummp.contactus.biomodels.title"/>
        </a>
    </li>
    <li style="border-right: none" id="menuItemFeedback" data-open="rate_review_form" role="menuitem">
        <!-- rate_review_form is the identifier of the modal feedback form defined in the footer.
             This form is rendered using the feedback template of the web plugin -->
        <a><i class="icon icon-common icon-comment"></i> <g:message code="jummp.feedback.default.title"/></a>
    </li>
    <!-- If you need to include functional (as opposed to purely navigational) links in your local menu,
       add them here, and give them a class of "functional". Remember: you'll need a class of "last" for
       whichever one will show up last...
       For example: -->
    <sec:ifLoggedIn>
        <li class="functional first float-right" role="menuitem">
            <a>My Account</a>
            <ul class="menu">
                <li><a href="${grailsApplication.config.grails.serverURL}/user"><i class="icon icon-common icon-user-circle"></i>
                    ${sec.username()}'s Profile</a></li>
                <li class="divider"></li>
                <li><a href="${g.createLink(controller: 'search', action: 'list')}">
                    <img width="20" height="auto" title="Click here to see your models"
                         src="${grailsApplication.config.grails.serverURL}/images/biomodels/mymodels.png"/>&nbsp;My Models</a></li>
                <li><a href="${g.createLink(controller: 'team', action: 'index')}">
                    <img width="20" height="auto" title="Click here to see your teams"
                         src="${grailsApplication.config.grails.serverURL}/images/biomodels/team.png"/>
                    My Teams</a></li>
                <li class="functional float-right" role="menuitem" id="notificationCount">
                    <a title="View ${sec.username()}'s Notifications" href='<g:createLink controller="notification" action="list"/>'>
                        <img width="20" height="auto" title="Click here to see your notifications"
                             src="${grailsApplication.config.grails.serverURL}/images/email.png"/>
                        <span id="notificationLink">My Notifications</span>
                    </a>
                </li>
                <li class="divider"></li>
                <li class="functional last float-right" role="menuitem">
                    <a href="${grailsApplication.config.grails.serverURL}/logout" class="icon icon-functional" data-icon="l">
                        <g:message code="jummp.main.logout"/>
                    </a>
                </li>
            </ul>
        </li>
    </sec:ifLoggedIn>
    <sec:ifNotLoggedIn>
        <li class="functional first float-right" role="menuitem">
            <a href="${grailsApplication.config.grails.serverURL}/registration" class="icon icon-functional" data-icon="7">
                <g:message code="jummp.main.register"/>
            </a>
        </li>
        <li class="functional last float-right" role="menuitem">
            <a href="${grailsApplication.config.grails.serverURL}/login" class="icon icon-functional" data-icon="l">
                <g:message code="jummp.main.login"/>
            </a>
        </li>
    </sec:ifNotLoggedIn>
</ul>
</nav>
