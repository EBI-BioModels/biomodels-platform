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





<div id="skip-to">
    <a href="#content">Skip to main content</a>
</div>
<header id="masthead-black-bar" class="clearfix masthead-black-bar">
    <!-- EBML-EBI menu items will be rendered by the script in script.js -->
</header>

<div data-sticky-container class="sticky-container">
    <!-- Suggested layout containers -->
    <header id="masthead" class="masthead" data-sticky data-sticky-on="large" data-top-anchor="content:top"
            data-btm-anchor="content:bottom">

        <!-- local-title, local search -->
        <div class="masthead-inner row expanded">
            <div id="local-title"
                 class="hide-for-small-only medium-12 large-4 columns padding-top-none padding-bottom-none padding-left-none padding-right-none">
                <h1>
                    <a href="${createLink(uri: '/', absolute: true)}" title="Back to BioModels homepage"
                    style="text-decoration: none; border-bottom-style: none">
                        <img src="${grailsApplication.config.grails.serverURL}/images/biomodels/logo_small.png"
                             title="BioModels Homepage"/>
                        <span class="hide-for-small-only">BioModels</span></a>
                </h1>
            </div>
            <!-- local-search -->
            <div id="localsearch" class="small-12 medium-12 large-8 columns float-right">
                <g:render template="/templates/${grailsApplication.config.jummp.branding.style}/searchBox"/>
            </div>
            <!-- local navigation bar -->
            <g:render template="/templates/${grailsApplication.config.jummp.branding.style}/navbar"/>
        </div><!-- /local-title -->
    </header>
</div>
