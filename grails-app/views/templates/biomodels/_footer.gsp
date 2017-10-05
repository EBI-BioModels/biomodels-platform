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











 <footer>
    <!-- Optional local footer (insert citation / project-specific copyright / etc here -->
    <div id="local-footer">
        <div class="row">
            <span><wcm:render path="footer"/></span>
        </div>
    </div>
    <!-- End optional local footer -->

    <div id="global-footer">
        <nav id="global-nav-expanded" class="row">
            <!-- Footer will be automatically inserted by footer.js -->
        </nav>
        <section id="ebi-footer-meta" class="row">
            <!-- Footer meta will be automatically inserted by footer.js -->
        </section>
    </div>
     <g:render template="/templates/feedback" plugin="jummp-plugin-web-application"/>
 </footer>
</div> <!--! end of #wrapper -->

  <!-- JavaScript at the bottom for fast page loading -->
  <!-- Grab Google CDN's jQuery, with a protocol relative URL; fall back to local if offline -->

  <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/js/cookiebanner.js"></script>
  <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/js/foot.js"></script>
  <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/js/script.js"></script>

  <!-- The Foundation theme JavaScript -->
  <script src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/libraries/foundation-6/js/foundation.js"></script>
  <script src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/js/foundationExtendEBI.js"></script>
  <script type="text/JavaScript">$(document).foundation();</script>
  <script type="text/JavaScript">$(document).foundationExtendEBI();</script>

  <!-- customised scripts -->
  <g:javascript src="common.js"></g:javascript>
  <!-- end scripts-->

  <!-- Google Analytics details... -->
  <script>
    window._gaq = [['_setAccount','UA-106769759-1'],['_trackPageview'],['_trackPageLoadTime']];
    Modernizr.load({
      load: ('https:' == location.protocol ? '//ssl' : '//www') + '.google-analytics.com/ga.js'
    });
  </script>

  <!-- Prompt IE 6 users to install Chrome Frame. Remove this if you want to support IE 6.
       chromium.org/developers/how-tos/chrome-frame-getting-started -->
  <!--[if lt IE 7 ]>
  <!--<script src="//ajax.googleapis.com/ajax/libs/chrome-frame/1.0.3/CFInstall.min.js"></script>-->
  <!--<script>window.attachEvent('onload',function(){CFInstall.check({mode:'overlay'})})</script>-->
