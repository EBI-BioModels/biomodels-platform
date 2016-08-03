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










  <div id="skip-link">
    <a href="#main-menu" class="element-invisible element-focusable">Jump to Navigation</a>
  </div>

  <div id="header-full">
      <div id="header"><div class="section clearfix">

          <a href="${createLink(uri: '/', absolute: true)}" title="Home" rel="home" id="logo"><g:img dir="images/ddmore" file="logo.png" alt="DDMoRe Model Repository Logo"/></a>

      <div class="region region-header">
      	<div id="block-system-user-menu" class="block block-system user-menu block-menu first odd">      
      		<div class="content">
      			<ul class="menu">
      			<sec:ifLoggedIn>
      				<li class="first leaf">
      					<a title="View ${sec.username()}'s Profile" href="${grailsApplication.config.grails.serverURL}/user">
      						${sec.username()}'s Profile
      					</a>
      				</li>
      				<li class="leaf" id="notificationCount">
      					<a title="View ${sec.username()}'s Notifications" href='<g:createLink controller="notification" action="list"/>'>
                            <img width="20" height="auto" title="notifications" src="${grailsApplication.config.grails.serverURL}/images/email.png"/>
      						<span id="notificationLink" style="display: none;"></span>
      					</a>
      				</li>
      				<li class="last leaf">
      					<a title="Logout" href="${grailsApplication.config.grails.serverURL}/logout">
      						<g:message code="jummp.main.logout"/>
      					</a>
      				</li>
      			</sec:ifLoggedIn>
      			<sec:ifNotLoggedIn>
                    <g:if test="${grailsApplication.config.jummp.security.anonymousRegistration}">
      				<li class="first leaf">
      					<a href="${grailsApplication.config.grails.serverURL}/registration">
      						<g:message code="jummp.main.register"/>
      					</a>
      				</li>
                    <li class="last leaf">
                    </g:if>
                    <g:else>
                    <li class="first leaf">
                    </g:else>
      					<a href="${grailsApplication.config.grails.serverURL}/login">
      						<g:message code="jummp.main.login"/>
      					</a>
      				</li>
      			</sec:ifNotLoggedIn>
      			</ul>
      		</div><!-- /.block -->
      	</div>
      <div id="block-search-form" class="block block-search search even">
      	 <div class="content">
      	 	<g:form controller="search" action="searchRedir">
      	 	<%-- <form action="/" method="post" id="search-block-form" accept-charset="UTF-8"> --%><div><div class="container-inline">
      	 			<input title="Enter the terms you wish to search for." type="text" id="edit-search-block-form--2" name="search_block_form" value="" maxlength="256" class="form-text" />
      	 			<input type="submit" id="edit-submit" value="" class="form-submit" />
     		</div></div>
    	 	</g:form>
	
  </div>

</div>
      	 		
<div id="block-system-main-menu" class="block block-system main-menu block-menu last odd">

      
  <div class="content">
    <ul class="menu">
    	<li class="first expanded active-trail">
    		<a href="${g.createLink(controller: 'search', action: 'list')}" 
    		<g:if test="${g.pageProperty(name:'page.browse')?.length()}">
	    		class="active-trail active"
	    	</g:if>
    		title="browse models">Browse</a>
    	</li>
    	<li class="expanded">
    		<a href="${g.createLink(controller: 'model', action: 'create')}" 
    		<g:if test="${g.pageProperty(name:'page.submit')?.length()}">
	    		class="active-trail active"
	    	</g:if>
    		title="submit a model">Submit</a>
    	</li>
        <sec:ifLoggedIn>
            <li class="expanded">
                <a href="${g.createLink(controller: 'team', action: 'index')}"
                <g:if test="${g.pageProperty(name:'page.teams')?.length()}">
                    class="active-trail active"
                </g:if>
                title="view my teams">My Teams</a>
            </li>
        </sec:ifLoggedIn>
        <li class="expanded">
          <a href="${g.createLink(controller: 'jummp', action: 'feedback')}"
            <g:if test="${g.pageProperty(name:'page.feedback')?.length()}">
            class="active-trail active"
            </g:if>>
            <g:message code="jummp.feedback.ddmore.title"/></a>
        </li>
    </ul>
</div>
<p>${g.pageProperty(name:'page.selectedtab')}</p>
</div><!-- /.block -->
  </div><!-- /.region -->

  </div></div><!-- /.section, /#header -->

</div>

