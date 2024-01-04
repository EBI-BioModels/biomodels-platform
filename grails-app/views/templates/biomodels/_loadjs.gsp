<%@ page import="grails.util.Environment" %>
<!-- JavaScript at the bottom for fast page loading -->
<script src="https://ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.3/js/script.js"></script>
<!-- Grab Google CDN's jQuery, with a protocol relative URL; fall back to local if offline -->

<!-- The Foundation theme JavaScript -->
<script src="https://ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.3/libraries/foundation-6/js/foundation.js"></script>
<script src="https://ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.3/js/foundationExtendEBI.js"></script>
<script type="text/JavaScript">$(document).foundation();</script>
<script type="text/JavaScript">$(document).foundationExtendEBI();</script>

<!-- customised scripts -->
<g:javascript src="common.js"/>
<!-- end scripts-->

<g:if test="${!Environment.developmentMode}">
    <!-- Google Analytics details... -->
    <!-- Global site tag (gtag.js) - Google Analytics -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=UA-39747892-1"></script>
    <script>
        window.dataLayer = window.dataLayer || [];
        function gtag(){dataLayer.push(arguments);}
        gtag('js', new Date());

        gtag('config', 'UA-39747892-1');
    </script>
</g:if>
