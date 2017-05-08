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
<ul class="grid_24 main-menu dropdown menu float-left columns medium-12"
    id="local-nav" data-dropdown-menu role="menubar">
    <li <g:if test="${g.pageProperty(name:'page.search')?.length()}"> class="first active" </g:if> role="menuitem">
        <a href="${g.createLink(controller: 'search', action: 'search', params: [query: '*:*'])}">Browse</a>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.submit')?.length()}"> class="active" </g:if> role="menuitem">
        <a href="${g.createLink(controller: 'model', action: 'create')}">Submit</a>
    </li>
    <sec:ifLoggedIn>
        <li <g:if test="${g.pageProperty(name:'page.mymodels')?.length()}"> class="active" </g:if> role="menuitem">
            <a href="${g.createLink(controller: 'search', action: 'list')}">My Models</a>
        </li>
        <li <g:if test="${g.pageProperty(name:'page.teams')?.length()}"> class="active" </g:if> role="menuitem">
            <a href="${g.createLink(controller: 'team', action: 'index')}">My Teams</a>
        </li>
    </sec:ifLoggedIn>
    <li <g:if test="${g.pageProperty(name:'page.support')?.length()}"> class="active" </g:if> role="menuitem">
        <a href="#">
            <g:message code="jummp.support.biomodels.title"/>
        </a>
        <ul class="menu">
            <li><a href="http://www.ebi.ac.uk/biomodels-main/faq" target="_blank">FAQ</a></li>
            <li><a href="http://www.ebi.ac.uk/biomodels-main/courses">Courses</a></li>
            <li><a href="http://www.ebi.ac.uk/biomodels/tools/converters/" target="_blank">Online converters</a></li>
            <li><a href="https://www.ebi.ac.uk/rdf/services/biomodels/sparql" target="_blank">SPARQL Endpoint</a></li>
            <li><a href="https://bitbucket.org/jummp/jummp">Technical corner</a></li>
        </ul>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.aboutus')?.length()}"> class="active" </g:if> role="menuitem">
        <a href="#">
            <g:message code="jummp.aboutus.biomodels.title"/>
        </a>
        <ul class="menu">
            <li><a href="${g.createLink(controller: 'jummp', action: 'termsOfUse')}">Terms of Use</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'howToCiteBioModelsDatabase')}">Citation</a></li>
            <li><a href="${grailsApplication.config.grails.serverURL}/content/news">News</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'acknowledgements')}">Acknowledgements</a></li>
            <li><a href="https://bitbucket.org/jummp/jummp">Jobs</a></li>
            <li><a href="https://bitbucket.org/jummp/jummp">Curator Sign in</a></li>
        </ul>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.contactus')?.length()}"> class="active" </g:if> role="menuitem">
        <a href="${g.createLink(controller: 'jummp', action: 'contactus')}">
            <g:message code="jummp.contactus.biomodels.title"/>
        </a>
    </li>
    <!-- If you need to include functional (as opposed to purely navigational) links in your local menu,
       add them here, and give them a class of "functional". Remember: you'll need a class of "last" for
       whichever one will show up last...
       For example: -->
    <sec:ifLoggedIn>
        <li class="functional last float-right" role="menuitem">
            <a href="${grailsApplication.config.grails.serverURL}/logout" class="icon icon-functional" data-icon="l">
                <g:message code="jummp.main.logout"/>
            </a>
        </li>
        <li class="functional first float-right" role="menuitem">
            <a href="${grailsApplication.config.grails.serverURL}/user" class="icon icon-functional" data-icon="5">
                ${sec.username()}'s Profile
            </a>
        </li>
        <li class="functional float-right" role="menuitem" id="notificationCount">
      		<a title="View ${sec.username()}'s Notifications" href='<g:createLink controller="notification" action="list"/>'>
                <img width="20" height="auto" title="notifications"
                     src="${grailsApplication.config.grails.serverURL}/images/email.png"/>
      			<span id="notificationLink" style="display: none;"></span>
      		</a>
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
