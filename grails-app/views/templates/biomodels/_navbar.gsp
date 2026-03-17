<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils" %>
<%--
 Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

<div data-responsive-toggle="biomodels-menu" data-hide-for="medium">
    <button class="menu-icon" type="button" data-toggle="biomodels-menu"></button>
    <div class="title-bar-title">Menu</div>
</div>
<nav>
<div id="biomodels-menu">
<div class="top-bar-left">
<ul id="local-nav" class="main-menu dropdown menu"
    data-description="navigational" data-dropdown-menu role="menubar">
    <li <g:if test="${actionName == null}"> class="first active main-menu-item" </g:if>
        <g:else>class="main-menu-item"</g:else>
        role="menuitem">
        <a href="${createLink(uri: '/', absolute: true)}" title="Back to BioModels homepage">
            <i class="icon icon-generic" data-icon="H"></i> Home</a>
    </li>
    <%
        boolean selectedSupportItems = g.pageProperty(name:'page.faq')?.length() ||
            g.pageProperty(name:'page.courses')?.length() ||
            g.pageProperty(name:'page.curator-zone')?.length() ||
            g.pageProperty(name:'page.developer-zone')?.length()

        boolean selectedBrowseItems = g.pageProperty(name: 'page.search')?.length() ||
            g.pageProperty(name: 'page.bpsearch')?.length() ||
            g.pageProperty(name: 'page.covid19')?.length() ||
            g.pageProperty(name: 'page.goChart')?.length() ||
            g.pageProperty(name: 'page.agedbrain')?.length() ||
            g.pageProperty(name: 'page.path2models')?.length() ||
            g.pageProperty(name: 'page.pdgsmm')?.length() ||
            g.pageProperty(name: 'page.reproducibility')?.length()

        boolean selectedCurationItems = g.pageProperty(name: 'page.curationpage')?.length() ||
            g.pageProperty(name: 'page.fbcpage')?.length()

        boolean selectedAboutusItems = g.pageProperty(name:'page.termsOfUse')?.length() ||
            g.pageProperty(name:'page.citation')?.length() ||
            g.pageProperty(name:'page.news')?.length() ||
            g.pageProperty(name:'page.acknowledgements')?.length() ||
            g.pageProperty(name:'page.jobs')?.length()

        boolean loginSelected = g.pageProperty(name:'page.login')?.length()

        boolean registerSelected = g.pageProperty(name:'page.register')?.length()

        boolean selectedMyAccountItems = g.pageProperty(name:'page.myprofile')?.length() ||
            g.pageProperty(name:'page.mymodels')?.length() ||
            g.pageProperty(name:'page.myteams')?.length() ||
            g.pageProperty(name:'page.mynotifications')?.length() ||
            g.pageProperty(name:'page.adminboard')?.length() ||
            g.pageProperty(name:'page.curatorboard')?.length()
    %>
    <li <g:if test="${selectedBrowseItems}"> class="active main-menu-item" </g:if>
        <g:else>class="main-menu-item"</g:else>
        role="menuitem">
        <a><i class="icon icon-common" data-icon="b"></i> Browse</a>
        <ul class="menu">
            <li><a href="${g.createLink(controller: 'search', action: 'search', params: [query: '*:*'])}">All
            models</a></li>
            <li><a
                href="${g.createLink(controller: 'parameterSearch', action: 'index', plugin:
                    'jummp-plugin-biomodels-dom')}">BioModels Parameters Search</a>
            </li>
            <li><g:link mapping="covid19">COVID-19</g:link></li>
            %{--<li><a
                href="${g.createLink(controller: 'goChart', action: 'index', plugin: 'jummp-plugin-biomodels-dom')}">GO categories</a>
            </li>--}%
            <li><g:link mapping="agedbrain">Neurodegeneration models</g:link></li>
            <li><g:link mapping="path2models">Path2Models models</g:link></li>
            <li>
                <a href="${g.createLink(controller: 'pdgsmm', action: 'index')}">
                    PDGSM models
                </a>
            </li>
            <li><g:link mapping="reproducibility">Reproducibility</g:link></li>
        </ul>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.submit')?.length()}"> class="active main-menu-item" </g:if>
        <g:else>class="main-menu-item"</g:else>
        role="menuitem">
        <a href="${g.createLink(controller: 'model', action: 'submission-guidelines-and-agreement')}">
            <i class="icon icon-common icon-submit"></i>&nbsp;Submit</a>
    </li>
    <li <g:if test="${selectedCurationItems}"> class="active main-menu-item" </g:if>
        <g:else>class="main-menu-item"</g:else>role="menuitem">
        <a><i class="icon icon-common icon-cogs"></i>&nbsp;Curation</a>
        <ul class="menu">
            %{--<li><a href="${g.createLink(controller: 'curation', action: 'index')}">All curation pages</a></li>
            <li class="divider"></li>--}%
            <li><a href="${g.createLink(controller: 'curation', action: 'fbc')}">FBC</a></li>
        </ul>
    </li>
    <li <g:if test="${selectedSupportItems}"> class="active main-menu-item" </g:if>
        <g:else>class="main-menu-item"</g:else>role="menuitem">
        <a><i class="icon icon-common icon-support"></i> <g:message code="jummp.support.biomodels.title"/></a>
        <ul class="menu">
            <li><a href="${g.createLink(controller: 'jummp', action: 'faq')}">FAQs</a></li>
            <li class="divider"></li>
            <li><a href="${grailsApplication.config.jummp.context.help.root}/manual.html" target="_blank">User Manual</a></li>
            <li class="divider"></li>
            <li><a href="${createLink(controller: 'jummp', action: 'curatorZone')}">Curator's Zone</a></li>
            <li class="divider"></li>
            <li><a href="${createLink(controller: 'jummp', action: 'developerZone')}">Developer's Zone</a></li>
            <li class="divider"></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'courses')}">Courses</a></li>
            <li><a href="//www.ebi.ac.uk/biomodels/tools/converters/" target="_blank">Online converters</a></li>
        </ul>
    </li>
    <li <g:if test="${selectedAboutusItems}"> class="active main-menu-item" </g:if>
        <g:else>class="main-menu-item"</g:else>role="menuitem">
        <a><i class="icon icon-common icon-info"></i> <g:message code="jummp.aboutus.biomodels.title"/></a>
        <ul class="menu">
            <li><a href="${g.createLink(controller: 'jummp', action: 'privacyPolicy')}">Privacy Policy</a></li>
            %{-- <li><a href="${g.createLink(url: 'https://www.ebi.ac.uk/biomodels/static-assets/info/privacy-notice-nov-2022.pdf')}">Privacy Policy</a></li>--}%
            <li><a href="${g.createLink(controller: 'jummp', action: 'termsOfUse')}">Terms of Use</a></li>
            <li class="divider"></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'howToCiteBioModelsDatabase')}">Citation</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'fetchNews')}">News</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'acknowledgements')}">Acknowledgements</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'jobs')}">Jobs</a></li>
            <li><a href="${g.createLink(controller: 'jummp', action: 'curators')}">Curators</a></li>
            <li class="divider"></li>
            <li><a>Model of the Year</a>
                <ul class="menu">
                    <li><a href='<g:createLink uri="/competition/model-of-the-year-2025"/>'>MOY2025</a></li>
                    <li><a href='<g:createLink uri="/competition/model-of-the-year-2024"/>'>MOY2024</a></li>
                    <li><a href='<g:createLink uri="/competition/model-of-the-year-2023"/>'>MOY2023</a></li>
                </ul>
            </li>
        </ul>
    </li>
    <li <g:if test="${g.pageProperty(name:'page.contactus')?.length()}"> class="active main-menu-item" </g:if>
        <g:else>class="main-menu-item"</g:else>role="menuitem">
        <a href="${g.createLink(controller: 'jummp', action: 'contactus')}">
            <i class="icon icon-common icon-contact"></i> <g:message code="jummp.contactus.biomodels.title"/>
        </a>
    </li>
    <li style="border-right: none" id="menuItemFeedback" data-open="rate_review_form"
        role="menuitem" class="main-menu-item">
        <!-- rate_review_form is the identifier of the modal feedback form defined in the footer.
             This form is rendered using the feedback template of the web plugin -->
        <a><i class="icon icon-common icon-comment"></i> <g:message code="jummp.feedback.default.title"/></a>
    </li>
    <!-- If you need to include functional (as opposed to purely navigational) links in your local menu,
       add them here, and give them a class of "functional". Remember: you'll need a class of "last" for
       whichever one will show up last... For example: -->
</ul></div>
<div class="top-bar-right">
<ul class="main-menu dropdown menu"
        data-description="navigational" data-dropdown-menu role="menubar">
    <sec:ifLoggedIn>
        <li <g:if test="${selectedMyAccountItems}">
            class="active functional first float-right opens-left main-menu-item"</g:if>
            <g:else>
            class="functional first float-right opens-left main-menu-item"</g:else>
            role="menuitem" id="menu-item-myaccount">
            <a>My Account</a>
            <ul class="dropdown menu" data-dropdown-menu style="width: 235px; max-width: 265px">
                <li><a href='<g:createLink controller="usermanagement" action="show"/>'>
                    <span class="icon icon-common icon-user-circle">&nbsp;</span>${sec.username()}'s Profile</a></li>
                <li class="divider"></li>
                <li><a href="${g.createLink(controller: 'search', action: 'list')}">
                    <span class="icon icon-common icon-folder-open">&nbsp;</span>My Models</a></li>
                <li><a href="${g.createLink(controller: 'team', action: 'index')}">
                    <span class="icon icon-common icon-group">&nbsp;</span>
                    My Teams</a></li>
                <li class="functional float-right" role="menuitem" id="notificationCount">
                    <a title="View ${sec.username()}'s Notifications" href='<g:createLink controller="notification" action="list"/>'>
                        <span class="icon icon-common icon-envelope">&nbsp;</span>
                        <span id="notificationLink">My Notifications</span>
                    </a>
                </li>

                <g:if test="${SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')}">
                <li class="divider"></li>
                <li><a href="${g.createLink(uri: '/administration/dashboard')}"
                       title="Administration Dashboard"><span class="icon icon-common icon-user-md">&nbsp;</span>Admin
                    Dashboard</a></li>
                </g:if>

                <g:if test="${SpringSecurityUtils.ifAnyGranted('ROLE_CURATOR')}">
                <li class="divider"></li>
                <li><a href="${g.createLink(controller: 'curator', action: 'dashboard')}"
                       title="Curation Dashboard">
                    <span class="icon icon-common icon-reviewed-data hide-for-small-only">&nbsp;</span>
                    Curation Dashboard</a></li>
                </g:if>

                <li class="divider"></li>
                <li class="functional last float-right" role="menuitem">
                    <a href='<g:createLink controller="logout" action="index"/>'>
                        <span class="icon icon-common icon-sign-out-alt">&nbsp;</span><g:message
                            code="jummp.main.logout"/>
                    </a>
                </li>
            </ul>
        </li>
    </sec:ifLoggedIn>
    <sec:ifNotLoggedIn>
        <li <g:if test="${registerSelected}"> class="active main-menu-item functional first float-right " </g:if>
            <g:else>class="main-menu-item functional first float-right "</g:else>
            role="menuitem">
            <a href='<g:createLink uri="/registration" />' class="icon icon-functional"
               data-icon="7"><g:message code="jummp.main.register"/></a>
        </li>
        <li <g:if test="${loginSelected}"> class="active main-menu-item functional first float-right " </g:if>
            <g:else>class="main-menu-item functional first float-right "</g:else>
            role="menuitem">
            <a href='<g:createLink uri="/login" />'>
                <span class="icon icon-common icon-sign-in-alt">&nbsp;</span><g:message code="jummp.main.login"/>
            </a>
        </li>
    </sec:ifNotLoggedIn>
</ul>
</div></div>
</nav>
