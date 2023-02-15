<%--
Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
Deutsches Krebsforschungszentrum (DKFZ)

This file is part of Jummp.

Jummp is free software; you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free
Software Foundation; either version 3 of the License, or (at your option) any
later version.

Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

You should have received a copy of the GNU Affero General Public License along
with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>

<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 12/12/2018
  Time: 17:18
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>Mechanistic models describing neurodegenerative disease processes | BioModels</title>
    <style type="text/css">
        div.model-map {
            display: flex;
            align-items: center;
            justify-content: center;
            max-width: 100%;
            overflow: auto;
        }
        div.model-map p {
            margin: 0
        }
        #svg4949 {
            margin: 0 auto;
            display: block;
        }
    </style>
</head>

<body>
<cache:block  ttl="${60*60*24*7}">
    <h2>Mechanistic models describing neurodegenerative disease processes</h2>
<p style="padding: 8px 8px; background: #FCEFA1 none repeat scroll 0% 0%; margin-bottom: 2em; width: 100%;">
    This page is best viewed using Mozilla Firefox, Google Chrome or Safari. Please see
    <a target="_blank" href="http://caniuse.com/#feat=svg-html5" title="browser support page -- opens in a new tab">this page</a>
    for additional information.
</p>

<p style="margin-bottom: 1em;">
    <span style="text-decoration: underline;">
        Model space in neurodegeneration - model landscape map.
    </span>

    This cellular and molecular process diagram of neurodegeneration was created by
    integrating the mechanisms described in 89 neurodegenerative disease models published
    in the literature. It is an abstract representation of the processes involved,
    i.e. only important mechanisms of each process are shown for better visualisation.
    Boundaries of subcellular organelles are represented as solid lines, cell boundaries
    as thick solid lines and the blood-brain barrier as thick dotted lines. The 15
    biological processes associated with ND that the models describe are in capital
    bold red font. The models falling under each of these processes are displayed as
    numbers and hyperlinked to the original publication or if the models exist in
    BioModels it is linked to model page in BioModels. Models involving more than one
    process are over-lined. The density of distribution of models in each process is
    illustrated as colour gradient (gradient definition is provided in the figure).
</p>
<div class="model-map">
    ${svgAgedBrain}
</div>
<div>
    <p style=" margin-top: 3em; margin-bottom: 1em">
        A review of <a href="//onlinelibrary.wiley.com/doi/10.1002/psp4.12155/full" title="Access this publication in a new window" target="_blank">mechanistic models on neurodegenerative disease processes</a>
        was published in CPT:PSP.</p>
    <p style="padding-left: 1em;">
        <a href="//www.agedbrainsysbio.eu/" title="AgedBrainSYSBIO">
            <img style="height: 50px" src="${grailsApplication.config.grails.serverURL}/images/biomodels/AgedBrainSYSBIO_logo.png" alt="AgedBrainSYSBIO logo" title="AgedBrainSYSBIO"></a>
        <a href="//cordis.europa.eu/projects/rcn/105858_en.html" title="View additional information about the AgedBrain project">
            <img style="height: 50px" src="${grailsApplication.config.grails.serverURL}/images/biomodels/european_flag_blueyellow_standard.jpg" alt="EU logo" title="European Union (EU)"></a>
        AgedBrainSYSBIO has received funding from the European Union's Seventh
    Framework Programme for research, technological development and demonstration under grant agreement
    No 305299
    </p>
</div>
</cache:block>
</body>
</html>
<content tag="agedbrain">
    selected
</content>
