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
        def sidebarContent = g.pageProperty(name: 'page.sidebar')
        if (sidebarContent) {
            sidebarContent = sidebarContent.trim()
        }
        def facetSearchContent = g.pageProperty(name: 'page.facetsearch')
        if (facetSearchContent) {
            facetSearchContent = facetSearchContent.stripMargin()
        } else {
            facetSearchContent = ""
        }
    %>

    <div id="content" role="main" class="row">
        <div data-sticky-container class="sticky-container">
            <div>
                <g:if test="${facetSearchContent}">
                    <div class="medium-2 large-2 columns show-for-medium hide-for-small-only">
                        ${raw(facetSearchContent)}
                    </div>
                    <g:if test="${sidebarContent}">
                        <div class="small-12 medium-8 large-8 columns">
                            <g:render template="/templates/notification/showNotificationDiv"/>
                            <g:pageProperty name="page.main-content" />
                            <g:layoutBody/>
                        </div>
                        <div class="medium-2 large-2 columns sidebar sticky-container
                        show-for-medium hide-for-small-only" data-sticky-container>
                            ${raw(sidebarContent)}
                        </div>
                    </g:if>
                    <g:else>
                        <div class="small-12 medium-10 large-10 columns">
                            <g:render template="/templates/notification/showNotificationDiv"/>
                            <g:pageProperty name="page.main-content" />
                            <g:layoutBody/>
                        </div>
                    </g:else>
                </g:if>
                <g:else>
                    <!-- Facets search is unavailable -->
                    <g:if test="${sidebarContent}">
                        <div class="small-12 medium-8 large-9 columns">
                            <g:render template="/templates/notification/showNotificationDiv"/>
                            <g:pageProperty name="page.main-content" />
                            <g:layoutBody/>
                        </div>
                        <div class="small-12 medium-4 large-3 columns sticky-container" data-sticky-container>
                            ${raw(sidebarContent)}
                        </div>
                    </g:if>
                    <g:else>
                        <div id="main-content" class="small-12 medium-12 large-12 columns">
                            <g:render template="/templates/notification/showNotificationDiv"/>
                            <g:pageProperty name="page.main-content" />
                            <g:layoutBody/>
                        </div>
                    </g:else>
                </g:else>
            </div>
        </div>
    </div>
