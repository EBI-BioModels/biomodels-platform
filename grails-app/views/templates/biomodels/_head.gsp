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






<meta charset="utf-8">
<!-- Use the .htaccess and remove these lines to avoid edge case issues.
   More info: h5bp.com/b/378 -->
<!-- Not yet implemented -->
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
<!-- Describe what this page is about -->
<meta name="description" content="BioModels is a repository of freely-available mathematical models of biological and biomedical systems. It hosts a vast selection of physiologically and pharmaceutically
relevant mechanistic models in standard formats."/>
<meta name="keywords" content="BioModels Database, biomodels, systems biology, bioinformatics, computational
    modelling, systems modelling, model repository, model database, SBML, PharmML, COMBINE Archive, OMEX,
    public domain">
<meta name="author" content="BioModels"/>
<meta name="google" content="notranslate" />
<meta name="ebi:masthead-color" content="#254146">
<meta name="ebi:masthead-image"
      content="//ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.3/images/backgrounds/embl-ebi-background.jpg"/>

<!-- Mobile viewport optimized: j.mp/bplateviewport -->
<meta name="viewport" content="width=device-width,initial-scale=1">

<!-- Place favicon.ico and apple-touch-icon.png in the root directory: mathiasbynens.be/notes/touch-icons -->
<%
    bmUrlPrefix = "${grailsApplication.config.grails.serverURL}/images/biomodels"
%>
<link rel="apple-touch-icon" sizes="57x57" href="${bmUrlPrefix}/apple-icon-57x57.png">
<link rel="apple-touch-icon" sizes="60x60" href="${bmUrlPrefix}/apple-icon-60x60.png">
<link rel="apple-touch-icon" sizes="72x72" href="${bmUrlPrefix}/apple-icon-72x72.png">
<link rel="apple-touch-icon" sizes="76x76" href="${bmUrlPrefix}/apple-icon-76x76.png">
<link rel="apple-touch-icon" sizes="114x114" href="${bmUrlPrefix}/apple-icon-114x114.png">
<link rel="apple-touch-icon" sizes="120x120" href="${bmUrlPrefix}/apple-icon-120x120.png">
<link rel="apple-touch-icon" sizes="144x144" href="${bmUrlPrefix}/apple-icon-144x144.png">
<link rel="apple-touch-icon" sizes="152x152" href="${bmUrlPrefix}/apple-icon-152x152.png">
<link rel="apple-touch-icon" sizes="180x180" href="${bmUrlPrefix}/apple-icon-180x180.png">
<link rel="icon" type="image/png" sizes="192x192"  href="${bmUrlPrefix}/android-icon-192x192.png">
<link rel="icon" type="image/png" sizes="32x32" href="${bmUrlPrefix}/favicon-32x32.png">
<link rel="icon" type="image/png" sizes="96x96" href="${bmUrlPrefix}/favicon-96x96.png">
<link rel="icon" type="image/png" sizes="16x16" href="${bmUrlPrefix}/favicon-16x16.png">
<link rel="manifest" href="${bmUrlPrefix}/manifest.json">
<meta name="msapplication-TileColor" content="#ffffff">
<meta name="msapplication-TileImage" content="${bmUrlPrefix}/ms-icon-144x144.png">
<meta name="theme-color" content="#ffffff">

<!-- If you link to any other sites frequently, consider optimising performance with a DNS prefetch -->
<link rel="dns-prefetch" href="//embl.de" />
<!-- CSS: implied media=all -->
<!-- CSS concatenated and minified via ant build script -->
<link rel="stylesheet" type="text/css"
      href="//ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.3/css/ebi-global.css"
      media="none" onload="if(media!=='all') media='all'">
<link rel="stylesheet" type="text/css"
      href="//ebi.emblstatic.net/web_guidelines/EBI-Icon-fonts/v1.3/fonts.css"
      media="none" onload="if(media!=='all') media='all'">
<link rel="stylesheet" type="text/css"
      href="//ebi.emblstatic.net/web_guidelines/EBI-Framework/v1.3/css/theme-embl-petrol.css"
      media="none" onload="if(media!=='all') media='all'">

<!-- you can replace this with [projectname]-colours.css. See http://frontier.ebi.ac.uk/web/style/colour
for details of how to do this -->
<style type="text/css">
    /* You have the option of setting a maximum width for your page, and making sure everything is centered */
    body {
        margin: 2px 5px auto;
    }
    li.divider {
        border-top: 1px solid #999;
    }

    #local-masthead nav ul.menu li:hover {
        background: rgb(0,124,130);
    }
</style>
<link rel="stylesheet" type="text/css" href="<g:resource dir="css" file="biomodels/biomodels.css"/>">
<link rel="stylesheet" type="text/css" href="<g:resource dir="css" file="common.css"/>">
<!-- end CSS-->

<title><g:layoutTitle default="BioModels"/></title>
