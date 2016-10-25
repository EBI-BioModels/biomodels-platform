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
        def sidebarContent = g.pageProperty(name:'page.sidebar')
        if (sidebarContent) {
            sidebarContent = sidebarContent.trim()
        }
        def action = request.forwardURI
        action = action.substring(action.lastIndexOf("/") + 1)
    %>
    %{--<g:if test="${action == "models" || action == "search"}">
    <div id="content" role="main" class="row" style="max-width: inherit">
    </g:if>
    <g:else>
    <div id="content" role="main" class="row">
    </g:else>--}%
    <div id="content" role="main" class="row" style="max-width: inherit">
        <div data-sticky-container class="sticky-container">
            <section>
                <div id="main-content-area" class="row">
                <g:if test="${sidebarContent}">
                    <div class="medium-2 columns">
                        <h2>LeftSideBar</h2>
                        <p>Compared to cleaner areas, researchers found living in a polluted or noisy area increases a persons likelihood of developing high blood pressure by 22 per cent.
                        <br/>
                        It is believed the fine particles in the air could lead to harmful inflammation in blood vessels. </p>
                    </div>
                    <div class="medium-7 columns">
                        <g:render template="/templates/notification/showNotificationDiv"/>
                        <g:pageProperty name="page.main-content" />
                        <g:layoutBody/>
                    </div>
                    <div class="medium-3 columns sidebar sticky-container" data-sticky-container>
                        ${raw(sidebarContent)}
                    </div>
                </g:if>
                <g:else>
                    <div class="medium-12 columns" style="min-height: 600px" >
                        <g:render template="/templates/notification/showNotificationDiv"/>
                        <g:pageProperty name="page.main-content" />
                        <g:layoutBody/>
                    </div>
                </g:else>
                </div>
            </section>
        </div>
    </div>
