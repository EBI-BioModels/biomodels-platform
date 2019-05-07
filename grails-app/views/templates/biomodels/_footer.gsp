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











<footer id="local-footer" class="local-footer">
    <!-- Optional local footer (insert citation / project-specific copyright / etc here -->
    <div class="row">
        <div id="footer" class="float-left">
            Build: <g:render template="/templates/version"/>
        </div>
        <div class="clear"></div>
    </div>
     <div id="elixir-banner" data-color="grey" data-name="This service"
          data-description="BioModels is an ELIXIR Deposition Database"
          data-more-information-link="//www.elixir-europe.org/platforms/data/elixir-deposition-databases"
          data-use-basic-styles="true"></div>
     <script defer="defer"
             src="//ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.3/js/elixirBanner.js"></script>
     <style>
         .elixir-ribbon {
             padding: 1rem 0;
             /*background-color: rgb(79,138,156);*/
             background-color: rgb(0, 124, 130);
             /*background-color:#008080;*/
             vertical-align: middle;
         }

         .elixir-ribbon,
         .elixir-ribbon h5,
         .elixir-ribbon a,
         .elixir-ribbon a:active,
         .elixir-ribbon a:visited,
         .elixir-ribbon a:hover {
             color: #fff;
             text-decoration: none;
         }
         .elixir-ribbon a:hover {
             opacity: .8;
         }
         .elixir-ribbon .readmore {
             border-bottom: 1px dotted #fff;
         }
         .elixir-ribbon h5 {
             margin: 0;
         }
         .elixir-ribbon .elixir-logo-kite {
             background: 80% 58% url("https://ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.2/images/logos/assorted/elixir_kitemark-60px.png") no-repeat;
             position: relative;
             /*top: -5px;*/
             /*margin: 0 1rem -.5rem 0;*/
             margin-left: 1.25rem;
             height: 60px;
             width: 60px;
             display: inline-block;
             float: left;
             background-size: 60px;
             vertical-align: middle;
         }
         .elixir-ribbon .row {
             margin: 0 !important;
         }
     </style>
    <!-- End optional local footer -->

    <div id="global-footer" class="global-footer">
        <nav id="global-nav-expanded" class="row global-nav-expanded">
            <!-- Footer will be automatically inserted by footer.js -->
        </nav>
        <section id="ebi-footer-meta" class="row ebi-footer-meta">
            <!-- Footer meta will be automatically inserted by footer.js -->
        </section>
    </div>
     <g:render template="/templates/feedback" plugin="jummp-plugin-web-application"/>
     <g:render template="/templates/switchClassicBioModels" plugin="jummp-plugin-web-application"/>
     <g:render template="/templates/biomodels/searchTips" />
</footer>
</div> <!--! end of #mainframe -->

<!-- JavaScript at the bottom for fast page loading -->
<script src="https://dev.ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.3/js/script.js"></script>
<!-- Grab Google CDN's jQuery, with a protocol relative URL; fall back to local if offline -->

<!-- The Foundation theme JavaScript -->
<script src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.3/libraries/foundation-6/js/foundation.js"></script>
<script src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.3/js/foundationExtendEBI.js"></script>
<script type="text/JavaScript">$(document).foundation();</script>
<script type="text/JavaScript">$(document).foundationExtendEBI();</script>

<!-- customised scripts -->
<g:javascript src="common.js"></g:javascript>
<!-- end scripts-->

<!-- Google Analytics details... -->
<!-- Global site tag (gtag.js) - Google Analytics -->
<script async src="https://www.googletagmanager.com/gtag/js?id=UA-39747892-1"></script>
<script>
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments);}
    gtag('js', new Date());

    gtag('config', 'UA-39747892-1');
</script>

