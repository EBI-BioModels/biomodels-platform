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
    <ul>
    <li><a href="#content">Skip to main content</a></li>
    <li><a href="#local-nav">Skip to local navigation</a></li>
    <li><a href="#global-nav">Skip to EBI global navigation menu</a></li>
    <li><a href="#global-nav-expanded">Skip to expanded EBI global navigation menu (includes all sub-sections)</a></li>
    </ul>
</div>
<header id="masthead-black-bar" class="clearfix masthead-black-bar">
    <!-- EBML-EBI menu items will be rendered by the script in script.js -->
</header>

<div id="content" data-sticky-container="" lass="sticky-container">
    <!-- Suggested layout containers -->
    <header id="masthead" class="masthead sticky is-anchored is-at-top" data-sticky="42dzxn-sticky"
            data-sticky-on="large" data-top-anchor="content:top" data-btm-anchor="content:bottom"
            data-resize="masthead" data-mutate="masthead" data-events="mutate"
            style="max-width: 2545px; margin-top: 0px; bottom: auto; top: 0px; background-color: rgb(37, 65, 70);
                background-image: url('//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.3/images/backgrounds/embl-ebi-background.jpg');">
        <!-- local-title, local search -->
        <div id="local-title" class="row" style="padding-top: 10px">
            <div class="small-2 medium-7 large-8 columns" style="padding: 0">
                <h1>
                    <a href="${createLink(uri: '/', absolute: true)}" title="Back to BioModels homepage"
                    style="text-decoration: none; border-bottom-style: none">
                        <img src="${grailsApplication.config.grails.serverURL}/images/biomodels/logo_small.png"
                             title="BioModels Homepage"/>
                        <span class="hide-for-small-only">BioModels</span></a>
                </h1>
            </div>
            <!-- local-search -->
            <div id="localsearch" class="small-10 medium-5 large-4 columns float-right">
                <g:render template="/templates/${grailsApplication.config.jummp.branding.style}/searchBox"/>
            </div>
        </div>
        <!-- /local-title -->
        <div class="row">
            <!-- local navigation bar -->
            <g:render template="/templates/${grailsApplication.config.jummp.branding.style}/navbar"/>
        </div>
    </header>
</div>
