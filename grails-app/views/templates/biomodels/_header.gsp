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

<div id="wrapper" class="container_24">
<header>
<div id="global-masthead" class="masthead grid_24">
<!--This has to be one line and no newline characters-->
<a href="//www.ebi.ac.uk/" title="Go to the EMBL-EBI homepage"><img src="//www.ebi.ac.uk/web_guidelines/images/logos/EMBL-EBI/EMBL_EBI_Logo_white.png" alt="EMBL European Bioinformatics Institute"></a>

<nav>
<ul id="global-nav">
  <!-- set active class as appropriate -->
  <li class="first active" id="services"><a href="//www.ebi.ac.uk/services">Services</a></li>
  <li id="research"><a href="//www.ebi.ac.uk/research">Research</a></li>
  <li id="training"><a href="//www.ebi.ac.uk/training">Training</a></li>
  <li id="industry"><a href="//www.ebi.ac.uk/industry">Industry</a></li>
  <li id="about" class="last"><a href="//www.ebi.ac.uk/about">About us</a></li>
</ul>
</nav>

</div>

<div id="local-masthead" class="masthead grid_24 nomenu">

<!-- local-title -->
<!-- NB: for additional title style patterns, see http://frontier.ebi.ac.uk/web/style/patterns -->
<div id="local-title" class="grid_12 alpha">
    <h1 style="margin: 5px 0;">
        <a href="${createLink(uri: '/', absolute: true)}" title="Back to BioModels homepage">
            <img src="${grailsApplication.config.grails.serverURL}/images/biomodels/logo_small.png"
                 height="100" width="100" style="padding-right: 12px;"
                 title="Biomodels Database Homepage"/>BioModels Database</a>
    </h1>
</div>
<!-- /local-title -->

<!-- local-search -->
<!-- NB: if you do not have a local-search, delete the following div, and drop the class="grid_12 alpha" class from local-title above -->

<div class="grid_12 omega">
        <form id="local-search" name="local-search" action="${createLink(controller: 'search', action: 'searchRedir')}" method="post">
                
          <fieldset>
          
          <div class="left">
            <label>
            <input type="text" name="search_block_form" id="local-searchbox">
            </label>
          </div>
          
          <div class="right">
            <input type="submit" name="submit" value="Search" class="submit">          
            <!-- If your search is more complex than just a keyword search, you can link to an Advanced Search,
                 with whatever features you want available 
            <span class="adv"><a href="../search" id="adv-search" title="Advanced">Advanced</a></span>-->
          </div>                  
          
          </fieldset>
          
        </form>
      </div>



<g:render template="/templates/${grailsApplication.config.jummp.branding.style}/navbar"/>
</div>
</header>
