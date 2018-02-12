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
<div data-sticky-container class="sticky-container" >
    <div id="local-masthead" data-sticky data-sticky-on="large" data-top-anchor="165.75" data-btm-anchor="300000"
         class="sticky meta-background-color meta-background-image" data-resize="local-masthead" data-events="resize">
        <header>
        <div id="global-masthead" class="clearfix">
	      <!--This has to be one line and no newline characters-->
	      <a href="//www.ebi.ac.uk/" title="Go to the EMBL-EBI homepage"><span class="ebi-logo"></span></a>
            <nav>
                <div class="row">
                    <ul id="global-nav" class="menu">
                        <!-- set active class as appropriate -->
                        <li id="home-mobile" class=""><a href="//www.ebi.ac.uk"></a></li>
                        <li id="home" class="active"><a href="//www.ebi.ac.uk"><i class="icon icon-generic"
                                                                                  data-icon="H"></i> EMBL-EBI</a></li>
                        <li id="services"><a href="//www.ebi.ac.uk/services"><i class="icon icon-generic"
                                                                                data-icon="("></i> Services</a></li>
                        <li id="research"><a href="//www.ebi.ac.uk/research"><i class="icon icon-generic"
                                                                                data-icon=")"></i> Research</a></li>
                        <li id="training"><a href="//www.ebi.ac.uk/training"><i class="icon icon-generic"
                                                                                data-icon="t"></i> Training</a></li>
                        <li id="about"><a href="//www.ebi.ac.uk/about"><i class="icon icon-generic"
                                                                          data-icon="i"></i> About us</a></li>
                        <li id="search">
                            <a href="#" data-toggle="search-global-dropdown" aria-controls="search-global-dropdown"
                               data-is-focus="false" data-yeti-box="search-global-dropdown" aria-haspopup="true"
                               aria-expanded="false"><i class="icon icon-functional" data-icon="1"></i> <span
                                class="show-for-small-only">Search</span></a>

                            <div id="search-global-dropdown" class="dropdown-pane" data-dropdown="zq3ej2-dropdown"
                                 data-options="closeOnClick:true;" aria-hidden="true"
                                 data-yeti-box="search-global-dropdown" data-resize="search-global-dropdown"
                                 aria-labelledby="ehvmx4-dd-anchor">
                                <form id="global-search" name="global-search" action="/ebisearch/search"
                                      method="GET">
                                    <fieldset>
                                        <div class="input-group">
                                            <input type="text" name="query" id="global-searchbox"
                                                   class="input-group-field" placeholder="Search all of EMBL-EBI">

                                            <div class="input-group-button">
                                                <input type="submit" name="submit" value="Search" class="button">
                                                <input type="hidden" name="db" value="allebi" checked="checked">
                                                <input type="hidden" name="requestFrom" value="global-masthead"
                                                       checked="checked">
                                            </div>
                                        </div>
                                    </fieldset>
                                </form>
                            </div>
                        </li>
                        <li class="float-right show-for-medium embl-selector">
                            <button class="button" type="button" data-toggle="embl-dropdown"
                                    aria-controls="embl-dropdown" data-is-focus="false" data-yeti-box="embl-dropdown"
                                    aria-haspopup="true" aria-expanded="false">Hinxton</button>
                            <!-- The dropdown menu will be programmatically added by script.js -->

                        </li>
                    </ul>
                </div>
            </nav>
        </div>
        <div class="masthead row">
            <!-- local-title -->
            <div id="local-title" class="columns medium-12">
                <div class="row" style="margin-top: -25px">
                    <div class="column small-2 medium-7 large-8">
                        <h1>
                            <a href="${createLink(uri: '/', absolute: true)}" title="Back to BioModels homepage"
                            style="text-decoration: none; border-bottom-style: none">
                                <img src="${grailsApplication.config.grails.serverURL}/images/biomodels/logo_small.png"
                                     title="BioModels Homepage"/>
                                <span class="hide-for-small-only">BioModels</span>
                                <sup><span class="icon icon-generic" data-icon=">"></span></sup></a>
                        </h1>
                    </div>
                    <!-- local-search -->
                    <div id="localsearch" class="column small-10 medium-5 large-4 float-right">
                        <g:render template="/templates/${grailsApplication.config.jummp.branding.style}/searchBox"/>
                    </div>
                </div>
            </div>
            <!-- /local-title -->
            <!-- local navigation bar -->
            <g:render template="/templates/${grailsApplication.config.jummp.branding.style}/navbar"/>
        </div>
    </header>
    </div>
</div>
