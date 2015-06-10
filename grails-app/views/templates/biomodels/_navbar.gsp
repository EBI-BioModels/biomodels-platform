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
<ul class="grid_24 main-menu" id="local-nav">
    <li <g:if test="${g.pageProperty(name:'page.browse')?.length()}"> class="active" </g:if>>
        <a href="${g.createLink(controller: 'search', action: 'list')}">Browse</a>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.submit')?.length()}"> class="active" </g:if>>
        <a href="${g.createLink(controller: 'model', action: 'create')}">Submit</a>
    </li>
    <sec:ifLoggedIn>
        <li <g:if test="${g.pageProperty(name:'page.teams')?.length()}"> class="active" </g:if>>
            <a href="${g.createLink(controller: 'team', action: 'index')}">My Teams</a>
        </li>
    </sec:ifLoggedIn>
    <li <g:if test="${g.pageProperty(name:'page.help')?.length()}"> class="active" </g:if>>
        <a href="${g.createLink(controller: 'jummp', action: 'help')}">Help</a>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.feedback')?.length()}"> class="active" </g:if>>
        <a href="${g.createLink(controller: 'jummp', action: 'feedback')}">Feedback</a>
    </li>
    <!-- If you need to include functional (as opposed to purely navigational) links in your local menu,
       add them here, and give them a class of "functional". Remember: you'll need a class of "last" for
       whichever one will show up last...
       For example: -->
    <sec:ifLoggedIn>
        <li class="functional last">
            <a href="/jummp/logout" class="icon icon-functional" data-icon="l">
                <g:message code="jummp.main.logout"/>
            </a>
        </li>
        <li class="functional first">
            <a href="${grailsApplication.config.grails.serverURL}/user" class="icon icon-functional" data-icon="5">
                ${sec.username()}'s Profile
            </a>
        </li>
        <li class="functional" id="notificationCount">
      		<a title="View ${sec.username()}'s Notifications" href='<g:createLink controller="notification" action="list"/>'>
                <img width="20" height="auto" title="notifications" src="http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Generic/Generic%20icons/email.png"/>
      			<span id="notificationLink" style="display: none;"></span>
      		</a>
      	</li>
    </sec:ifLoggedIn>
    <sec:ifNotLoggedIn>
        <li class="functional first">
            <a href="${grailsApplication.config.grails.serverURL}/registration" class="icon icon-functional" data-icon="7">
                <g:message code="jummp.main.register"/>
            </a>
        </li>
        <li class="functional last">
            <a href="${grailsApplication.config.grails.serverURL}/login" class="icon icon-functional" data-icon="l">
                <g:message code="jummp.main.login"/>
            </a>
        </li>
    </sec:ifNotLoggedIn>
</ul>
</nav>
